# Load environment variables from .env into the current process and then run the webhook server.
$envFile = ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -and $_ -notmatch '^[ \t]*#') {
            $pair = $_ -split '=', 2
            if ($pair.Length -eq 2) {
                $name = $pair[0].Trim()
                $value = $pair[1].Trim()
                if (-not [string]::IsNullOrWhiteSpace($name)) {
                    $env:$name = $value
                }
            }
        }
    }
}

$jdkBin = Join-Path $PSScriptRoot "oracleJdk-26\bin"
$javac = Join-Path $jdkBin "javac.exe"
$java = Join-Path $jdkBin "java.exe"

if (-not (Test-Path $javac)) {
    Write-Error "Cannot find javac at $javac"
    exit 1
}
if (-not (Test-Path $java)) {
    Write-Error "Cannot find java at $java"
    exit 1
}

$buildDir = Join-Path $PSScriptRoot "out"
if (-not (Test-Path $buildDir)) {
    New-Item -ItemType Directory -Path $buildDir | Out-Null
}

Write-Host "Compiling Java sources..."
& $javac -d $buildDir "$PSScriptRoot\src\main\java\com\example\prflow\*.java"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Java compilation failed. Fix errors and retry."
    exit $LASTEXITCODE
}

Write-Host "Starting GitHub PR webhook server..."
& $java -cp $buildDir com.example.prflow.GithubPrWebhookServer
