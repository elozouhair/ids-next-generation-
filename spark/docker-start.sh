#!/bin/bash
set -e

SPARK_MASTER_HOST="${SPARK_MASTER_HOST:-spark-master}"
SPARK_MASTER_PORT="${SPARK_MASTER_PORT:-7077}"
SPARK_MASTER_WEBUI_PORT="${SPARK_MASTER_WEBUI_PORT:-8080}"

/opt/spark/bin/spark-class org.apache.spark.deploy.master.Master \
  --host "$SPARK_MASTER_HOST" \
  --port "$SPARK_MASTER_PORT" \
  --webui-port "$SPARK_MASTER_WEBUI_PORT" &
MASTER_PID=$!
echo "=== Spark Master started (PID: $MASTER_PID) ==="

# Give Spark master time to initialise and dependencies (PostgreSQL, Kafka) to settle
sleep 20

echo "=== Starting Spark streaming job ==="
rm -rf /opt/spark/app/checkpoint/*

JAR="/opt/spark/app/target/ids-spark-1.0.0.jar"
if [ ! -f "$JAR" ]; then
  JAR="/opt/spark/app/spark-ids-1.0.0-shaded.jar"
fi

nohup /opt/spark/bin/spark-submit \
  --class com.ids.spark.IdsSparkJob \
  --master local[2] \
  --driver-memory 512m \
  --conf spark.sql.streaming.schemaInference=true \
  "$JAR" \
  >> /opt/spark/app/streaming_run.log 2>&1 &
STREAMING_PID=$!
echo "=== Streaming job submitted (PID: $STREAMING_PID) ==="

# Monitor streaming job; restart if it dies while master is alive
while kill -0 "$MASTER_PID" 2>/dev/null; do
  if ! kill -0 "$STREAMING_PID" 2>/dev/null; then
    echo "=== Streaming job died. Restarting... ==="
    rm -rf /opt/spark/app/checkpoint/*
    nohup /opt/spark/bin/spark-submit \
      --class com.ids.spark.IdsSparkJob \
      --master local[2] \
      --driver-memory 512m \
      --conf spark.sql.streaming.schemaInference=true \
      "$JAR" \
      >> /opt/spark/app/streaming_run.log 2>&1 &
    STREAMING_PID=$!
    echo "=== Streaming job restarted (PID: $STREAMING_PID) ==="
  fi
  sleep 30
done
