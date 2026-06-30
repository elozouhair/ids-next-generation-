$ErrorActionPreference = "Stop"

Write-Host "=== IDS Spark Streaming Auto-Start ===" -ForegroundColor Cyan

# Wait for Kafka and PostgreSQL to be ready
Write-Host "Waiting for Kafka..." -ForegroundColor Yellow
$kafkaReady = $false
for ($i = 0; $i -lt 30; $i++) {
    $result = docker exec ids-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>&1
    if ($result -match "network-traffic") {
        $kafkaReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $kafkaReady) {
    Write-Host "ERROR: Kafka not ready after 60s" -ForegroundColor Red
    exit 1
}
Write-Host "Kafka ready!" -ForegroundColor Green

Write-Host "Waiting for PostgreSQL..." -ForegroundColor Yellow
$pgReady = $false
for ($i = 0; $i -lt 15; $i++) {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -c "SELECT 1" 2>&1
    if ($result -match "1 row") {
        $pgReady = $true
        break
    }
    Start-Sleep -Seconds 2
}
if (-not $pgReady) {
    Write-Host "ERROR: PostgreSQL not ready after 30s" -ForegroundColor Red
    exit 1
}
Write-Host "PostgreSQL ready!" -ForegroundColor Green

# Check if Spark streaming is already running
Write-Host "Checking for existing Spark streaming job..." -ForegroundColor Yellow
$existing = docker exec ids-spark-master ps aux 2>&1 | Select-String "IdsSparkJob"
if ($existing) {
    Write-Host "Spark streaming job already running (PID: $($existing[0]))" -ForegroundColor Green
    exit 0
}

# Clean checkpoint and start fresh
Write-Host "Cleaning old checkpoint..." -ForegroundColor Yellow
docker exec ids-spark-master rm -rf /opt/spark/app/checkpoint/* 2>&1 | Out-Null

Write-Host "Starting Spark streaming job (local[2])..." -ForegroundColor Cyan
docker exec ids-spark-master bash -c "nohup /opt/spark/bin/spark-submit --class com.ids.spark.IdsSparkJob --master local[2] --driver-memory 512m --conf spark.sql.streaming.schemaInference=true /opt/spark/app/target/ids-spark-1.0.0.jar >> /opt/spark/app/streaming_run.log 2>&1 &"

Start-Sleep -Seconds 15

# Verify it started
$check = docker exec ids-spark-master ps aux 2>&1 | Select-String "IdsSparkJob"
if ($check) {
    Write-Host "Spark streaming job started successfully!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Spark streaming job may not have started. Check logs." -ForegroundColor Red
}
