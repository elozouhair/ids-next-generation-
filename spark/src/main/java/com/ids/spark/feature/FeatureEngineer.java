package com.ids.spark.feature;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FeatureEngineer {

    private static final Logger log = LoggerFactory.getLogger(FeatureEngineer.class);
    private static final Config config = ConfigFactory.load();

    public static Dataset<Row> extractFeatures(Dataset<Row> df) {
        List<String> featureColumns = config.getStringList("ml.feature-columns");

        Dataset<Row>
 cleanDf = df;

        for (String col : featureColumns) {
            if (!hasColumn(df, col)) {
                log.warn("Column '{}' not found in stream. Adding default 0.0", col);
                cleanDf = cleanDf.withColumn(col, functions.lit(0.0));
            }
        }

        cleanDf = cleanDf
                .withColumn("flow_duration",
                        functions.when(functions.col("flow_duration").isNull(), 0.0)
                                .otherwise(functions.col("flow_duration")))
                .withColumn("total_fwd_packets",
                        functions.when(functions.col("total_fwd_packets").isNull(), 0.0)
                                .otherwise(functions.col("total_fwd_packets")))
                .withColumn("total_backward_packets",
                        functions.when(functions.col("total_backward_packets").isNull(), 0.0)
                                .otherwise(functions.col("total_backward_packets")))
                .withColumn("flow_bytes_per_sec",
                        functions.when(functions.col("flow_bytes_per_sec").isNull()
                                        .or(functions.col("flow_bytes_per_sec").equalTo(Double.POSITIVE_INFINITY)), 0.0)
                                .otherwise(functions.col("flow_bytes_per_sec")))
                .withColumn("flow_packets_per_sec",
                        functions.when(functions.col("flow_packets_per_sec").isNull()
                                        .or(functions.col("flow_packets_per_sec").equalTo(Double.POSITIVE_INFINITY)), 0.0)
                                .otherwise(functions.col("flow_packets_per_sec")));

        List<String> numericCols = List.of(
                "fwd_packet_length_max", "fwd_packet_length_min", "fwd_packet_length_mean",
                "bwd_packet_length_max", "bwd_packet_length_min", "bwd_packet_length_mean",
                "fwd_iat_total", "fwd_iat_mean", "bwd_iat_total", "bwd_iat_mean",
                "fwd_psh_flags", "bwd_psh_flags", "fwd_urg_flags", "bwd_urg_flags",
                "fin_flag_count", "syn_flag_count", "rst_flag_count",
                "psh_flag_count", "ack_flag_count", "urg_flag_count",
                "packet_length_mean", "packet_length_std", "packet_length_variance",
                "avg_packet_size", "avg_fwd_segment_size", "avg_bwd_segment_size",
                "subflow_fwd_packets", "subflow_fwd_bytes",
                "subflow_bwd_packets", "subflow_bwd_bytes",
                "init_win_bytes_forward", "init_win_bytes_backward",
                "active_mean", "idle_mean"
        );

        for (String col : numericCols) {
            if (hasColumn(df, col)) {
                cleanDf = cleanDf
                        .withColumn(col,
                                functions.when(functions.col(col).isNull(), 0.0)
                                        .otherwise(functions.col(col)));
            }
        }

        log.info("Feature engineering completed. {} features prepared.", featureColumns.size());
        cleanDf.printSchema();
        return cleanDf;
    }

    private static boolean hasColumn(Dataset<Row> df, String colName) {
        return java.util.Arrays.asList(df.columns()).contains(colName);
    }
}
