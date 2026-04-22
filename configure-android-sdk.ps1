$ErrorActionPreference = "Stop"

function Escape-SdkPath([string]$path) {
    return $path.Replace("\", "\\")
}

function Resolve-AndroidSdkPath {
    $candidates = @(
        $env:ANDROID_SDK_ROOT,
        $env:ANDROID_HOME,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    ) | Where-Object { $_ -and $_.Trim() -ne "" }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return (Resolve-Path $candidate).Path
        }
    }

    throw "Android SDK nao encontrado. Defina ANDROID_SDK_ROOT/ANDROID_HOME ou instale em $env:LOCALAPPDATA\Android\Sdk."
}

function Write-LocalProperties([string]$projectDir, [string]$sdkPath) {
    $target = Join-Path $projectDir "local.properties"
    $content = "sdk.dir=$(Escape-SdkPath $sdkPath)"
    Set-Content -Path $target -Value $content -Encoding ASCII
    Write-Host "local.properties atualizado:" $target
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdkPath = Resolve-AndroidSdkPath

Write-Host "Android SDK encontrado em:" $sdkPath

$androidProjects = @(
    (Join-Path $root "android"),
    (Join-Path $root "android-v4")
) | Where-Object { Test-Path $_ }

if (-not $androidProjects) {
    throw "Nenhuma pasta Android encontrada para configurar."
}

foreach ($project in $androidProjects) {
    Write-LocalProperties -projectDir $project -sdkPath $sdkPath
}

Write-Host ""
Write-Host "Configuracao concluida para:" ($androidProjects -join ", ")
Write-Host "Se quiser validar, rode:"
Write-Host "  `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'"
Write-Host "  .\android\gradlew.bat assembleDebug"
Write-Host "  .\android\gradlew.bat -p android-v4 :app:assembleDebug"
