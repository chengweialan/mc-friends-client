Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script:Root = Split-Path $PSScriptRoot -Parent
$script:Config = Get-Content (Join-Path $Root 'client.json') -Raw | ConvertFrom-Json
$script:Java = Join-Path $Root $Config.javaRelativePath
function Get-Channel {
    $channel = Invoke-RestMethod -Uri $Config.channelUrl -TimeoutSec 30 -Headers @{'Cache-Control'='no-cache'}
    if ($channel.schema -ne 1 -or $channel.java -ne $Config.javaMajor) { throw 'This release needs a newer client bundle. Ask the server owner.' }
    foreach ($v in @($channel.minecraft, $channel.neoforge)) {
        if ($v -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$') { throw 'Invalid game/loader version.' }
    }
    if ($channel.packUrl -notmatch '^https://raw\.githubusercontent\.com/chengweialan/mc-friends-client/[0-9a-f]{40}/pack/pack\.toml$') { throw 'Release is not published to an immutable commit yet.' }
    return $channel
}
function Assert-Tools {
    foreach ($item in @(@('packwiz-installer.jar',$Config.installerSha256),@('packwiz-installer-bootstrap.jar',$Config.bootstrapSha256))) {
        $actual = (Get-FileHash (Join-Path $Root ('tools/'+$item[0])) -Algorithm SHA256).Hash
        if ($actual -ne $item[1]) { throw ('Updater checksum mismatch: '+$item[0]) }
    }
    if (!(Test-Path -LiteralPath $Java)) { throw 'Bundled Java is missing. Extract the complete ZIP first.' }
}
function Invoke-PackSync($Channel, [string]$GameDir) {
    Assert-Tools
    Push-Location -LiteralPath $GameDir
    try {
        & $Java -jar (Join-Path $Root 'tools/packwiz-installer-bootstrap.jar') --bootstrap-no-update --bootstrap-main-jar (Join-Path $Root 'tools/packwiz-installer.jar') -g -s client $Channel.packUrl
        if ($LASTEXITCODE -ne 0) { throw "Mod synchronization failed (exit $LASTEXITCODE). Game will not start." }
    } finally { Pop-Location }
}
