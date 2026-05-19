$path = "app/src/main/kotlin/com/pharmalink/feature/help/presentation/HelpScreen.kt"
$enc = [System.Text.Encoding]::Unicode # UTF-16 LE
$text = [System.IO.File]::ReadAllText($path, $enc)
$lines = $text -split "`n"
$targetLines = @(161,162,166,167,171,172,176,177,216,217,241,242,272,273,337,347,502,595,601,617,624)
foreach ($ln in $targetLines) {
    if ($ln -le $lines.Count) {
        $line = $lines[$ln-1]
        Write-Host "$ln`: $line"
    }
}
