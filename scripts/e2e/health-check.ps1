[CmdletBinding()]
param(
    [string]$GatewayUrl = "http://localhost:7049",
    [string]$AiUrl = "http://localhost:7055",
    [string]$ExamUrl = "http://localhost:7056",
    [string]$NacosUrl = "http://localhost:8848",
    [string]$OllamaUrl = "http://localhost:11434",
    [int[]]$RequiredPorts = @(5173, 7049, 7050, 7051, 7053, 7054, 7055, 7056, 5433, 6378, 8848, 9876, 11434)
)

$ErrorActionPreference = "Stop"
$failures = [System.Collections.Generic.List[string]]::new()

function Add-Failure([string]$message) {
    [void]$failures.Add($message)
    Write-Host "FAIL $message" -ForegroundColor Red
}

function Test-TcpPort([int]$port) {
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $async = $client.BeginConnect("127.0.0.1", $port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(1500)) { return $false }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Get-ResponseText($content) {
    if ($content -is [byte[]]) {
        return [Text.Encoding]::UTF8.GetString($content)
    }
    return [string]$content
}

function Test-Http([string]$name, [string]$uri, [int[]]$expectedStatus = @(200), [hashtable]$headers = @{}) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -Headers $headers -TimeoutSec 12
        if ($expectedStatus -notcontains [int]$response.StatusCode) {
            Add-Failure "$name returned HTTP $([int]$response.StatusCode)"
            return
        }
        $body = Get-ResponseText $response.Content
        Write-Host "PASS $name HTTP $([int]$response.StatusCode) $($body.Substring(0, [Math]::Min(160, $body.Length)))" -ForegroundColor Green
        return $body
    } catch {
        $response = $_.Exception.Response
        if ($null -ne $response -and $expectedStatus -contains [int]$response.StatusCode) {
            Write-Host "PASS $name HTTP $([int]$response.StatusCode)" -ForegroundColor Green
            return
        }
        $status = if ($null -ne $response) { [int]$response.StatusCode } else { 0 }
        Add-Failure "$name unavailable (HTTP $status): $($_.Exception.Message)"
    }
}

Write-Host "== TCP dependencies =="
foreach ($port in $RequiredPorts) {
    if (Test-TcpPort $port) {
        Write-Host "PASS TCP 127.0.0.1:$port" -ForegroundColor Green
    } else {
        Add-Failure "TCP 127.0.0.1:$port is not reachable"
    }
}

$token = $env:E2E_ACCESS_TOKEN
$authHeaders = @{}
if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "WARN E2E_ACCESS_TOKEN is not set; protected actuator checks are limited to unauthenticated HTTP 401." -ForegroundColor Yellow
} else {
    $authHeaders = @{ Authorization = "Bearer $token" }
}

Write-Host "== application health =="
if ($authHeaders.Count -gt 0) {
    $aiHealth = Test-Http "AI actuator" "$($AiUrl.TrimEnd('/'))/ai/actuator/health" @(200) $authHeaders
    $examHealth = Test-Http "Exam actuator" "$($ExamUrl.TrimEnd('/'))/exam/actuator/health" @(200) $authHeaders
    if ($aiHealth -notmatch '"status"\s*:\s*"UP"') { Add-Failure "AI actuator did not report UP" }
    if ($examHealth -notmatch '"status"\s*:\s*"UP"') { Add-Failure "Exam actuator did not report UP" }
} else {
    Test-Http "AI actuator authentication" "$($AiUrl.TrimEnd('/'))/ai/actuator/health" @(401)
    Test-Http "Exam actuator authentication" "$($ExamUrl.TrimEnd('/'))/exam/actuator/health" @(401)
}

$expectedGatewayStatus = if ($authHeaders.Count -gt 0) { @(200) } else { @(401) }
Test-Http "Gateway AI health route" "$($GatewayUrl.TrimEnd('/'))/ai/actuator/health" $expectedGatewayStatus $authHeaders | Out-Null
Test-Http "Gateway Exam health route" "$($GatewayUrl.TrimEnd('/'))/exam/actuator/health" $expectedGatewayStatus $authHeaders | Out-Null
Test-Http "Nacos HTTP" "$($NacosUrl.TrimEnd('/'))/nacos/" @(200, 302, 401, 403) | Out-Null
Test-Http "Ollama tags" "$($OllamaUrl.TrimEnd('/'))/api/tags" @(200) | Out-Null

if ($failures.Count -gt 0) {
    Write-Host "Health check failed: $($failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}
Write-Host "Health check passed." -ForegroundColor Green
