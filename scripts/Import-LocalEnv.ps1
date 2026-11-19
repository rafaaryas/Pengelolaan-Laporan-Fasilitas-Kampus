$ErrorActionPreference = 'Stop'
$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) '.env'
if (-not (Test-Path -LiteralPath $envFile)) { throw 'File .env belum ada. Salin .env.example menjadi .env dan isi konfigurasi.' }
$allowedKeys = @('DB_HOST','DB_PORT','DB_NAME','DB_USERNAME','DB_PASSWORD','SERVER_PORT',
    'DEMO_MAHASISWA_PASSWORD','DEMO_ADMIN_PASSWORD','DEMO_PETUGAS_PASSWORD','MYSQL_ROOT_PASSWORD')
foreach ($line in Get-Content -LiteralPath $envFile -Encoding UTF8) {
    if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
    if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$') { throw 'Format .env tidak valid; gunakan KEY=value tanpa export atau tanda kutip.' }
    $key = $Matches[1]
    $value = $Matches[2]
    if ($key -notin $allowedKeys) { throw "Variabel .env tidak didukung: $key" }
    if ([string]::IsNullOrWhiteSpace($value) -or $value.StartsWith('CHANGE_ME')) {
        if ($key -eq 'MYSQL_ROOT_PASSWORD') { continue } # Tidak diperlukan oleh Laragon.
        throw "Isi nilai lokal untuk $key di .env"
    }
    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
}
$requiredKeys = $allowedKeys | Where-Object { $_ -ne 'MYSQL_ROOT_PASSWORD' }
foreach ($key in $requiredKeys) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, 'Process'))) { throw "Variabel $key wajib diisi." }
}
# Tidak memakai Invoke-Expression. Nilai .env adalah string literal dan tidak dieksekusi.
