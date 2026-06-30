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

import java.util.List;

public class ModelRetrainer {

    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.load();

        String trainingDir = args.length > 0 ? args[0] : config.getString("spark.training-data-path");
        String modelPath = args.length > 1 ? args[1] : "/opt/spark/app/models/streaming-model";

        SparkSession spark = SparkSession.builder()
                .appName("IDS-Model-Retrainer")
                .master(config.getString("spark.master"))
                .config("spark.sql.shuffle.partitions", "4")
                .getOrCreate();

        spark.sparkContext().setLogLevel("WARN");

        Dataset<Row> data = spark.read().parquet(trainingDir);
        long totalRows = data.count();
        System.out.println("=== Loaded " + totalRows + " training rows from " + trainingDir + " ===");

        if (totalRows < 100) {
            System.out.println("=== Not enough training data (need >= 100 rows). Skipping retrain. ===");
            spark.stop();
            return;
        }

        String[] cols = data.columns();
        boolean hasLabel = false;
        for (String c : cols) {
            if (c.equals("true_label")) { hasLabel = true; break; }
        }
        if (!hasLabel) {
            System.out.println("=== No true_label column found. Skipping retrain. ===");
            spark.stop();
            return;
        }

        // Convert numeric true_label (0.0/1.0) to string label (Normal/Attack)
        Dataset<Row> prepared = data
                .withColumn("label",
                        functions.when(functions.col("true_label").equalTo(1.0), functions.lit("Attack"))
                                .otherwise(functions.lit("Normal")))
                .drop("true_label");

        // Handle NaN/Infinity/null in numeric feature columns
        for (String c : config.getStringList("ml.feature-columns")) {
            prepared = prepared.withColumn(c,
                    functions.coalesce(
                            functions.expr("IF(isnan(" + c + ") OR (" + c + " = double('Infinity')) OR (" + c + " = double('-Infinity')), 0.0, " + c + ")"),
                            functions.lit(0.0)
                    ));
        }

        // Only drop rows where label is null
        prepared = prepared.filter(functions.col("label").isNotNull());
        long cleanRows = prepared.count();
        System.out.println("=== Cleaned data: " + cleanRows + " rows ===");

        if (cleanRows < 100) {
            System.out.println("=== Not enough clean rows after filtering. Skipping retrain. ===");
            spark.stop();
            return;
        }

        List<String> featureColumns = config.getStringList("ml.feature-columns");
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(featureColumns.toArray(new String[0]))
                .setOutputCol("features");

        StringIndexer labelIndexer = new StringIndexer()
                .setInputCol("label")
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

        System.out.println("=== Training Random Forest with " + config.getInt("ml.num-trees") + " trees on " + cleanRows + " rows ===");

        PipelineModel model = pipeline.fit(prepared);

        model.write().overwrite().save(modelPath);

        RandomForestClassificationModel rfModel = (RandomForestClassificationModel) model.stages()[2];
        System.out.println("=== Model retrained and saved to " + modelPath + " ===");
        System.out.println("=== Training accuracy: " + rfModel.summary().accuracy() + " ===");

        spark.stop();
    }
}
