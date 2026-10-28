# disable-batch.ps1 - disable a small batch of packages on the TV, one pm call per package.
# Keep this file next to adb.exe (C:\platform-tools).
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\disable-batch.ps1 com.example.one com.example.two
# Log of what we disabled: C:\tv-backup\disabled-by-us.txt  (restore.ps1 reads it)

param(
    [switch]$AllowLauncher,
    [Parameter(Position = 0, ValueFromRemainingArguments = $true)]
    [string[]]$Packages
)

$Adb    = Join-Path $PSScriptRoot 'adb.exe'
$Backup = 'C:\tv-backup'
$Log    = Join-Path $Backup 'disabled-by-us.txt'

if (-not $Packages -or $Packages.Count -eq 0) { Write-Host 'No packages given.'; exit 1 }
if (-not (Test-Path $Adb)) { Write-Host "adb.exe not found next to this script ($PSScriptRoot)."; exit 1 }
if (-not (Test-Path (Join-Path $Backup 'packages-all.txt'))) {
    Write-Host 'No backup found (C:\tv-backup\packages-all.txt). Make the backup first.'
    exit 1
}

$state = (& $Adb get-state 2>&1 | Out-String).Trim()
if ($state -ne 'device') {
    Write-Host "TV not connected or not authorised (adb get-state: $state). Run: .\adb connect <TV-IP>:5555"
    exit 1
}

# Packages the TV needs to boot and work. A list containing any of these is refused whole.
$Protected = @(
    '^android$',
    '^com\.android\.(systemui|shell|phone|settings|tv\.settings)($|\.)',
    '^com\.android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata|vending|networkstack|bluetooth|inputdevices|keychain|certinstaller|companiondevicemanager)',
    '^com\.android\.providers\.(?!calendar|contacts|userdictionary|blockednumber)',
    '^com\.google\.android\.(permissioncontroller|packageinstaller|ext\.services|ext\.shared|modulemetadata|overlay\.modules|gms|gsf|webview|inputmethod|tv\.remote\.service)',
    '^com\.google\.android\.apps\.mediashell',
    '^com\.mediatek\.',
    '^com\.airoha\.',
    '^com\.example\.bt_pair_tips',
    '^com\.at\.factory\.',
    '^com\.cvte\.tv\.(fac\.|hkcfac|at$|steward|infoprovider|hotel)'
)

$bad = @()
foreach ($p in $Packages) {
    foreach ($rx in $Protected) {
        if ($p -match $rx) { $bad += $p; break }
    }
}
if ($bad.Count -gt 0) {
    Write-Host 'REFUSED - these packages are protected, nothing was changed:'
    $bad | ForEach-Object { Write-Host "  $_" }
    exit 1
}

if (($Packages -contains 'com.google.android.tvlauncher') -and -not $AllowLauncher) {
    Write-Host 'REFUSED - the stock launcher may only be disabled once the new launcher is Home and confirmed.'
    Write-Host 'If that is the case, add -AllowLauncher.'
    exit 1
}

# Every package must exist on the TV.
$installed = (& $Adb shell pm list packages) | ForEach-Object { ($_.Trim()) -replace '^package:', '' }
$missing = @($Packages | Where-Object { $installed -notcontains $_ })
if ($missing.Count -gt 0) {
    Write-Host 'REFUSED - not installed on this TV (typo?), nothing was changed:'
    $missing | ForEach-Object { Write-Host "  $_" }
    exit 1
}

# One pm command per adb call.
foreach ($p in $Packages) {
    Write-Host "Disabling $p ..."
    $out = (& $Adb shell pm disable-user --user 0 $p 2>&1 | Out-String).Trim()
    Write-Host "  $out"
}

# Verify with the disabled list, not with the exit code.
$disabledNow = (& $Adb shell pm list packages -d) | ForEach-Object { ($_.Trim()) -replace '^package:', '' }
$done = @(); $failed = @()
foreach ($p in $Packages) {
    if ($disabledNow -contains $p) { $done += $p } else { $failed += $p }
}

if ($done.Count -gt 0) {
    $existing = @()
    if (Test-Path $Log) { $existing = Get-Content $Log }
    foreach ($p in $done) {
        if ($existing -notcontains $p) { Add-Content -Path $Log -Value $p -Encoding ascii }
    }
}

Write-Host ''
Write-Host "Disabled and verified: $($done.Count)"
$done | ForEach-Object { Write-Host "  $_" }
if ($failed.Count -gt 0) {
    Write-Host "NOT disabled: $($failed.Count)"
    $failed | ForEach-Object { Write-Host "  $_" }
    if ($done.Count -eq 0) {
        Write-Host 'Every package was refused. Stop here and tell Claude; this firmware may not allow pm disable-user.'
    }
}
Write-Host ''
Write-Host 'Now take a screenshot and check the TV still behaves before the next batch.'
