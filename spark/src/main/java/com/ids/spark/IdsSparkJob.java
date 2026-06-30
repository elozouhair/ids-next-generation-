package com.ids.spark;

import com.ids.spark.ml.Predictor;
import com.ids.spark.stream.KafkaStreamConsumer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.spark.sql.*;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import java.util.Iterator;

import static org.apache.spark.sql.functions.col;

public class IdsSparkJob {

    private static com.ids.spark.geo.GeoIPResolver geoIP;

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();
        geoIP = new com.ids.spark.geo.GeoIPResolver(config);

        SparkSession spark = SparkSession.builder()
                .appName(config.getString("spark.app-name"))
                .master(config.getString("spark.master"))
                .config("spark.sql.shuffle.partitions", "2")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.streaming.stopGracefullyOnShutdown", "true")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        System.out.println("=== Spark session started ===");

        Dataset<Row> rawStream = KafkaStreamConsumer.consume(spark, config);

        Dataset<Row> parsedStream = KafkaStreamConsumer.parseJsonStream(rawStream);

        System.out.println("=== PARSED STREAM SCHEMA ===");
        parsedStream.printSchema();

        Dataset<Row> featureDf = com.ids.spark.feature.FeatureEngineer.extractFeatures(parsedStream);

        String modelPath = config.getString("spark.model-path");
        System.out.println("Attempting ML prediction (falls back to rules if no model)...");
        Dataset<Row> predictions = Predictor.predictBatch(featureDf, modelPath);

        List<String> rawFeatureCols = new java.util.ArrayList<>();
        rawFeatureCols.addAll(config.getStringList("ml.feature-columns"));
        rawFeatureCols.add("true_label");
        Column[] rawCols = rawFeatureCols.stream()
                .map(functions::col)
                .toArray(Column[]::new);

        Dataset<Row> enrichedForDb = predictions
                .withColumn("timestamp", functions.current_timestamp())
                .select(
                        functions.col("timestamp"),
                        functions.col("prediction_label").as("attack_type"),
                        functions.col("src_ip"),
                        functions.col("dst_ip"),
                        functions.col("src_port"),
                        functions.col("dst_port"),
                        functions.col("protocol"),
                        functions.when(functions.col("prediction").equalTo(1.0),
                                functions.lit("high")).otherwise(functions.lit("low")).as("severity"),
                        functions.col("prediction"),
                        functions.col("true_label"),
                        functions.to_json(functions.struct(rawCols)).as("raw_features")
                );

        System.out.println("Starting single streaming query...");

