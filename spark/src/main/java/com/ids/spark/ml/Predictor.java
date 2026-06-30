package com.ids.spark.ml;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Predictor {

    private static final Logger log = LoggerFactory.getLogger(Predictor.class);
    private static final Config config = ConfigFactory.load();

    // Model hot-reload cache
    private static PipelineModel cachedModel = null;
    private static String cachedModelPath = null;
    private static long cachedModelTimestamp = 0;

    private static synchronized PipelineModel loadModelWithReload(String modelPath) {
        java.io.File modelFile = new java.io.File(modelPath.replace("file://", ""));
        if (!modelFile.exists()) {
            cachedModel = null;
            return null;
        }
        long lastMod = modelFile.lastModified();
        if (cachedModel == null || !modelPath.equals(cachedModelPath) || lastMod > cachedModelTimestamp) {
            log.info("Loading model from: {} (modified: {})", modelPath, lastMod);
            cachedModel = PipelineModel.load(modelPath);
            cachedModelPath = modelPath;
            cachedModelTimestamp = lastMod;
        }
        return cachedModel;
    }

    public static Dataset<Row> predictBatch(Dataset<Row> featureDf, String modelPath) {
        List<String> featureColumns = config.getStringList("ml.feature-columns");

        PipelineModel model = loadModelWithReload(modelPath);
        if (model == null) {
            log.warn("Model not found at {}. Using rule-based fallback.", modelPath);
            return ruleBasedFallback(featureDf);
        }

        String[] requiredFeatures = featureColumns.toArray(new String[0]);

        Dataset<Row> featuresDf = featureDf;
        for (String col : requiredFeatures) {
            if (!hasColumn(featuresDf, col)) {
                featuresDf = featuresDf.withColumn(col, functions.lit(0.0));
            }
        }

        // Add string-typed label column for StringIndexer compatibility
        String labelColumn = config.getString("ml.label-column");
        featuresDf = featuresDf.withColumn(labelColumn, functions.lit("Unknown"));

        Dataset<Row> predictions = model.transform(featuresDf);

        Dataset<Row> result = classifyCicidsAttackType(predictions);

        log.info("Batch prediction completed.");

        return result;
    }

    // Map numeric prediction to CICIDS2017 attack type based on feature patterns
    private static Dataset<Row> classifyCicidsAttackType(Dataset<Row> df) {
        return df.withColumn("prediction_label",
                functions.when(functions.col("prediction").equalTo(0.0), functions.lit("BENIGN"))
                        // DDoS: very high flow_bytes_per_sec + high packet rate
                        .when(functions.col("flow_bytes_per_sec").gt(500000)
                                .and(functions.col("flow_packets_per_sec").gt(50000)),
                                functions.lit("DDoS"))
                        // DoS Hulk: high total_fwd_packets + high bytes
                        .when(functions.col("total_fwd_packets").gt(5000)
                                .and(functions.col("flow_bytes_per_sec").gt(200000)),
                                functions.lit("DoS Hulk"))
                        // Port Scan: many syn flags + low duration
                        .when(functions.col("syn_flag_count").gt(200)
                                .and(functions.col("flow_duration").lt(1000)),
                                functions.lit("PortScan"))
                        // Brute Force: many fin/rst flags + moderate packets
                        .when(functions.col("fin_flag_count").gt(100)
                                .or(functions.col("rst_flag_count").gt(50)),
                                functions.lit("Brute Force"))
                        // Bot: high bwd packets + abnormal packet length
                        .when(functions.col("total_backward_packets").gt(5000)
                                .and(functions.col("packet_length_mean").lt(100)),
                                functions.lit("Bot"))
                        // Web Attack: high fwd packets + specific flag patterns
                        .when(functions.col("total_fwd_packets").gt(3000)
                                .and(functions.col("fwd_psh_flags").gt(50)),
                                functions.lit("Web Attack - XSS"))
                        // Generic Attack: anything else flagged
                        .otherwise(functions.lit("Infiltration")));
    }

    public static Dataset<Row> ruleBasedFallback(Dataset<Row> featureDf) {
        log.info("Using rule-based fallback detection");

        List<String> featureColumns = config.getStringList("ml.feature-columns");

        Dataset<Row> result = featureDf
                .withColumn("prediction",
                        functions.when(
                                functions.col("flow_bytes_per_sec").gt(100000)
                                        .or(functions.col("flow_packets_per_sec").gt(10000))
                                        .or(functions.col("syn_flag_count").gt(100))
                                        .or(functions.col("fin_flag_count").gt(100))
                                        .or(functions.col("rst_flag_count").gt(50))
                                        .or(functions.col("fwd_iat_mean").isNull()
                                                .and(functions.col("flow_duration").gt(0)))
                                        .or(functions.col("total_fwd_packets").gt(5000))
                                        .or(functions.col("total_backward_packets").gt(5000)),
                                1.0
                        ).otherwise(0.0)
                );

        result = classifyCicidsAttackType(result);

        return result;
    }

    private static boolean hasColumn(Dataset<Row> df, String colName) {
        return java.util.Arrays.asList(df.columns()).contains(colName);
    }
}
