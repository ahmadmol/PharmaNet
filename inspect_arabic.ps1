$path = "feature/profile/src/main/kotlin/com/pharmalink/feature/profile/ProfileScreen.kt"
$lines = Get-Content $path
# Show specific lines with their Unicode codepoints
$targetLines = @(200,316,317,318,319,350,385,391,509,510,511,512,564,671,674,677,680,709,711,714,715,718,721,723)
foreach ($ln in $targetLines) {
    $line = $lines[$ln-1]
    $chars = $line.ToCharArray()
    $hex = ($chars | ForEach-Object { [int]$_ }) -join ' '
    Write-Host "$ln`: $line"
    Write-Host "   Codepoints: $hex"
    Write-Host ""
}
