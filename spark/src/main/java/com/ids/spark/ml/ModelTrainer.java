package com.ids.spark.ml;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.classification.RandomForestClassificationModel;
import org.apache.spark.ml.classification.RandomForestClassifier;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Arrays;

public class ModelTrainer {

    private static final Logger log = LoggerFactory.getLogger(ModelTrainer.class);
    private static final Config config = ConfigFactory.load();

    private static Dataset<Row> normalizeColumnNames(Dataset<Row> df) {
        for (String col : df.columns()) {
            String normalized = col.trim()
                    .replace("\r", "")
                    .replace("\n", "")
                    .toLowerCase()
                    .replaceAll("[^a-z0-9_]", "_")
                    .replaceAll("_+", "_")
                    .replaceAll("^_|_$", "");
            if (!col.equals(normalized)) {
                df = df.withColumnRenamed(col, normalized);
            }
        }
        // Map normalized CSV names to standard names used by Kafka stream
        String[][] renames = {
            {"flow_bytes_s", "flow_bytes_per_sec"},
            {"flow_packets_s", "flow_packets_per_sec"},
            {"average_packet_size", "avg_packet_size"}
        };
        for (String[] r : renames) {
            if (java.util.Arrays.asList(df.columns()).contains(r[0])) {
                df = df.withColumnRenamed(r[0], r[1]);
            }
        }
        return df;
    }

    public static void train(SparkSession spark, Dataset<Row> trainingData, String modelPath) throws Exception {
        List<String> featureColumns = config.getStringList("ml.feature-columns");
        String labelColumn = config.getString("ml.label-column");

        String[] requiredFeatures = featureColumns.toArray(new String[0]);

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(requiredFeatures)
                .setOutputCol("features");

        StringIndexer labelIndexer = new StringIndexer()
                .setInputCol(labelColumn)
                .setOutputCol("label_indexed")
                .setHandleInvalid("keep");

        RandomForestClassifier rf = new RandomForestClassifier()
                .setLabelCol("label_indexed")
                .setFeaturesCol("features")
                .setNumTrees(config.getInt("ml.num-trees"))
                .setMaxDepth(config.getInt("ml.max-depth"))
                .setImpurity(config.getString("ml.impurity"))
                .setFeatureSubsetStrategy(config.getString("ml.feature-subset-strategy"))
                .setSeed(config.getLong("ml.seed"));

        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{labelIndexer, assembler, rf});

        log.info("Starting Random Forest training with {} trees and max depth {}",
                config.getInt("ml.num-trees"), config.getInt("ml.max-depth"));

        PipelineModel model = pipeline.fit(trainingData);

        model.write().overwrite().save(modelPath);

        RandomForestClassificationModel rfModel = (RandomForestClassificationModel) model.stages()[2];
        log.info("Model trained successfully. Accuracy on training data: {}",
                rfModel.summary().accuracy());

        log.info("Model saved to: {}", modelPath);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            log.error("Usage: ModelTrainer <training-data-path> <model-output-path>");
            System.exit(1);
        }

        String dataPath = args[0];
        String modelPath = args[1];

        SparkSession spark = SparkSession.builder()
                .appName("IDS-Model-Trainer")
                .master(config.getString("spark.master"))
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> data = spark.read()
                .option("header", "true")
                .option("inferSchema", "true")
                .csv(dataPath);

        data = normalizeColumnNames(data);

        // Replace NaN/Infinity in numeric feature columns
        data = data.na().drop();
        for (String c : config.getStringList("ml.feature-columns")) {
            data = data.withColumn(c,
                    functions.expr("IF(isnan(" + c + ") OR (" + c + " = double('Infinity')) OR (" + c + " = double('-Infinity')), 0.0, " + c + ")"));
        }

        String[] cols = data.columns();
        log.info("Loaded training data: {} rows, columns: {}", data.count(), Arrays.toString(cols));

        // Verify label column exists
        boolean hasLabel = false;
        for (String c : cols) {
            if (c.equals("label")) { hasLabel = true; break; }
        }
        if (!hasLabel) {
            log.error("Label column 'label' not found! Available columns: {}", Arrays.toString(cols));
            for (String c : cols) {
                log.info("Col[{}] = '{}' (bytes: {})", c, c, java.util.Arrays.toString(c.chars().toArray()));
            }
            System.exit(1);
        }

        train(spark, data, modelPath);

        spark.stop();
    }
}
