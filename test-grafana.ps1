$headers = @{"Authorization"="Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin"))}
$body = Get-Content -Raw "C:\Users\ASUS\Desktop\BIG DATA PROJET SPARK SOC\test-payload.json"
$r = Invoke-WebRequest -Uri "http://localhost:3001/api/ds/query" -Method POST -ContentType "application/json" -Headers $headers -Body $body -UseBasicParsing
$r.Content
