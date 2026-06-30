import org.apache.spark.sql.functions._
val df = spark.read.parquet("/opt/spark/app/training_data")
println("Total count: " + df.count())
println("Schema:")
df.printSchema()
println("Null counts for features:")
val cols = Array("flow_bytes_per_sec", "syn_flag_count", "flow_duration", "total_fwd_packets", "true_label")
val nullCounts = cols.map(c => sum(when(col(c).isNull, 1).otherwise(0)).alias(c))
df.select(nullCounts: _*).show()
println("Sample data:")
df.select("flow_bytes_per_sec", "syn_flag_count", "flow_packets_per_sec", "true_label").show(5, false)
println("Label distribution:")
df.groupBy("true_label").count().show()
// done
