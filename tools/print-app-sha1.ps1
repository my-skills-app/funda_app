# Print SHA-1 fingerprints for Firebase / Google Sign-In setup.
# Usage: .\tools\print-app-sha1.ps1 [-Apk path\to\signed-release.apk]

param(
    [string]$Apk = ""
)

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$keytool = "keytool"

function Format-Sha1($hex) {
    $pairs = for ($i = 0; $i -lt $hex.Length; $i += 2) { $hex.Substring($i, 2) }
    return ($pairs -join ":").ToUpperInvariant()
}

Write-Host "=== Bhagavat Gita signing fingerprints ==="
Write-Host ""
Write-Host "Package: com.chill.with.examsaathi"
Write-Host ""

$debugKs = Join-Path $env:USERPROFILE ".android\debug.keystore"
if (Test-Path $debugKs) {
    $out = & $keytool -list -v -keystore $debugKs -storepass android -alias androiddebugkey 2>&1 | Out-String
    if ($out -match "SHA1:\s*(.+)") {
        Write-Host "Debug keystore SHA-1 (local debug builds):"
        Write-Host "  $($Matches[1].Trim())"
        Write-Host ""
    }
}

if ($Apk -and (Test-Path $Apk)) {
    $out = & $keytool -printcert -jarfile $Apk 2>&1 | Out-String
    if ($out -match "SHA1:\s*(.+)") {
        Write-Host "Release APK SHA-1 ($Apk):"
        Write-Host "  $($Matches[1].Trim())"
        Write-Host ""
    }
} elseif (Test-Path $adb) {
    $path = (& $adb shell pm path com.chill.with.examsaathi 2>$null) -replace "^package:", "" | ForEach-Object { $_.Trim() }
    if ($path) {
        $local = Join-Path $env:TEMP "bg-installed.apk"
        & $adb pull $path $local 2>$null | Out-Null
        if (Test-Path $local) {
            $out = & $keytool -printcert -jarfile $local 2>&1 | Out-String
            if ($out -match "SHA1:\s*(.+)") {
                Write-Host "Installed release APK SHA-1 (from phone):"
                Write-Host "  $($Matches[1].Trim())"
                Write-Host ""
            }
        }
    }
}

Write-Host "Firebase steps:"
Write-Host "  1. Firebase Console -> Project settings -> Your Android app"
Write-Host "  2. Add SHA-1 fingerprint(s) above"
Write-Host "  3. Authentication -> Sign-in method -> Enable Google"
Write-Host "  4. Download new google-services.json -> app/google-services.json"
Write-Host "  5. Rebuild signed release APK and reinstall"
