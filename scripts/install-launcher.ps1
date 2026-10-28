# install-launcher.ps1 - install the launcher APK on the TV (does NOT make it the default Home yet).
# Keep this file next to adb.exe (C:\platform-tools), or pass -AdbDir.
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\install-launcher.ps1 -Apk C:\path\to\app-debug.apk
#
# Other apps (for example the weather add-on): add  -Pkg io.github.sumyabazar1.weather
#
# On Android 14, "adb install" can hang waiting for an on-screen confirmation, so this pushes the APK
# and installs it with "pm install" from the shell instead.

param(
    [Parameter(Mandatory = $true)][string]$Apk,
    [string]$AdbDir = '',
    [string]$Pkg = 'com.gathin.tvhome'
)

if (-not $AdbDir) {
    $AdbDir = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
}
$Adb = Join-Path $AdbDir 'adb.exe'
if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found in $AdbDir (use -AdbDir)."; exit 1 }
if (-not (Test-Path $Apk)) { Write-Host "APK not found: $Apk"; exit 1 }

$state = (& $Adb get-state 2>&1 | Out-String).Trim()
if ($state -ne 'device') {
    Write-Host "TV not connected or not authorised (adb get-state: $state). Run: .\adb connect <TV-IP>:5555"
    exit 1
}

$remote = '/data/local/tmp/tvhome.apk'
Write-Host 'Pushing the APK ...'
& $Adb push $Apk $remote
if ($LASTEXITCODE -ne 0) { Write-Host 'Push failed.'; exit 1 }

Write-Host 'Installing ...'
$out = (& $Adb shell pm install -r $remote 2>&1 | Out-String).Trim()
Write-Host $out
& $Adb shell rm $remote | Out-Null

$found = (& $Adb shell pm list packages $Pkg | Out-String).Trim()
if ($found -notmatch [regex]::Escape($Pkg)) {
    Write-Host ''
    Write-Host 'NOT installed. Copy the message above and send it to Claude. Do not retry in a loop.'
    exit 1
}

Write-Host ''
Write-Host "Installed: $Pkg"
Write-Host 'The TV still uses its stock Home. Next: .\make-home.ps1'
