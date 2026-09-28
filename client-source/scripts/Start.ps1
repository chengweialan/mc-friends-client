try {
    . (Join-Path $PSScriptRoot 'Common.ps1')
    Assert-Tools
    $launcher = Join-Path $Root 'launcher/prismlauncher.exe'
    $active = Get-Process -Name prismlauncher -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $launcher }
    if ($active) { throw 'Close this bundle''s Prism window and game before reopening Start.cmd.' }
    $lock = [IO.File]::Open((Join-Path $Root '.start.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    try {
        $channel = Get-Channel
        # A loader change also gets a separate instance. Never copy old personal mods automatically.
        $id = 'FriendsMC-' + $channel.minecraft + '-' + $channel.neoforge
        $instance = Join-Path $Root ('launcher/instances/'+$id)
        $gameDir = Join-Path $instance '.minecraft'
        if (!(Test-Path -LiteralPath $instance)) {
            New-Item -ItemType Directory -Path $gameDir -Force | Out-Null
            Copy-Item (Join-Path $Root 'templates/servers.dat') (Join-Path $gameDir 'servers.dat')
            $javaPortable = '../' + $Config.javaRelativePath
            @"
[General]
InstanceType=OneSix
name=Friends MC $($channel.minecraft)
iconKey=default
OverrideMemory=true
MinMemAlloc=512
MaxMemAlloc=4096
OverrideJavaLocation=true
JavaPath=$javaPortable
AutomaticJava=false
OverrideCommands=true
PreLaunchCommand=powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"`$INST_DIR/../../../scripts/Sync.ps1`"
"@ | Set-Content (Join-Path $instance 'instance.cfg') -Encoding UTF8
            @{formatVersion=1;components=@(
                @{uid='net.minecraft';version=$channel.minecraft;important=$true},
                @{uid='net.neoforged';version=$channel.neoforge;important=$true}
            )} | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $instance 'mmc-pack.json') -Encoding UTF8
        }
        if (!(Test-Path (Join-Path $instance 'instance.cfg')) -or !(Test-Path (Join-Path $instance 'mmc-pack.json'))) {
            throw 'Instance creation was interrupted. Ask the owner to repair this instance before retrying.'
        }
        # Preflight before opening the launcher; the prelaunch hook checks again on each play.
        Invoke-PackSync $channel $gameDir
        Write-Host 'Ready. On first use, sign in with your own Microsoft Minecraft account.'
        Start-Process -FilePath $launcher -WorkingDirectory (Split-Path $launcher) -ArgumentList @('--launch', $id)
    } finally { $lock.Dispose() }
} catch { Write-Host $_.Exception.Message -ForegroundColor Red; exit 1 }
