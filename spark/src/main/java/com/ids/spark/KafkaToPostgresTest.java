package com.ids.spark;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;

public class KafkaToPostgresTest {
    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();

        SparkSession spark = SparkSession.builder()
                .appName("KafkaToPostgresTest")
                .master(config.getString("spark.master"))
                .config("spark.sql.shuffle.partitions", "4")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> rawStream = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", config.getString("kafka.bootstrap-servers"))
                .option("subscribe", config.getString("kafka.topic"))
                .option("startingOffsets", "earliest")
                .option("failOnDataLoss", false)
                .load()
                .select(functions.col("value").cast("string").alias("raw_value"));

        Dataset<Row> parsed = rawStream.select(
                functions.col("raw_value"),
                functions.current_timestamp().alias("timestamp")
        );

        StreamingQuery query = parsed
                .writeStream()
                .outputMode(OutputMode.Append())
                .foreachBatch((Dataset<Row> batch, Long batchId) -> {
                    if (!batch.isEmpty()) {
                        System.out.println("Batch " + batchId + " has " + batch.count() + " rows, writing to postgres");
                        batch.write()
                                .mode(SaveMode.Append)
                                .format("jdbc")
                                .option("url", config.getString("postgres.url"))
                                .option("user", config.getString("postgres.user"))
                                .option("password", config.getString("postgres.password"))
                                .option("driver", config.getString("postgres.driver"))
                                .option("dbtable", "rawd")
                                .save();
                        System.out.println("Batch " + batchId + " write complete");
                    } else {
                        System.out.println("Batch " + batchId + " is empty, skipping");
                    }
                })
                .option("checkpointLocation", config.getString("spark.checkpoint-dir") + "/rawtest")
                .trigger(Trigger.ProcessingTime("10 seconds"))
                .start();

        spark.streams().awaitAnyTermination();
    }
}
