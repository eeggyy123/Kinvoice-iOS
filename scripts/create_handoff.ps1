[CmdletBinding()]
param(
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $projectRoot
$deliveryRoot = if ($OutputDirectory) {
    [System.IO.Path]::GetFullPath($OutputDirectory)
} else {
    Join-Path $workspaceRoot "delivery"
}
$packageName = "KinVoice-Apple-Handoff-2026-07-25"
$stagingRoot = Join-Path $deliveryRoot $packageName
$zipPath = Join-Path $deliveryRoot "$packageName.zip"

function Copy-ExactFile {
    param([string]$Source, [string]$Destination)
    $destinationDirectory = Split-Path -Parent $Destination
    New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
    Copy-Item -LiteralPath $Source -Destination $Destination -Force
}

New-Item -ItemType Directory -Force -Path $deliveryRoot | Out-Null
if (Test-Path -LiteralPath $stagingRoot) { Remove-Item -LiteralPath $stagingRoot -Recurse -Force }
if (Test-Path -LiteralPath $zipPath) { Remove-Item -LiteralPath $zipPath -Force }
New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null

# Native Apple client. There are no generated build folders in this source tree.
Copy-Item -LiteralPath (Join-Path $projectRoot "ios") -Destination (Join-Path $stagingRoot "ios") -Recurse

# Stateless production backend: copy only modules reachable from app.main.
$backendFiles = @(
    ".env.example", "Dockerfile", "docker-compose.yml", "README.md", "requirements.txt",
    "app/__init__.py", "app/main.py", "app/config.py", "app/security.py",
    "app/api/__init__.py", "app/api/memory_ai.py",
    "app/schemas/__init__.py", "app/schemas/memory_ai.py",
    "app/services/__init__.py", "app/services/llm_service.py", "app/services/memory_ai_service.py",
    "app/utils/__init__.py", "app/utils/logger.py",
    "tests/test_api.py", "tests/test_llm_service.py", "tests/test_memory_ai_service.py"
)
foreach ($relativePath in $backendFiles) {
    Copy-ExactFile `
        -Source (Join-Path (Join-Path $projectRoot "backend") $relativePath) `
        -Destination (Join-Path (Join-Path $stagingRoot "backend") $relativePath)
}

Copy-Item -LiteralPath (Join-Path $projectRoot "preview") -Destination (Join-Path $stagingRoot "preview") -Recurse
Copy-ExactFile -Source (Join-Path $projectRoot "scripts/validate_delivery.py") -Destination (Join-Path $stagingRoot "scripts/validate_delivery.py")
Copy-ExactFile -Source (Join-Path $projectRoot "scripts/create_handoff.ps1") -Destination (Join-Path $stagingRoot "scripts/create_handoff.ps1")
Copy-Item -LiteralPath (Join-Path $workspaceRoot "docs") -Destination (Join-Path $stagingRoot "docs") -Recurse
$handoffManual = Get-ChildItem -LiteralPath (Join-Path $workspaceRoot "docs") -Filter "04-*.md" | Select-Object -First 1
$acceptanceStatus = Get-ChildItem -LiteralPath (Join-Path $workspaceRoot "docs") -Filter "05-*.md" | Select-Object -First 1
Copy-ExactFile -Source $handoffManual.FullName -Destination (Join-Path $stagingRoot "README-HANDOFF.md")
Copy-ExactFile -Source $acceptanceStatus.FullName -Destination (Join-Path $stagingRoot "ACCEPTANCE-STATUS.md")

$materialsRoot = Join-Path $stagingRoot "competition-materials"
New-Item -ItemType Directory -Force -Path $materialsRoot | Out-Null
Get-ChildItem -LiteralPath $workspaceRoot -Directory | Where-Object {
    !$_.Name.StartsWith(".") -and
    $_.FullName -ne $projectRoot -and
    $_.FullName -ne $deliveryRoot
} | ForEach-Object {
    Get-ChildItem -LiteralPath $_.FullName -File | Where-Object { $_.Extension -in @(".pdf", ".docx") } | ForEach-Object {
        Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $materialsRoot $_.Name) -Force
    }
}

$forbiddenNames = @(".env", "private.pem")
$forbidden = Get-ChildItem -LiteralPath $stagingRoot -Recurse -Force | Where-Object {
    $_.Name -in $forbiddenNames -or $_.Name -in @(".venv", "venv", "__pycache__", "logs", "data")
}
if ($forbidden) {
    throw "Forbidden files entered the handoff package: $($forbidden.FullName -join ', ')"
}

$secretPattern = "sk-[A-Za-z0-9_-]{16,}|BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY"
$secretHits = Get-ChildItem -LiteralPath $stagingRoot -File -Recurse | Where-Object { $_.Length -lt 5MB } | Select-String -Pattern $secretPattern -ErrorAction SilentlyContinue
if ($secretHits) {
    throw "Potential secret found in handoff package: $($secretHits.Path -join ', ')"
}

& python (Join-Path $stagingRoot "scripts/validate_delivery.py")
if ($LASTEXITCODE -ne 0) { throw "Static delivery validation failed in staging." }

$manifestPath = Join-Path $stagingRoot "SHA256SUMS.txt"
Get-ChildItem -LiteralPath $stagingRoot -File -Recurse | Where-Object { $_.FullName -ne $manifestPath } |
    Sort-Object FullName |
    ForEach-Object {
        $relative = $_.FullName.Substring($stagingRoot.Length + 1).Replace("\", "/")
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $relative"
    } | Set-Content -LiteralPath $manifestPath -Encoding UTF8

Compress-Archive -LiteralPath $stagingRoot -DestinationPath $zipPath -CompressionLevel Optimal
$zipHash = (Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Output "HANDOFF_DIRECTORY=$stagingRoot"
Write-Output "HANDOFF_ZIP=$zipPath"
Write-Output "HANDOFF_ZIP_SHA256=$zipHash"
