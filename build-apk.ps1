# Build the FolioFrame Android release APK for one channel.
#
#   .\build-apk.ps1 -Channel private   # personal build: Bangumi goes through my own proxy
#   .\build-apk.ps1 -Channel public    # public build: no private infrastructure baked in
#
# Both channels share one source tree. The only difference is the value injected into
# frontend/assets/build-config.js, which is restored after the build so the repository
# never carries a personal proxy URL.

param(
    [ValidateSet('private', 'public')]
    [string]$Channel = 'private',
    [string]$Version = '0.5.0'
)

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$configPath = Join-Path $root 'frontend\assets\build-config.js'
$androidDir = Join-Path $root 'android'
$bgmProxy = 'https://folioframe-bgm.pages.dev'

if (-not (Test-Path $configPath)) {
    Write-Error 'Missing frontend/assets/build-config.js'
    exit 1
}

$originalConfig = Get-Content $configPath -Raw

function Write-BuildConfig {
    param([string]$Proxy)
    $escaped = $Proxy.Replace("'", "\'")
    $body = @"
/* Build-time configuration.
 *
 * bgmProxy stays empty in the committed source on purpose. A personal build injects its
 * own Cloudflare proxy URL into the packaged copy; the public build falls back to the
 * official api.bgm.tv endpoint and uses none of the maintainer's infrastructure.
 */
window.FOLIOFRAME_CONFIG = { bgmProxy: '$escaped' };
"@
    Set-Content -Path $configPath -Value $body -NoNewline -Encoding UTF8
}

function Repair-CapacitorGradle {
    $cap = Join-Path $androidDir 'app\capacitor.build.gradle'
    if (-not (Test-Path $cap)) { return }
    $text = Get-Content -Raw $cap
    if ($text -match 'VERSION_21') { $text = $text -replace 'JavaVersion\.VERSION_21', 'JavaVersion.VERSION_17' }
    if ($text -match 'JavaVersion\\') { $text = $text -replace 'JavaVersion\\\.', 'JavaVersion.' }
    Set-Content -Path $cap -Value $text -NoNewline
}

try {
    $value = if ($Channel -eq 'private') { $bgmProxy } else { '' }
    Write-BuildConfig -Proxy $value
    Write-Host "Channel: $Channel"

    & (Join-Path $root 'node_modules\.bin\cap.cmd') sync android | Select-Object -Last 2
    if ($LASTEXITCODE -ne 0) { throw 'cap sync failed' }
    Repair-CapacitorGradle

    Push-Location $androidDir
    try {
        & .\gradlew.bat assembleRelease 2>&1 | Select-Object -Last 6
        if ($LASTEXITCODE -ne 0) { throw 'gradle build failed' }
    } finally {
        Pop-Location
    }

    $apk = Join-Path $androidDir 'app\build\outputs\apk\release\app-release.apk'
    if (-not (Test-Path $apk)) { throw 'APK not produced' }

    # Build the Chinese app name from code points. Windows PowerShell 5.1 reads this
    # script as GBK when no BOM is present, which would corrupt a literal "卷影".
    $appName = [string][char]0x5377 + [string][char]0x5F71
    $target = Join-Path $root ("$appName-FolioFrame-$Version-$Channel.apk")
    Copy-Item $apk $target -Force
    $info = Get-Item $target
    Write-Host "APK: $($info.FullName)"
    Write-Host "Size: $($info.Length) bytes"
    Write-Host "SHA256: $((Get-FileHash $target -Algorithm SHA256).Hash)"
} finally {
    Set-Content -Path $configPath -Value $originalConfig -NoNewline -Encoding UTF8
}
