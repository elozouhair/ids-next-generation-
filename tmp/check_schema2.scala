import org.apache.spark.sql.functions._
val df = spark.read.parquet("/opt/spark/app/training_data")
val cols = Array("flow_bytes_per_sec", "syn_flag_count", "flow_duration", "total_fwd_packets", "true_label")
val nullCounts = cols.map(c => sum(when(col(c).isNull, 1).otherwise(0)).alias(c))
df.select(nullCounts: _*).show()
println("Non-null sample:")
df.filter("flow_bytes_per_sec IS NOT NULL").select("flow_bytes_per_sec", "syn_flag_count", "true_label").show(5, false)
println("Total count:")
println(df.count())
println("Non-null flow_bytes_per_sec count:")
df.filter("flow_bytes_per_sec IS NOT NULL").count()
// done
