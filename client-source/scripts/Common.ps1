Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$script:Root = Split-Path $PSScriptRoot -Parent
$script:Config = Get-Content (Join-Path $Root 'client.json') -Raw | ConvertFrom-Json
$script:Java = Join-Path $Root $Config.javaRelativePath
function Get-Channel {
    New-Item -ItemType Directory -Force -Path (Join-Path $Root 'state') | Out-Null
    $channelFile = Join-Path $Root ('state/channel-'+[Guid]::NewGuid().ToString('N')+'.json')
    try {
        & $Java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-jar' (Join-Path $Root 'tools/friends-updater.jar') 'channel' $Root $channelFile | ForEach-Object {
            Write-Host $_
            Write-ClientProgress 2 '核对服务器版本' ([string]$_)
        }
        if ($LASTEXITCODE -ne 0) { throw '无法获取版本清单；请查看日志中的下载地址，稍后重试。' }
        $channel = Get-Content -LiteralPath $channelFile -Raw -Encoding UTF8 | ConvertFrom-Json
    } finally {
        if (Test-Path -LiteralPath $channelFile) { Remove-Item -LiteralPath $channelFile }
    }
    if ($channel.schema -ne 2 -or $channel.java -ne $Config.javaMajor) { throw 'This release needs a newer client bundle. Ask the server owner.' }
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
    Write-ClientProgress 3 '同步模组与配置' '正在读取清单并检查本地文件'
    $startInfo = New-Object Diagnostics.ProcessStartInfo
    $startInfo.FileName = $Java
    $startInfo.WorkingDirectory = $GameDir
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $updater = Join-Path $Root 'tools/friends-updater.jar'
    if (!(Test-Path -LiteralPath $updater)) { throw 'Download and extract the 0.4.0 client update first.' }
    $startInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
    $startInfo.StandardErrorEncoding = [Text.Encoding]::UTF8
    $startInfo.Arguments = '-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "'+$updater+'" sync "'+$Root+'" "'+$GameDir+'" "'+$Channel.packUrl+'" "'+$Channel.catalogSha256+'"'
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $startInfo
    $process.Start() | Out-Null
    $errorTask = $process.StandardError.ReadToEndAsync()
    try {
        while (($line = $process.StandardOutput.ReadLine()) -ne $null) {
            Write-Host $line
            if ($line -match '^\((\d+)/(\d+)\)\s+(.*)') {
                Write-ClientProgress 3 '同步模组与配置' $Matches[3] -Current ([int]$Matches[1]) -Total ([int]$Matches[2])
            } elseif ($line -match 'Loading pack|Loading manifest') {
                Write-ClientProgress 3 '读取整合包清单' '正在连接更新地址；网络较慢时请稍候'
            } elseif ($line -match 'Checking local|Comparing|Validating') {
                Write-ClientProgress 3 '检查本地文件' '正在比对已安装的模组与配置'
            } else {
                Write-ClientProgress 3 '同步模组与配置' $line
            }
        }
        $process.WaitForExit()
        $errors = $errorTask.Result
        if ($errors) { Write-Host $errors }
        if ($process.ExitCode -ne 0) { throw "Mod synchronization failed (exit $($process.ExitCode)). Game will not start. $errors" }
    } finally { $process.Dispose() }
}
function Set-PclIni([string]$Path, [string]$Key, [string]$Value) {
    New-Item -ItemType Directory -Force -Path (Split-Path $Path) | Out-Null
    $lines = @()
    if (Test-Path -LiteralPath $Path) { $lines = @(Get-Content -LiteralPath $Path -Encoding UTF8 | Where-Object { !$_.StartsWith($Key+':') }) }
    $lines += $Key+':'+$Value
    [IO.File]::WriteAllLines($Path, [string[]]$lines, (New-Object Text.UTF8Encoding($false)))
}
function New-PclImport($Channel, [string]$Id, [string]$Stage) {
    Write-ClientProgress 3 '准备首次安装文件' '正在打包 Java 与配置，完成后由 PCL 安装游戏'
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $manifest = @{formatVersion=1;game='minecraft';versionId=$Channel.release;name=$Id;summary='Friends MC - packwiz managed';files=@();dependencies=@{minecraft=$Channel.minecraft;neoforge=$Channel.neoforge}}
    $manifestPath = Join-Path $Stage 'modrinth.index.json'
    [IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 6), (New-Object Text.UTF8Encoding($false)))
    $temporary = Join-Path $Root ('state/import-'+[Guid]::NewGuid().ToString('N')+'.mrpack')
    $zip = [IO.Compression.ZipFile]::Open($temporary,[IO.Compression.ZipArchiveMode]::Create)
    try {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$manifestPath,'modrinth.index.json') | Out-Null
        $overrides = Join-Path $Stage 'overrides'
        foreach ($file in Get-ChildItem -LiteralPath $overrides -Recurse -File) {
            $relative = $file.FullName.Substring($overrides.Length+1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,('overrides/'+$relative)) | Out-Null
        }
        $runtime = Split-Path (Split-Path $Java -Parent) -Parent
        $runtimeFiles = @(Get-ChildItem -LiteralPath $runtime -Recurse -File)
        $fileCount = 0
        foreach ($file in $runtimeFiles) {
            $relative = $file.FullName.Substring($runtime.Length+1).Replace('\','/')
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,('overrides/java/'+$relative)) | Out-Null
            $fileCount++
            if ($fileCount % 10 -eq 0 -or $fileCount -eq $runtimeFiles.Count) {
                Write-ClientProgress 3 '准备首次安装文件' ('正在打包 '+$file.Name) -Current $fileCount -Total $runtimeFiles.Count
            }
        }
    } finally { $zip.Dispose() }
    Move-Item -LiteralPath $temporary -Destination (Join-Path $Root 'launcher/modpack.mrpack') -Force
}
