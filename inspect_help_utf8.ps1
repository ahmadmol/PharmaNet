$path = "app/src/main/kotlin/com/pharmalink/feature/help/presentation/HelpScreen.kt"
[System.Text.Encoding]::UTF8
$text = [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8)
$lines = $text -split "`n"
$targetLines = @(161,162,166,167,171,172,176,177,216,217,241,242,272,273,337,347,502,595,601,617,624)
foreach ($ln in $targetLines) {
    if ($ln -le $lines.Count) {
        $line = $lines[$ln-1]
        Write-Host "$ln`: $line"
    }
}
