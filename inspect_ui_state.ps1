$path = "feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileUiState.kt"
$lines = Get-Content $path
# Show lines with Arabic (by checking for non-ASCII)
for ($i=0; $i -lt $lines.Length; $i++) {
    $lineNum = $i+1
    $line = $lines[$i]
    if ($line -match '[\u0600-\u06FF]') {
        Write-Host "$lineNum`: $line"
        $chars = $line.ToCharArray()
        $hex = ($chars | ForEach-Object { [int]$_ }) -join ' '
        Write-Host "   CP: $hex"
    }
}
