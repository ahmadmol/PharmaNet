$path = "app/src/main/kotlin/com/pharmalink/feature/help/presentation/HelpScreen.kt"
$lines = Get-Content $path
$targetLines = @(161,162,166,167,171,172,176,177,216,217,241,242,272,273,337,347,502,595,601,617,624)
foreach ($ln in $targetLines) {
    if ($ln -le $lines.Length) {
        $line = $lines[$ln-1]
        Write-Host "$ln`: $line"
        $chars = $line.ToCharArray()
        $hex = ($chars | ForEach-Object { [int]$_ }) -join ' '
        Write-Host "   CP: $hex"
    }
}
