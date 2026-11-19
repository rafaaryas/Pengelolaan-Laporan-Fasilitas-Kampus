param(
    [ValidateSet('Run', 'Test', 'Package', 'VerifyDatabase', 'Info')]
    [string]$Task = 'Run'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    # Semua cache build ada di workspace, tidak mengubah konfigurasi global.
    $env:MAVEN_USER_HOME = Join-Path $projectRoot '.cache\maven'
    $temporaryDirectory = Join-Path $projectRoot '.cache\tmp'
    New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null
    $env:TEMP = $temporaryDirectory
    $env:TMP = $temporaryDirectory
    $jdkCandidates = @()
    if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
    if (Test-Path '.tools') {
        $jdkCandidates += Get-ChildItem '.tools' -Directory -Filter 'jdk-21*' | Select-Object -ExpandProperty FullName
    }
    $javaCommand = Get-Command javac -ErrorAction SilentlyContinue
    if ($javaCommand) { $jdkCandidates += Split-Path (Split-Path $javaCommand.Source -Parent) -Parent }
    $selectedJdk = $null
    foreach ($candidate in $jdkCandidates) {
        $compiler = Join-Path $candidate 'bin\javac.exe'
        if (Test-Path $compiler) {
            $version = & $compiler -version 2>&1 | Out-String
            if ($version -match 'javac 21[\s\.]') { $selectedJdk = $candidate; break }
        }
    }
    if (-not $selectedJdk) { throw 'JDK 21 belum ditemukan. Set $env:JAVA_HOME pada sesi ini atau ekstrak Temurin JDK 21 ke .tools.' }
    $env:JAVA_HOME = $selectedJdk
    $env:PATH = "$(Join-Path $selectedJdk 'bin');$env:PATH"

    if ($Task -in 'Run', 'VerifyDatabase') {
        & (Join-Path $PSScriptRoot 'Import-LocalEnv.ps1')
        $env:SPRING_PROFILES_ACTIVE = 'local'
    }
    $repo = '-Dmaven.repo.local=' + (Join-Path $projectRoot '.cache\repository')
    [string[]]$goals = @(switch ($Task) {
        'Run' { @('spring-boot:run') }
        'Test' { @('test') }
        'Package' { @('clean', 'verify') }
        'VerifyDatabase' { @('test', '-Dtest=MySqlConnectionTest', '-DexcludedGroups=none') }
        'Info' { @('--version') }
    })
    & '.\mvnw.cmd' $repo @goals
    if ($LASTEXITCODE -ne 0) { throw "Maven gagal dengan exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
