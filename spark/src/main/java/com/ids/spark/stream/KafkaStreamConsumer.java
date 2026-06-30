package com.ids.spark.stream;

import com.typesafe.config.Config;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import static org.apache.spark.sql.functions.*;

public class KafkaStreamConsumer {

    public static Dataset<Row> consume(SparkSession spark, Config config) {
        String bootstrapServers = config.getString("kafka.bootstrap-servers");
        String topic = config.getString("kafka.topic");

        return spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", bootstrapServers)
                .option("subscribe", topic)
                .option("startingOffsets", config.getString("kafka.starting-offsets"))
                .option("failOnDataLoss", config.getBoolean("kafka.fail-on-data-loss"))
                .option("maxOffsetsPerTrigger", config.getInt("kafka.max-offsets-per-trigger"))
                .load()
                .select(col("value").cast(DataTypes.StringType).alias("raw_value"));
    }

    // CamelCase → snake_case mapping for JSON fields from the Kafka producer
    private static final java.util.Map<String, String> FIELD_MAP = java.util.Map.ofEntries(
            java.util.Map.entry("srcIp", "src_ip"),
            java.util.Map.entry("dstIp", "dst_ip"),
            java.util.Map.entry("srcPort", "src_port"),
            java.util.Map.entry("dstPort", "dst_port"),
            java.util.Map.entry("protocol", "protocol"),
            java.util.Map.entry("timestamp", "timestamp"),
            java.util.Map.entry("flowDuration", "flow_duration"),
            java.util.Map.entry("totalFwdPackets", "total_fwd_packets"),
            java.util.Map.entry("totalBackwardPackets", "total_backward_packets"),
            java.util.Map.entry("fwdPacketLengthMax", "fwd_packet_length_max"),
            java.util.Map.entry("fwdPacketLengthMin", "fwd_packet_length_min"),
            java.util.Map.entry("fwdPacketLengthMean", "fwd_packet_length_mean"),
            java.util.Map.entry("bwdPacketLengthMax", "bwd_packet_length_max"),
            java.util.Map.entry("bwdPacketLengthMin", "bwd_packet_length_min"),
            java.util.Map.entry("bwdPacketLengthMean", "bwd_packet_length_mean"),
            java.util.Map.entry("flowBytesPerSec", "flow_bytes_per_sec"),
            java.util.Map.entry("flowPacketsPerSec", "flow_packets_per_sec"),
            java.util.Map.entry("fwdIatTotal", "fwd_iat_total"),
            java.util.Map.entry("fwdIatMean", "fwd_iat_mean"),
            java.util.Map.entry("bwdIatTotal", "bwd_iat_total"),
            java.util.Map.entry("bwdIatMean", "bwd_iat_mean"),
            java.util.Map.entry("fwdPshFlags", "fwd_psh_flags"),
            java.util.Map.entry("bwdPshFlags", "bwd_psh_flags"),
            java.util.Map.entry("fwdUrgFlags", "fwd_urg_flags"),
            java.util.Map.entry("bwdUrgFlags", "bwd_urg_flags"),
            java.util.Map.entry("finFlagCount", "fin_flag_count"),
            java.util.Map.entry("synFlagCount", "syn_flag_count"),
            java.util.Map.entry("rstFlagCount", "rst_flag_count"),
            java.util.Map.entry("pshFlagCount", "psh_flag_count"),
            java.util.Map.entry("ackFlagCount", "ack_flag_count"),
            java.util.Map.entry("urgFlagCount", "urg_flag_count"),
            java.util.Map.entry("packetLengthMean", "packet_length_mean"),
            java.util.Map.entry("packetLengthStd", "packet_length_std"),
            java.util.Map.entry("packetLengthVariance", "packet_length_variance"),
            java.util.Map.entry("avgPacketSize", "avg_packet_size"),
            java.util.Map.entry("avgFwdSegmentSize", "avg_fwd_segment_size"),
            java.util.Map.entry("avgBwdSegmentSize", "avg_bwd_segment_size"),
            java.util.Map.entry("subflowFwdPackets", "subflow_fwd_packets"),
            java.util.Map.entry("subflowFwdBytes", "subflow_fwd_bytes"),
            java.util.Map.entry("subflowBwdPackets", "subflow_bwd_packets"),
            java.util.Map.entry("subflowBwdBytes", "subflow_bwd_bytes"),
            java.util.Map.entry("initWinBytesForward", "init_win_bytes_forward"),
            java.util.Map.entry("initWinBytesBackward", "init_win_bytes_backward"),
            java.util.Map.entry("activeMean", "active_mean"),
            java.util.Map.entry("idleMean", "idle_mean"),
            java.util.Map.entry("label", "true_label")
    );

