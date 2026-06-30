param(
    [string]$Action = "start"
)

$ROOT = Split-Path -Parent $PSScriptRoot

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Start-Infrastructure {
    Write-Step "Starting Kafka, PostgreSQL, Zookeeper..."
    docker compose -f "$ROOT\docker-compose.yml" up -d zookeeper kafka postgres
    Write-Host "Waiting for Kafka to be ready..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
}

function Start-Spark {
    Write-Step "Starting Spark cluster..."
    docker compose -f "$ROOT\docker-compose.yml" up -d spark-master spark-worker
    Start-Sleep -Seconds 10
}

function Start-Backend {
    Write-Step "Building and starting Spring Boot backend..."
    docker compose -f "$ROOT\docker-compose.yml" up -d backend
}

function Start-Frontend {
    Write-Step "Building and starting React dashboard..."
    docker compose -f "$ROOT\docker-compose.yml" up -d frontend
}

function Start-Producer {
    Write-Step "Starting Kafka log producer..."
    docker compose -f "$ROOT\docker-compose.yml" up -d suricata
    Write-Host "`nTo start the synthetic data producer manually:" -ForegroundColor Yellow
    Write-Host "  cd $ROOT\kafka\producer" -ForegroundColor Gray
    Write-Host "  mvn exec:java -Dexec.mainClass=com.ids.producer.LogProducer" -ForegroundColor Gray
}

function Start-Grafana {
    Write-Step "Starting Grafana monitoring..."
    docker compose -f "$ROOT\docker-compose.yml" up -d grafana
}

function Show-Urls {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  IDS Platform is running!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  Dashboard : http://localhost:3000" -ForegroundColor White
    Write-Host "  Backend   : http://localhost:8082" -ForegroundColor White
    Write-Host "  Spark     : http://localhost:8080" -ForegroundColor White
    Write-Host "  Grafana   : http://localhost:3001 (admin/admin)" -ForegroundColor White
    Write-Host "  Kafka     : localhost:29092" -ForegroundColor White
    Write-Host "  PostgreSQL: localhost:5432 (ids_user/ids_password)" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Green
}

function Stop-All {
    Write-Step "Stopping all services..."
    docker compose -f "$ROOT\docker-compose.yml" down
    Write-Host "All services stopped." -ForegroundColor Green
}

function Show-Logs {
    param([string]$Service)
    docker compose -f "$ROOT\docker-compose.yml" logs -f $Service
}

switch ($Action) {
    "start" {
        Start-Infrastructure
        Start-Spark
        Start-Backend
        Start-Frontend
        Start-Grafana
        Show-Urls
    }
    "stop" { Stop-All }
    "restart" { Stop-All; Start-Sleep 5; & $PSCommandPath "start" }
    "logs" { Show-Logs -Service $args[0] }
    "infra" { Start-Infrastructure }
    "spark" { Start-Spark }
    "backend" { Start-Backend }
    "frontend" { Start-Frontend }
    "producer" { Start-Producer }
    default {
        Write-Host "Usage: .\deploy.ps1 [start|stop|restart|infra|spark|backend|frontend|producer|logs <service>]"
    }
}
