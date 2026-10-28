# backup.ps1 - save the TV's state before any change, so restore.ps1 and you can go back.
# Keep this file next to adb.exe (C:\platform-tools).
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\backup.ps1
# Writes C:\tv-backup\: packages-all.txt (disable-batch.ps1 refuses to run without it), the packages already
# disabled by the factory, and the system, secure and global settings. These files describe your own TV:
# keep them out of a public repository.

$Adb    = Join-Path $PSScriptRoot 'adb.exe'
$Backup = 'C:\tv-backup'

if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found next to this script ($PSScriptRoot)."; exit 1 }
$state = (& $Adb get-state 2>&1 | Out-String).Trim()
if ($state -ne 'device') {
    Write-Host "TV not connected or not authorised (adb get-state: $state). Run: .\adb connect <TV-IP>:5555"
    exit 1
}
if (Test-Path (Join-Path $Backup 'packages-all.txt')) {
    Write-Host "A backup already exists in $Backup. Move it away first if you really want a new one."
    exit 1
}

New-Item -ItemType Directory -Force $Backup | Out-Null
& $Adb shell pm list packages -f            | Set-Content (Join-Path $Backup 'packages-all.txt') -Encoding ascii
& $Adb shell pm list packages -d            | Set-Content (Join-Path $Backup 'packages-disabled-before.txt') -Encoding ascii
& $Adb shell settings list system           | Set-Content (Join-Path $Backup 'settings-system.txt') -Encoding utf8
& $Adb shell settings list secure           | Set-Content (Join-Path $Backup 'settings-secure.txt') -Encoding utf8
& $Adb shell settings list global           | Set-Content (Join-Path $Backup 'settings-global.txt') -Encoding utf8

Get-ChildItem $Backup | ForEach-Object { Write-Host ("  {0,-30} {1,8} bytes" -f $_.Name, $_.Length) }
Write-Host "Backup saved in $Backup."
