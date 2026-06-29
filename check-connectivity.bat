@echo off
chcp 65001 >nul
title IDS Pipeline - API Connectivity Test
color 0A

echo ============================================================
echo   IDS PIPELINE - Test de Connectivite API
echo   %date% %time%
echo ============================================================
echo.

set PASS=0
set FAIL=0

echo [1/7] PostgreSQL (port 5432)...
powershell -Command "Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet" >nul 2>&1
if %errorlevel%==0 (echo   [OK] PostgreSQL accessible & set /a PASS+=1) else (echo   [FAIL] PostgreSQL inaccessible & set /a FAIL+=1)

echo [2/7] Kafka (port 9092)...
powershell -Command "Test-NetConnection -ComputerName localhost -Port 9092 -InformationLevel Quiet" >nul 2>&1
if %errorlevel%==0 (echo   [OK] Kafka accessible & set /a PASS+=1) else (echo   [FAIL] Kafka inaccessible & set /a FAIL+=1)

echo [3/7] Spark Master UI (port 8080)...
powershell -Command "try { Invoke-WebRequest -Uri 'http://localhost:8080' -TimeoutSec 5 -UseBasicParsing | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 (echo   [OK] Spark Master actif & set /a PASS+=1) else (echo   [FAIL] Spark Master inaccessible & set /a FAIL+=1)

echo [4/7] Backend API - /api/dashboard (port 8082)...
powershell -Command "try { $r = Invoke-RestMethod -Uri 'http://localhost:8082/api/dashboard' -TimeoutSec 30; if($r.alerts_last_hour -gt 0){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 (echo   [OK] /api/dashboard repond & set /a PASS+=1) else (echo   [FAIL] /api/dashboard timeout/erreur & set /a FAIL+=1)

echo [5/7] Backend API - /api/geo/recent (port 8082)...
powershell -Command "try { $r = Invoke-RestMethod -Uri 'http://localhost:8082/api/geo/recent' -TimeoutSec 30; if($r.Count -gt 0){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 (echo   [OK] /api/geo/recent repond & set /a PASS+=1) else (echo   [FAIL] /api/geo/recent timeout/erreur & set /a FAIL+=1)

echo [6/7] Grafana (port 3001)...
powershell -Command "try { $r = Invoke-RestMethod -Uri 'http://localhost:3001/api/health' -TimeoutSec 5; if($r.database -eq 'ok'){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 (echo   [OK] Grafana actif & set /a PASS+=1) else (echo   [FAIL] Grafana inaccessible & set /a FAIL+=1)

echo [7/7] Frontend (port 3000)...
powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:3000' -TimeoutSec 5 -UseBasicParsing; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }" >nul 2>&1
if %errorlevel%==0 (echo   [OK] Frontend actif & set /a PASS+=1) else (echo   [FAIL] Frontend inaccessible & set /a FAIL+=1)

echo.
echo ============================================================
echo   RESULTATS: %PASS% OK / %FAIL% ECHEC sur 7 tests
echo ============================================================

if %FAIL%==0 (
    echo   Tous les services sont operationnels !
) else (
    echo   Certains services sont hors ligne.
)

echo.
pause