    public static Dataset<Row> parseJsonStream(Dataset<Row> rawStream) {
        Dataset<Row> parsed = rawStream.select(
                get_json_object(col("raw_value"), "$.srcIp").alias("src_ip"),
                get_json_object(col("raw_value"), "$.dstIp").alias("dst_ip"),
                get_json_object(col("raw_value"), "$.srcPort").cast(DataTypes.IntegerType).alias("src_port"),
                get_json_object(col("raw_value"), "$.dstPort").cast(DataTypes.IntegerType).alias("dst_port"),
                get_json_object(col("raw_value"), "$.protocol").alias("protocol"),
                get_json_object(col("raw_value"), "$.timestamp").alias("timestamp"),
                get_json_object(col("raw_value"), "$.flowDuration").cast(DataTypes.DoubleType).alias("flow_duration"),
                get_json_object(col("raw_value"), "$.totalFwdPackets").cast(DataTypes.DoubleType).alias("total_fwd_packets"),
                get_json_object(col("raw_value"), "$.totalBackwardPackets").cast(DataTypes.DoubleType).alias("total_backward_packets"),
                get_json_object(col("raw_value"), "$.fwdPacketLengthMax").cast(DataTypes.DoubleType).alias("fwd_packet_length_max"),
                get_json_object(col("raw_value"), "$.fwdPacketLengthMin").cast(DataTypes.DoubleType).alias("fwd_packet_length_min"),
                get_json_object(col("raw_value"), "$.fwdPacketLengthMean").cast(DataTypes.DoubleType).alias("fwd_packet_length_mean"),
                get_json_object(col("raw_value"), "$.bwdPacketLengthMax").cast(DataTypes.DoubleType).alias("bwd_packet_length_max"),
                get_json_object(col("raw_value"), "$.bwdPacketLengthMin").cast(DataTypes.DoubleType).alias("bwd_packet_length_min"),
                get_json_object(col("raw_value"), "$.bwdPacketLengthMean").cast(DataTypes.DoubleType).alias("bwd_packet_length_mean"),
                get_json_object(col("raw_value"), "$.flowBytesPerSec").cast(DataTypes.DoubleType).alias("flow_bytes_per_sec"),
                get_json_object(col("raw_value"), "$.flowPacketsPerSec").cast(DataTypes.DoubleType).alias("flow_packets_per_sec"),
                get_json_object(col("raw_value"), "$.fwdIatTotal").cast(DataTypes.DoubleType).alias("fwd_iat_total"),
                get_json_object(col("raw_value"), "$.fwdIatMean").cast(DataTypes.DoubleType).alias("fwd_iat_mean"),
                get_json_object(col("raw_value"), "$.bwdIatTotal").cast(DataTypes.DoubleType).alias("bwd_iat_total"),
                get_json_object(col("raw_value"), "$.bwdIatMean").cast(DataTypes.DoubleType).alias("bwd_iat_mean"),
                get_json_object(col("raw_value"), "$.fwdPshFlags").cast(DataTypes.DoubleType).alias("fwd_psh_flags"),
                get_json_object(col("raw_value"), "$.bwdPshFlags").cast(DataTypes.DoubleType).alias("bwd_psh_flags"),
                get_json_object(col("raw_value"), "$.fwdUrgFlags").cast(DataTypes.DoubleType).alias("fwd_urg_flags"),
                get_json_object(col("raw_value"), "$.bwdUrgFlags").cast(DataTypes.DoubleType).alias("bwd_urg_flags"),
                get_json_object(col("raw_value"), "$.finFlagCount").cast(DataTypes.DoubleType).alias("fin_flag_count"),
                get_json_object(col("raw_value"), "$.synFlagCount").cast(DataTypes.DoubleType).alias("syn_flag_count"),
                get_json_object(col("raw_value"), "$.rstFlagCount").cast(DataTypes.DoubleType).alias("rst_flag_count"),
                get_json_object(col("raw_value"), "$.pshFlagCount").cast(DataTypes.DoubleType).alias("psh_flag_count"),
                get_json_object(col("raw_value"), "$.ackFlagCount").cast(DataTypes.DoubleType).alias("ack_flag_count"),
                get_json_object(col("raw_value"), "$.urgFlagCount").cast(DataTypes.DoubleType).alias("urg_flag_count"),
                get_json_object(col("raw_value"), "$.packetLengthMean").cast(DataTypes.DoubleType).alias("packet_length_mean"),
                get_json_object(col("raw_value"), "$.packetLengthStd").cast(DataTypes.DoubleType).alias("packet_length_std"),
                get_json_object(col("raw_value"), "$.packetLengthVariance").cast(DataTypes.DoubleType).alias("packet_length_variance"),
                get_json_object(col("raw_value"), "$.avgPacketSize").cast(DataTypes.DoubleType).alias("avg_packet_size"),
                get_json_object(col("raw_value"), "$.avgFwdSegmentSize").cast(DataTypes.DoubleType).alias("avg_fwd_segment_size"),
                get_json_object(col("raw_value"), "$.avgBwdSegmentSize").cast(DataTypes.DoubleType).alias("avg_bwd_segment_size"),
                get_json_object(col("raw_value"), "$.subflowFwdPackets").cast(DataTypes.DoubleType).alias("subflow_fwd_packets"),
                get_json_object(col("raw_value"), "$.subflowFwdBytes").cast(DataTypes.DoubleType).alias("subflow_fwd_bytes"),
                get_json_object(col("raw_value"), "$.subflowBwdPackets").cast(DataTypes.DoubleType).alias("subflow_bwd_packets"),
                get_json_object(col("raw_value"), "$.subflowBwdBytes").cast(DataTypes.DoubleType).alias("subflow_bwd_bytes"),
                get_json_object(col("raw_value"), "$.initWinBytesForward").cast(DataTypes.DoubleType).alias("init_win_bytes_forward"),
                get_json_object(col("raw_value"), "$.initWinBytesBackward").cast(DataTypes.DoubleType).alias("init_win_bytes_backward"),
                get_json_object(col("raw_value"), "$.activeMean").cast(DataTypes.DoubleType).alias("active_mean"),
                get_json_object(col("raw_value"), "$.idleMean").cast(DataTypes.DoubleType).alias("idle_mean"),
                get_json_object(col("raw_value"), "$.label").cast(DataTypes.DoubleType).alias("true_label")
        );
        return parsed;
    }
}
