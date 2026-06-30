import java.io.File
val dir = new File("/opt/spark/app/training_data")
val parquetFiles = dir.listFiles.filter(f => f.getName.endsWith(".parquet"))
for (f <- parquetFiles) {
  try {
    val count = spark.read.parquet(f.getPath).count()
    println(s"OK: ${f.getName} -> $count rows")
  } catch {
    case e: Exception => println(s"BAD: ${f.getName} -> ${e.getMessage.take(100)}")
  }
}
// done
