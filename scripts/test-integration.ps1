# =============================================================================
# SCRIPT DE TEST D'INTÉGRATION — IDS Pipeline
# =============================================================================
# Ce script vérifie que tous les composants du pipeline fonctionnent
# Usage: .\scripts\test-integration.ps1
# =============================================================================

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST D'INTÉGRATION — IDS Pipeline" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passed = 0
$failed = 0
$total = 0

function Test-Component {
    param($name, $test)
    $script:total++
    Write-Host "[$script:total] $name" -NoNewline
    try {
        $result = & $test
        if ($result) {
            Write-Host " [PASS]" -ForegroundColor Green
            $script:passed++
        } else {
            Write-Host " [FAIL]" -ForegroundColor Red
            $script:failed++
        }
    } catch {
        Write-Host " [ERROR] $($_.Exception.Message)" -ForegroundColor Red
        $script:failed++
    }
}

# =============================================================================
# 1. TESTS DES CONTAINERS
# =============================================================================
Write-Host ""
Write-Host "--- 1. TESTS DES CONTAINERS ---" -ForegroundColor Yellow

$containers = @(
    "ids-postgres",
    "ids-zookeeper",
    "ids-kafka",
    "ids-spark-master",
    "ids-spark-worker",
    "ids-backend",
    "ids-frontend",
    "ids-grafana",
    "ids-producer"
)

foreach ($c in $containers) {
    Test-Component "Container $c" {
        $status = docker inspect --format '{{.State.Status}}' $c 2>$null
        return $status -eq "running"
    }
}

# =============================================================================
# 2. TESTS DES PORTS
# =============================================================================
Write-Host ""
Write-Host "--- 2. TESTS DES PORTS ---" -ForegroundColor Yellow

$ports = @{
    "PostgreSQL" = 5432
    "Zookeeper" = 2181
    "Kafka" = 9092
    "Spark Master" = 8080
    "Spark Worker" = 8081
    "Backend" = 8082
    "Frontend" = 3000
    "Grafana" = 3001
}

foreach ($name in $ports.Keys) {
    $port = $ports[$name]
    Test-Component "Port $name ($port)" {
        $result = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
        return $result.TcpTestSucceeded
    }
}

# =============================================================================
# 3. TESTS DES API ENDPOINTS
# =============================================================================
Write-Host ""
Write-Host "--- 3. TESTS DES API ENDPOINTS ---" -ForegroundColor Yellow

Test-Component "GET /api/dashboard" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8082/api/dashboard" -TimeoutSec 30 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

Test-Component "GET /api/alerts" {
    try {
        $r = Invoke-WebRequest -Uri 'http://localhost:8082/api/alerts?page=0&size=5' -TimeoutSec 30 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

Test-Component "GET /api/alerts/count" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8082/api/alerts/count" -TimeoutSec 30 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

Test-Component "GET /api/geo/recent" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8082/api/geo/recent" -TimeoutSec 30 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

# =============================================================================
# 4. TESTS DES FRONTENDS
# =============================================================================
Write-Host ""
Write-Host "--- 4. TESTS DES FRONTENDS ---" -ForegroundColor Yellow

Test-Component "Frontend React (port 3000)" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:3000" -TimeoutSec 15 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

Test-Component "Grafana (port 3001)" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:3001/login" -TimeoutSec 15 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

Test-Component "Spark Master UI (port 8080)" {
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 15 -ErrorAction Stop
        return $r.StatusCode -eq 200
    } catch { return $false }
}

# =============================================================================
# 5. TESTS DE LA BASE DE DONNÉES
# =============================================================================
Write-Host ""
Write-Host "--- 5. TESTS DE LA BASE DE DONNÉES ---" -ForegroundColor Yellow

Test-Component "PostgreSQL connection" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -c "SELECT 1" 2>$null
    return $LASTEXITCODE -eq 0
}

Test-Component "Table 'alerts' existe" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM alerts" 2>$null
    return $LASTEXITCODE -eq 0
}

Test-Component "Table 'attacks_geo' existe" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM attacks_geo" 2>$null
    return $LASTEXITCODE -eq 0
}