        StreamingQuery query = enrichedForDb
                .writeStream()
                .outputMode(OutputMode.Append())
                .foreachBatch((Dataset<Row> batch, Long batchId) -> {
                    batch.cache();
                    long count = batch.count();
                    System.out.println("=== Batch " + batchId + ": " + count + " rows ===");
                    if (count > 0) {
                        long t0 = System.currentTimeMillis();

                        Dataset<Row> metrics = batch.agg(
                                functions.sum(functions.when(
                                        functions.col("prediction").equalTo(1.0)
                                                .and(functions.col("true_label").equalTo(1.0)), 1L).otherwise(0L)).as("true_positives"),
                                functions.sum(functions.when(
                                        functions.col("prediction").equalTo(0.0)
                                                .and(functions.col("true_label").equalTo(0.0)), 1L).otherwise(0L)).as("true_negatives"),
                                functions.sum(functions.when(
                                        functions.col("prediction").equalTo(1.0)
                                                .and(functions.col("true_label").equalTo(0.0)), 1L).otherwise(0L)).as("false_positives"),
                                functions.sum(functions.when(
                                        functions.col("prediction").equalTo(0.0)
                                                .and(functions.col("true_label").equalTo(1.0)), 1L).otherwise(0L)).as("false_negatives")
                        ).select(
                                functions.lit(batchId).as("batch_id"),
                                functions.lit(count).as("total_samples"),
                                functions.col("true_positives"),
                                functions.col("true_negatives"),
                                functions.col("false_positives"),
                                functions.col("false_negatives"),
                                functions.col("true_positives").plus(functions.col("true_negatives"))
                                        .divide(functions.lit(count)).as("accuracy"),
                                functions.when(functions.col("true_positives").plus(functions.col("false_positives")).gt(0),
                                        functions.col("true_positives").divide(
                                                functions.col("true_positives").plus(functions.col("false_positives")))
                                ).otherwise(0.0).as("precision"),
                                functions.when(functions.col("true_positives").plus(functions.col("false_negatives")).gt(0),
                                        functions.col("true_positives").divide(
                                                functions.col("true_positives").plus(functions.col("false_negatives")))
                                ).otherwise(0.0).as("recall"),
                                functions.lit(0.0).as("f1_score"),
                                functions.current_timestamp().as("timestamp")
                        ).withColumn("f1_score",
                                functions.when(functions.col("precision").plus(functions.col("recall")).gt(0),
                                        functions.lit(2).multiply(functions.col("precision")).multiply(functions.col("recall"))
                                                .divide(functions.col("precision").plus(functions.col("recall")))
                                ).otherwise(0.0));
                        writeToPostgres(metrics, config, "model_metrics", 1);

                        writeToPostgres(batch, config, "alerts", 2);
                        System.out.println("=== Batch " + batchId + " alerts write: " + (System.currentTimeMillis()-t0) + "ms ===");

                        t0 = System.currentTimeMillis();
                        Dataset<Row> trafficStats = batch
                                .groupBy()
                                .agg(
                                        functions.count(functions.lit(1)).as("total_packets"),
                                        functions.sum(functions.when(
                                                functions.col("prediction").equalTo(0.0), 1).otherwise(0)).as("normal_count"),
                                        functions.sum(functions.when(
                                                functions.col("prediction").equalTo(1.0), 1).otherwise(0)).as("attack_count")
                                )
                                .withColumn("normal_percentage",
                                        functions.col("normal_count").divide(functions.col("total_packets")).multiply(100))
                                .withColumn("attack_percentage",
                                        functions.col("attack_count").divide(functions.col("total_packets")).multiply(100))
                                .withColumn("timestamp", functions.current_timestamp());

                        writeToPostgres(trafficStats, config, "traffic_stats", 1);
                        System.out.println("=== Batch " + batchId + " traffic_stats write: " + (System.currentTimeMillis()-t0) + "ms ===");

                        t0 = System.currentTimeMillis();
                        Dataset<Row> summary = batch
                                .groupBy("attack_type")
                                .agg(
                                        functions.count(functions.lit(1)).as("count"),
                                        functions.max("timestamp").as("last_seen"),
                                        functions.avg("prediction").as("avg_confidence")
                                );
                        summary.write()
                                .mode(SaveMode.Overwrite)
                                .format("jdbc")
                                .option("url", config.getString("postgres.url"))
                                .option("user", config.getString("postgres.user"))
                                .option("password", config.getString("postgres.password"))
                                .option("driver", config.getString("postgres.driver"))
                                .option("dbtable", "attack_summary")
                                .option("batchsize", "500")
                                .option("isolationLevel", "NONE")
                                .option("numPartitions", "1")
                                .option("truncate", "true")
                                .save();
                        System.out.println("=== Batch " + batchId + " attack_summary write: " + (System.currentTimeMillis()-t0) + "ms ===");

                        t0 = System.currentTimeMillis();
                        org.apache.spark.sql.types.StructType geoSchema = org.apache.spark.sql.types.StructType.fromDDL(
                                "src_ip STRING, attack_type STRING, severity STRING, " +
                                "latitude DOUBLE, longitude DOUBLE, timestamp TIMESTAMP"
                        );
                        final Config geoConfig = config;
                        Dataset<Row> geoEnriched = batch
                                .filter(functions.col("src_ip").isNotNull())
                                .mapPartitions((Iterator<Row> rows) -> {
                                    com.ids.spark.geo.GeoIPResolver localGeo = new com.ids.spark.geo.GeoIPResolver(geoConfig);
                                    java.util.List<Row> out = new java.util.ArrayList<>();
                                    while (rows.hasNext()) {
                                        Row row = rows.next();
                                        String srcIp = row.getAs("src_ip");
                                        if (srcIp == null || srcIp.isEmpty()) srcIp = "0.0.0.0";
                                        double[] coords = localGeo.resolve(srcIp);
                                        java.sql.Timestamp ts = row.getAs("timestamp");
                                        if (ts == null) ts = new java.sql.Timestamp(System.currentTimeMillis());
                                        String atkType = row.getAs("attack_type") != null ? row.<String>getAs("attack_type") : "Unknown";
                                        String sev = row.getAs("severity") != null ? row.<String>getAs("severity") : "medium";
                                        out.add(org.apache.spark.sql.RowFactory.create(srcIp, atkType, sev, coords[0], coords[1], ts));
                                    }
                                    return out.iterator();
                                }, org.apache.spark.sql.Encoders.row(geoSchema))
                                .cache();
                        long geoCount = geoEnriched.count();
                        if (geoCount > 0) {
                            writeToPostgres(geoEnriched, config, "attacks_geo", 2);
                        }
                        geoEnriched.unpersist();
                        System.out.println("=== Batch " + batchId + " attacks_geo: " + geoCount + " locations written in " + (System.currentTimeMillis()-t0) + "ms ===");

                        t0 = System.currentTimeMillis();
                        org.apache.spark.sql.types.StructType featureSchema = new org.apache.spark.sql.types.StructType();
                        for (String fc : config.getStringList("ml.feature-columns")) {
                            featureSchema = featureSchema.add(fc, org.apache.spark.sql.types.DataTypes.DoubleType, true);
                        }
                        if (batchId % 5 == 0) {
                            Dataset<Row> trainingBatch = batch
                                    .select(functions.from_json(functions.col("raw_features"), featureSchema).as("features"), functions.col("true_label"))
                                    .select("features.*", "true_label")
                                    .filter(functions.col("true_label").isNotNull());
                            long trainingCount = trainingBatch.count();
                            if (trainingCount > 0) {
                                trainingBatch.write().mode(SaveMode.Append).parquet(config.getString("spark.training-data-path"));
                            }
                            System.out.println("=== Batch " + batchId + " training data: " + trainingCount + " rows written in " + (System.currentTimeMillis()-t0) + "ms ===");
                        } else {
                            System.out.println("=== Batch " + batchId + " training data: skipped (every 5) ===");
                        }

                        System.out.println("=== Batch " + batchId + " COMPLETE ===");
                    }
                    batch.unpersist();
                })
                .option("checkpointLocation", config.getString("spark.checkpoint-dir") + "/single")
                .trigger(Trigger.ProcessingTime(config.getString("spark.batch-duration-seconds") + " seconds"))
                .start();

        System.out.println("Streaming query started. Waiting for termination...");

        spark.streams().awaitAnyTermination();
    }

    private static void writeToPostgres(Dataset<Row> df, Config config, String table, int numPartitions) {
        df.write()
                .mode(SaveMode.Append)
                .format("jdbc")
                .option("url", config.getString("postgres.url"))
                .option("user", config.getString("postgres.user"))
                .option("password", config.getString("postgres.password"))
                .option("driver", config.getString("postgres.driver"))
                .option("dbtable", table)
                .option("batchsize", "500")
                .option("isolationLevel", "NONE")
                .option("numPartitions", String.valueOf(numPartitions))
                .save();
    }
}
