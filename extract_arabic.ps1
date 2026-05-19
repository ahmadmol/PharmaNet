$path = "feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileScreen.kt"
$lines = Get-Content $path
for ($i=0; $i -lt $lines.Length; $i++) {
    $lineNum = $i + 1
    $line = $lines[$i]
    if ($line -match '[\u0600-\u06FF]') {
        Write-Host "$lineNum`: $line"
    }
}
