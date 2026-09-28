& (Join-Path $PSScriptRoot 'Start.ps1') -PrepareOnly
if (!$?) { exit 1 }
exit 0
