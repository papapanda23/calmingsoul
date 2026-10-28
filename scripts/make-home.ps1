# make-home.ps1 - make the new launcher the default Home and check that it really took over.
# Keep this file next to adb.exe (C:\platform-tools), or pass -AdbDir.
# The stock launcher is NOT disabled by this script. It stays installed as the way back.
#
# Way back (stock Home):
#   .\adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity

param(
    [string]$AdbDir = $PSScriptRoot
)

$Pkg = 'com.gathin.tvhome'
$Adb = Join-Path $AdbDir 'adb.exe'
if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found in $AdbDir (use -AdbDir)."; exit 1 }

$state = (& $Adb get-state 2>&1 | Out-String).Trim()
if ($state -ne 'device') { Write-Host "TV not connected (adb get-state: $state)."; exit 1 }

Write-Host 'Setting the default Home ...'
& $Adb shell cmd package set-home-activity "$Pkg/.MainActivity"

Write-Host 'Pressing Home ...'
& $Adb shell input keyevent KEYCODE_HOME
Start-Sleep -Seconds 3

# "Success" from set-home-activity proves nothing on some builds: check what is really on screen.
$focus = (& $Adb shell "dumpsys activity activities | grep mFocusedApp" | Out-String).Trim()
Write-Host $focus
Write-Host ''
if ($focus -match [regex]::Escape($Pkg)) {
    Write-Host 'OK: the new launcher is the Home screen.'
} else {
    Write-Host 'NOT taken over: Home still opens something else. Send the line above to Claude.'
    Write-Host 'To undo anything: .\adb shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity'
}
