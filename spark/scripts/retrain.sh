#!/bin/bash
# Auto-retrain script: trains a new model from collected training data
set -e

TRAINING_DIR="/opt/spark/app/training_data"
MODEL_DIR="/opt/spark/app/models/streaming-model"
LOG_FILE="/opt/spark/app/retrain.log"

echo "[$(date)] Starting model retraining..." >> "$LOG_FILE"

# Check training data exists
PARQUET_COUNT=$(ls "$TRAINING_DIR"/*.parquet 2>/dev/null | wc -l)
if [ "$PARQUET_COUNT" -lt 1 ]; then
    echo "[$(date)] No training data found. Skipping." >> "$LOG_FILE"
    exit 0
fi

# Run the retrainer
cd /opt/spark/app
/opt/spark/bin/spark-submit \
    --class com.ids.spark.ml.ModelRetrainer \
    --master local[2] \
    --conf spark.executor.memory=4g \
    --conf spark.driver.memory=4g \
    --conf spark.jars.ivy=/tmp/ivy \
    --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.3,org.postgresql:postgresql:42.7.1,com.typesafe:config:1.4.3 \
    target/ids-spark-1.0.0.jar \
    "$TRAINING_DIR" "$MODEL_DIR" >> "$LOG_FILE" 2>&1

if [ $? -eq 0 ]; then
    echo "[$(date)] Retraining succeeded." >> "$LOG_FILE"
else
    echo "[$(date)] Retraining failed!" >> "$LOG_FILE"
fi
