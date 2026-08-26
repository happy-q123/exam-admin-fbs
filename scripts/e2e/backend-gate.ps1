[CmdletBinding()]
param(
    [string]$MavenCommand = $env:MAVEN_CMD
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($MavenCommand)) {
    $maven = Get-Command mvn.cmd, mvn -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $maven) {
        throw "Maven not found. Set MAVEN_CMD to mvn.cmd before running this gate."
    }
    $MavenCommand = $maven.Source
}

$dbPassword = $env:E2E_DB_PASSWORD
$redisPassword = $env:E2E_REDIS_PASSWORD
if ([string]::IsNullOrWhiteSpace($dbPassword)) {
    throw "E2E_DB_PASSWORD is required; the gate never uses a repository-stored database password."
}
if ([string]::IsNullOrWhiteSpace($redisPassword)) {
    throw "E2E_REDIS_PASSWORD is required; the gate never uses a repository-stored Redis password."
}

function Invoke-MavenGate([string[]]$arguments) {
    $safeArguments = $arguments | ForEach-Object {
        $_ -replace '(?i)(spring\.datasource\.password|spring\.data\.redis\.password|password)=\S+', '$1=***'
    }
    Write-Host ("== mvn " + ($safeArguments -join " ") + " ==")
    & $MavenCommand @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven gate failed with exit code $LASTEXITCODE"
    }
}

$runtimeProperties = @(
    "-Dspring.datasource.password=$dbPassword",
    "-Dspring.data.redis.password=$redisPassword",
    "-Dspring.cloud.nacos.discovery.server-addr=127.0.0.1:8848",
    "-Dsurefire.failIfNoSpecifiedTests=false"
)

Invoke-MavenGate (@("-pl", "services/ai", "-am") + $runtimeProperties + @(
    "-Dtest=WrongQuestionAnswerQualityTest,GlobalExceptionHandlerTest", "test"
))

Invoke-MavenGate (@("-pl", "services/exam", "-am") + $runtimeProperties + @(
    "-Dtest=ExamQuestionRelationTest", "-Dexam.websocket-enabled=false", "test"
))

Invoke-MavenGate (@("-pl", "services/message", "-am") + $runtimeProperties + @(
    "-Drocketmq.name-server=127.0.0.1:9876", "-Dtest=MQTest", "test"
))

Write-Host "Backend gate passed." -ForegroundColor Green