Test-Component "Table 'traffic_stats' existe" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM traffic_stats" 2>$null
    return $LASTEXITCODE -eq 0
}

Test-Component "PostGIS extension active" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c 'SELECT version()' 2>$null
    return $result -match "PostgreSQL"
}

# =============================================================================
# 6. TESTS DU KAFKA
# =============================================================================
Write-Host ""
Write-Host "--- 6. TESTS DU KAFKA ---" -ForegroundColor Yellow

Test-Component "Kafka topic 'network-traffic'" {
    $result = docker exec ids-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>$null
    return $result -match "network-traffic"
}

Test-Component "Kafka topic 'network-traffic-alerts'" {
    $result = docker exec ids-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>$null
    return $result -match "network-traffic-alerts"
}

# =============================================================================
# 7. TESTS DU SPARK
# =============================================================================
Write-Host ""
Write-Host "--- 7. TESTS DU SPARK ---" -ForegroundColor Yellow

Test-Component "Spark Master alive" {
    $result = docker exec ids-spark-master curl -s http://localhost:8080/json/ 2>$null
    return $result -match "status"
}

Test-Component "Spark Worker connected" {
    $result = docker exec ids-spark-master curl -s http://localhost:8080/json/ 2>$null
    return $result -match "workers"
}

Test-Component "Streaming job active" {
    $result = docker exec ids-spark-master sh -c "ps aux | grep -v grep | grep SparkSubmit | wc -l" 2>$null
    return $result.Trim() -ne "0"
}

# =============================================================================
# 8. TESTS DES DONNÉES
# =============================================================================
Write-Host ""
Write-Host "--- 8. TESTS DES DONNÉES ---" -ForegroundColor Yellow

Test-Component "Données dans 'alerts'" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM alerts" 2>$null
    return [int]$result.Trim() -gt 0
}

Test-Component "Données dans 'attacks_geo'" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM attacks_geo" 2>$null
    return [int]$result.Trim() -gt 0
}

Test-Component "Données dans 'traffic_stats'" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM traffic_stats" 2>$null
    return [int]$result.Trim() -gt 0
}

Test-Component "Types CICIDS dans alerts" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(DISTINCT attack_type) FROM alerts" 2>$null
    return [int]$result.Trim() -gt 2
}

Test-Component "Modèle ML entraîné" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM model_metrics" 2>$null
    return [int]$result.Trim() -gt 0
}

Test-Component "Accuracy = 1.0" {
    $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT accuracy FROM model_metrics ORDER BY batch_id DESC LIMIT 1" 2>$null
    return $result.Trim() -eq "1"
}

# =============================================================================
# 9. TESTS DES INDEX
# =============================================================================
Write-Host ""
Write-Host "--- 9. TESTS DES INDEX ---" -ForegroundColor Yellow

$indexes = @(
    "idx_alerts_timestamp",
    "idx_alerts_attack_type",
    "idx_alerts_src_ip",
    "idx_alerts_severity",
    "idx_traffic_stats_timestamp",
    "idx_attacks_geo_timestamp"
)

foreach ($idx in $indexes) {
    Test-Component "Index $idx" {
        $result = docker exec ids-postgres psql -U ids_user -d ids_db -t -c "SELECT COUNT(*) FROM pg_indexes WHERE indexname='$idx'" 2>$null
        return $result.Trim() -eq "1"
    }
}

# =============================================================================
# RÉSUMÉ
# =============================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RÉSUMÉ DES TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Total: $total tests" -ForegroundColor White
Write-Host "Passés: $passed" -ForegroundColor Green
Write-Host "Échoués: $failed" -ForegroundColor Red
Write-Host ""

if ($failed -eq 0) {
    Write-Host "TOUS LES TESTS SONT PASSÉS !" -ForegroundColor Green
    Write-Host "Le pipeline est opérationnel." -ForegroundColor Green
} else {
    Write-Host "$failed test(s) ont échoué." -ForegroundColor Red
    Write-Host "Vérifiez les composants en erreur." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
