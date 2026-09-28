try {
    . (Join-Path $PSScriptRoot 'Common.ps1')
    if (!$env:INST_DIR -or !$env:INST_MC_DIR) { throw 'Run this updater through the launcher.' }
    $lock = [IO.File]::Open((Join-Path $env:INST_DIR '.sync.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        $channel = Get-Channel
        $installed = Get-Content (Join-Path $env:INST_DIR 'mmc-pack.json') -Raw | ConvertFrom-Json
        $mc = ($installed.components | Where-Object uid -eq 'net.minecraft').version
        $neo = ($installed.components | Where-Object uid -eq 'net.neoforged').version
        if ($mc -ne $channel.minecraft -or $neo -ne $channel.neoforge) { throw 'Game/loader changed. Close Prism and reopen Start.cmd to prepare the correct instance.' }
        Invoke-PackSync $channel $env:INST_MC_DIR
        $channel | ConvertTo-Json | Set-Content (Join-Path $env:INST_DIR 'last-synced-release.json') -Encoding UTF8
    } finally { $lock.Dispose() }
    exit 0
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
