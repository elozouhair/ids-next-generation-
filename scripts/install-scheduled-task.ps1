$ErrorActionPreference = "Stop"
$taskName = "IDS-Spark-Streaming"
$scriptPath = Join-Path $PSScriptRoot "start-streaming.ps1"

# Create a scheduled task that:
# 1. Runs at system startup
# 2. Runs every 5 minutes (to recover if streaming dies)
# 3. Runs as the current user

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""

$triggers = @(
    # At startup
    New-ScheduledTaskTrigger -AtStartup -RandomDelay "00:00:30",
    # Every 5 minutes
    New-ScheduledTaskTrigger -Daily -At "00:00" -RepetitionInterval (New-TimeSpan -Minutes 5) -RepetitionDuration (New-TimeSpan -Days 365)
)

$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable -MultipleInstances IgnoreNew

$principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType S4U -RunLevel Limited

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $triggers -Settings $settings -Principal $principal -Force

Write-Host "Scheduled task '$taskName' created successfully!" -ForegroundColor Green
Write-Host "It will run at system startup and every 5 minutes." -ForegroundColor Cyan
Write-Host ""
Write-Host "To run manually:" -ForegroundColor Yellow
Write-Host "  Start-ScheduledTask -TaskName '$taskName'" -ForegroundColor White
Write-Host ""
Write-Host "To view:" -ForegroundColor Yellow
Write-Host "  Get-ScheduledTask -TaskName '$taskName' | Format-List" -ForegroundColor White
Write-Host ""
Write-Host "To remove:" -ForegroundColor Yellow
Write-Host "  Unregister-ScheduledTask -TaskName '$taskName' -Confirm:`$false" -ForegroundColor White
