val df = spark.read.parquet("/opt/spark/app/training_data")
println("Parquet columns:")
df.printSchema()
println("Sample row:")
df.select("true_label", "raw_features", "flow_bytes_per_sec", "syn_flag_count").show(3, false)
// Check null counts
import org.apache.spark.sql.functions._
val cols = Array("flow_bytes_per_sec", "syn_flag_count", "flow_duration", "total_fwd_packets", "true_label")
val nullCounts = cols.map(c => sum(when(col(c).isNull, 1).otherwise(0)).alias(c))
println("Null counts:")
df.select(nullCounts: _*).show()
// Check a non-null row's raw_features
println("First raw_features sample:")
df.select("raw_features").show(1, false)
// done
