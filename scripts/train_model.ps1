param(
    [string]$DatasetPath = ".\cicids2017\MachineLearningCVE\Friday-WorkingHours-Afternoon-DDos.pcap_ISCX.csv",
    [string]$ModelOutputPath = ".\spark\models\random-forest-model"
)

$ErrorActionPreference = "Stop"
$ROOT = Split-Path -Parent $PSScriptRoot

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  IDS Model Training Pipeline" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if (-not (Test-Path $DatasetPath)) {
    Write-Host "Dataset not found at: $DatasetPath" -ForegroundColor Yellow
    Write-Host "Downloading CICIDS2017 sample dataset..." -ForegroundColor Yellow

    $url = "https://www.unb.ca/cic/datasets/IDS-2017.html"
    Write-Host "Please download CICIDS2017 from: $url" -ForegroundColor Yellow
    Write-Host "Then place CSV files in: $ROOT\cicids2017\" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nStep 1: Building Spark job JAR..." -ForegroundColor Green
Set-Location -Path "$ROOT\spark"
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "`nStep 2: Training Random Forest model..." -ForegroundColor Green
docker compose -f "$ROOT\docker-compose.yml" exec -T spark-master `
    spark-submit `
    --class com.ids.spark.ml.ModelTrainer `
    --master spark://spark-master:7077 `
    /opt/bitnami/spark/app/target/ids-spark-1.0.0.jar `
    /opt/bitnami/spark/app/data/training.csv `
    /opt/bitnami/spark/app/models/random-forest-model

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  Model trained successfully!" -ForegroundColor Green
    Write-Host "  Model saved to: $ModelOutputPath" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host "Model training failed!" -ForegroundColor Red
}
