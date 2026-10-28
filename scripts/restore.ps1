# restore.ps1 - re-enable packages that disable-batch.ps1 disabled.
# Keep this file next to adb.exe (C:\platform-tools).
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\restore.ps1                  (restore everything in the log)
#   powershell -ExecutionPolicy Bypass -File .\restore.ps1 com.example.one  (restore only this package)
#
# If Home is broken after replacing the launcher, restore the stock launcher first:
#   powershell -ExecutionPolicy Bypass -File .\restore.ps1 com.google.android.tvlauncher

param(
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Packages
)

$Adb    = Join-Path $PSScriptRoot 'adb.exe'
$Backup = 'C:\tv-backup'
$Log    = Join-Path $Backup 'disabled-by-us.txt'

if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found next to this script ($PSScriptRoot)."; exit 1 }

$state = (& $Adb get-state 2>&1 | Out-String).Trim()
if ($state -ne 'device') {
    Write-Host "TV not connected or not authorised (adb get-state: $state). Run: .\adb connect <TV-IP>:5555"
    exit 1
}

if (-not $Packages -or $Packages.Count -eq 0) {
    if (-not (Test-Path $Log)) { Write-Host "Nothing to restore: $Log does not exist."; exit 0 }
    $Packages = @(Get-Content $Log | Where-Object { $_.Trim() -ne '' })
}
if ($Packages.Count -eq 0) { Write-Host 'Nothing to restore.'; exit 0 }

# One pm command per adb call.
foreach ($p in $Packages) {
    Write-Host "Enabling $p ..."
    $out = (& $Adb shell pm enable $p 2>&1 | Out-String).Trim()
    Write-Host "  $out"
}

# Verify with the disabled list.
$disabledNow = (& $Adb shell pm list packages -d) | ForEach-Object { ($_.Trim()) -replace '^package:', '' }
$restored = @(); $still = @()
foreach ($p in $Packages) {
    if ($disabledNow -contains $p) { $still += $p } else { $restored += $p }
}

# Drop restored packages from the log.
if ((Test-Path $Log) -and $restored.Count -gt 0) {
    $left = @(Get-Content $Log | Where-Object { $_.Trim() -ne '' -and ($restored -notcontains $_.Trim()) })
    Set-Content -Path $Log -Value $left -Encoding ascii
}

Write-Host ''
Write-Host "Restored: $($restored.Count)"
$restored | ForEach-Object { Write-Host "  $_" }
if ($still.Count -gt 0) {
    Write-Host "Still disabled: $($still.Count)"
    $still | ForEach-Object { Write-Host "  $_" }
}
Write-Host ''
Write-Host 'Reboot the TV if an app still misbehaves:  .\adb reboot'
