param([ValidateSet('','checking','downloading','complete','error')][string]$PreviewState='', [string]$ScreenshotPath='', [switch]$PrepareOnly, [ValidateSet('friends','fool')][string]$Pack='friends')
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationFramework,PresentationCore,WindowsBase
trap {
    if (!$ScreenshotPath) { [Windows.MessageBox]::Show($_.Exception.Message,'Friends MC 窗口错误','OK','Error') | Out-Null }
    Write-Host $_.Exception.Message
    exit 1
}
$script:Root = Split-Path $PSScriptRoot -Parent
$script:worker = $null
$script:finished = $false
$script:started = [DateTime]::Now
$script:logText = ''
$script:lastUpdate = ''
$script:previewCaptured = $false
$stateDir = Join-Path $Root 'state/progress'
New-Item -ItemType Directory -Force -Path $stateDir | Out-Null
$runId = [DateTime]::Now.ToString('yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$script:statusFile = Join-Path $stateDir ($runId+'.json')
$script:logFile = Join-Path $stateDir ($runId+'.log')
[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
 xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
 Title="Friends MC · 更新中心" Width="840" Height="640" MinWidth="780" MinHeight="600"
 WindowStartupLocation="CenterScreen" Background="#0C141B" Foreground="#EEF4F7"
 FontFamily="Microsoft YaHei UI" ResizeMode="CanMinimize">
 <Window.Resources>
  <Style TargetType="Button">
   <Setter Property="FontSize" Value="13"/><Setter Property="Padding" Value="20,10"/>
   <Setter Property="Cursor" Value="Hand"/><Setter Property="Foreground" Value="#C6D4DE"/>
   <Setter Property="Background" Value="#1C2B36"/><Setter Property="BorderBrush" Value="#30434F"/>
   <Setter Property="Template"><Setter.Value><ControlTemplate TargetType="Button">
    <Border x:Name="Frame" Background="{TemplateBinding Background}" BorderBrush="{TemplateBinding BorderBrush}" BorderThickness="1" CornerRadius="8" Padding="{TemplateBinding Padding}">
     <ContentPresenter HorizontalAlignment="Center" VerticalAlignment="Center"/>
    </Border><ControlTemplate.Triggers><Trigger Property="IsMouseOver" Value="True"><Setter TargetName="Frame" Property="Opacity" Value="0.8"/></Trigger><Trigger Property="IsEnabled" Value="False"><Setter TargetName="Frame" Property="Opacity" Value="0.4"/></Trigger></ControlTemplate.Triggers>
   </ControlTemplate></Setter.Value></Setter>
  </Style>
 </Window.Resources>
 <Grid Margin="32,26,32,24">
  <Grid.RowDefinitions><RowDefinition Height="Auto"/><RowDefinition Height="142"/><RowDefinition Height="*"/><RowDefinition Height="Auto"/></Grid.RowDefinitions>
  <Grid Margin="0,0,0,22">
   <StackPanel Orientation="Horizontal"><Border Width="26" Height="26" Background="#A9E4B8" CornerRadius="5" Margin="0,0,10,0"><TextBlock Text="F" FontWeight="Bold" Foreground="#102B23" FontSize="18" HorizontalAlignment="Center" VerticalAlignment="Center"/></Border><TextBlock Text="FRIENDS MC" FontWeight="SemiBold" FontSize="16" VerticalAlignment="Center"/><TextBlock Text=" /  更新中心" Foreground="#748C9C" FontSize="12" VerticalAlignment="Center"/></StackPanel>
   <Border HorizontalAlignment="Right" Background="#182B29" CornerRadius="12" Padding="12,5"><TextBlock Text="PCL · Java 版" Foreground="#A9E4B8" FontSize="11"/></Border>
  </Grid>
  <Border Grid.Row="1" CornerRadius="14" Margin="0,0,0,20" ClipToBounds="True">
   <Border.Background><LinearGradientBrush StartPoint="0,0" EndPoint="1,1"><GradientStop Color="#183B39" Offset="0"/><GradientStop Color="#152832" Offset="1"/></LinearGradientBrush></Border.Background>
   <Grid Margin="24,16">
    <StackPanel VerticalAlignment="Center"><TextBlock Text="准备好，一起进入世界。" FontSize="25" FontWeight="SemiBold" Margin="0,0,0,9"/><TextBlock Text="同步服务器版本，让每一次出发都保持一致。" Foreground="#A3BEBE" FontSize="12"/></StackPanel>
    <Canvas Width="130" Height="90" HorizontalAlignment="Right" Opacity="0.65" IsHitTestVisible="False">
     <Path Fill="#305E56" Data="M 0 85 L 0 55 L 25 55 L 25 35 L 50 35 L 50 15 L 80 15 L 80 45 L 105 45 L 105 65 L 130 65 L 130 85 Z"/>
     <Path Fill="#568B70" Data="M 40 85 L 40 65 L 65 65 L 65 45 L 95 45 L 95 25 L 115 25 L 115 60 L 130 60 L 130 85 Z"/>
     <Rectangle Width="13" Height="13" Fill="#B6E0AD" Canvas.Left="100" Canvas.Top="0"/>
    </Canvas>
   </Grid>
  </Border>
  <Border Grid.Row="2" Background="#131F29" BorderBrush="#23333E" BorderThickness="1" CornerRadius="14" Padding="24,20">
   <Grid>
    <Grid.RowDefinitions><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="*"/></Grid.RowDefinitions>
    <UniformGrid Columns="4" Margin="0,0,0,22">
     <TextBlock x:Name="Step1" Text="01  环境检查" FontSize="12"/><TextBlock x:Name="Step2" Text="02  版本核对" FontSize="12"/><TextBlock x:Name="Step3" Text="03  文件同步" FontSize="12"/><TextBlock x:Name="Step4" Text="04  打开 PCL" FontSize="12"/>
    </UniformGrid>
    <Grid Grid.Row="1" Margin="0,0,0,10"><TextBlock x:Name="StatusTitle" Text="正在准备更新" FontSize="21" FontWeight="SemiBold"/><TextBlock x:Name="CountLabel" HorizontalAlignment="Right" VerticalAlignment="Center" FontSize="12" Foreground="#A9E4B8"/></Grid>
    <TextBlock Grid.Row="2" x:Name="StatusDetail" Text="窗口已就绪，正在启动检查程序…" Foreground="#9AB0C0" FontSize="12" TextWrapping="Wrap" MaxHeight="48" Margin="0,0,0,17"/>
    <StackPanel Grid.Row="3"><ProgressBar x:Name="Progress" Height="7" IsIndeterminate="True" Foreground="#A9E4B8" Background="#273843" BorderThickness="0"/><Grid Margin="0,12,0,0"><TextBlock x:Name="VersionLabel" Text="正在获取发布版本" Foreground="#6F8B9D" FontSize="10"/><TextBlock x:Name="ElapsedLabel" Text="已用时 00:00" HorizontalAlignment="Right" Foreground="#6F8B9D" FontSize="10"/></Grid></StackPanel>
    <Expander Grid.Row="4" x:Name="Details" Header="查看运行详情" Foreground="#9AB0C0" FontSize="11" Margin="0,18,0,0" VerticalAlignment="Bottom">
     <TextBox x:Name="LogBox" Height="74" Margin="0,8,0,0" Background="#0D171F" Foreground="#AAC0CE" BorderThickness="0" IsReadOnly="True" TextWrapping="Wrap" VerticalScrollBarVisibility="Auto" FontFamily="Consolas" FontSize="10" Padding="8"/>
    </Expander>
   </Grid>
  </Border>
  <Grid Grid.Row="3" Margin="0,20,0,0">
   <TextBlock Text="游戏本体下载进度将在 PCL 中显示" Foreground="#6F8797" FontSize="11" VerticalAlignment="Center"/>
   <StackPanel Orientation="Horizontal" HorizontalAlignment="Right"><Button x:Name="CopyLog" Content="复制日志" Margin="0,0,10,0"/><Button x:Name="CloseButton" Content="取消" Background="#A9E4B8" Foreground="#153427" BorderBrush="#A9E4B8"/></StackPanel>
  </Grid>
 </Grid>
</Window>
'@
$reader = New-Object Xml.XmlNodeReader $xaml
$script:window = [Windows.Markup.XamlReader]::Load($reader)
$script:controls = @{}
foreach ($name in @('Step1','Step2','Step3','Step4','StatusTitle','StatusDetail','CountLabel','Progress','VersionLabel','ElapsedLabel','Details','LogBox','CopyLog','CloseButton')) { $controls[$name] = $window.FindName($name) }

function Set-Display($data) {
    $controls.StatusTitle.Text = $data.title
    $controls.StatusDetail.Text = $data.detail
    if ($data.release) { $controls.VersionLabel.Text = $data.release }
    foreach ($i in 1..4) {
        $controls['Step'+$i].Foreground = if ($i -le $data.step) { '#A9E4B8' } else { '#536B7C' }
    }
    $controls.Progress.IsIndeterminate = ($data.total -le 0 -and $data.state -eq 'running')
    if ($data.total -gt 0) {
        $controls.Progress.Maximum = $data.total
        $controls.Progress.Value = [Math]::Min($data.current,$data.total)
        $controls.CountLabel.Text = '{0} / {1} 文件' -f $data.current,$data.total
    } else { $controls.CountLabel.Text = '' }
    if ($data.state -eq 'complete') {
        $controls.Progress.Maximum=1; $controls.Progress.Value=1
        $controls.CountLabel.Text='已完成'
    }
    if ($data.state -eq 'error') {
        $controls.StatusTitle.Foreground='#F2ADA4'; $controls.Progress.Foreground='#F2ADA4'
        $controls.Progress.Value=0; $controls.Details.IsExpanded=$true
        $controls.CountLabel.Text='需要处理'
    }
}
function Stop-OwnedWorker {
    if ($script:worker -and !$script:worker.HasExited) {
        # Kill only the worker we created and its descendants (including packwiz).
        $kill = New-Object Diagnostics.ProcessStartInfo
        $kill.FileName = Join-Path $env:SystemRoot 'System32/taskkill.exe'
        $kill.Arguments='/PID '+$script:worker.Id+' /T /F'
        $kill.UseShellExecute=$false; $kill.CreateNoWindow=$true
        $task = [Diagnostics.Process]::Start($kill); $task.WaitForExit(5000) | Out-Null; $task.Dispose()
    }
}
function Save-WindowCapture {
    $window.UpdateLayout()
    $bitmap=New-Object Windows.Media.Imaging.RenderTargetBitmap ([int]$window.ActualWidth),([int]$window.ActualHeight),96,96,([Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($window)
    $encoder=New-Object Windows.Media.Imaging.PngBitmapEncoder
    $encoder.Frames.Add([Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
    $stream=[IO.File]::Create($ScreenshotPath)
    try { $encoder.Save($stream) } finally { $stream.Dispose() }
}
$controls.CopyLog.Add_Click({
    $text = $controls.StatusTitle.Text+"`r`n"+$controls.StatusDetail.Text+"`r`n"+$controls.VersionLabel.Text+"`r`n"+$script:logText
    [Windows.Clipboard]::SetText($text)
    $controls.CopyLog.Content='已复制'
})
$controls.CloseButton.Add_Click({ $window.Close() })
$window.Add_Closing({
    param($sender,$eventArgs)
    if (!$script:finished -and $script:worker -and !$script:worker.HasExited) {
        $answer=[Windows.MessageBox]::Show($window,'更新尚未完成。取消后，下次启动会重新检查文件。是否取消？','取消更新','YesNo','Question')
        if ($answer -ne 'Yes') { $eventArgs.Cancel=$true; return }
        Stop-OwnedWorker
    }
})
$script:timer = New-Object Windows.Threading.DispatcherTimer
$timer.Interval=[TimeSpan]::FromMilliseconds(180)
$timer.Add_Tick({
    $elapsed=[DateTime]::Now-$script:started
    $controls.ElapsedLabel.Text='已用时 '+$elapsed.ToString('mm\:ss')
    if ($PreviewState) { return }
    if (Test-Path -LiteralPath $script:statusFile) {
        try {
            $data=Get-Content -LiteralPath $script:statusFile -Raw -Encoding UTF8 | ConvertFrom-Json
            if ($data.updated -ne $script:lastUpdate) {
                Set-Display $data
                $script:lastUpdate=$data.updated
                $controls.LogBox.AppendText($data.title+' · '+$data.detail+"`r`n")
                $controls.LogBox.ScrollToEnd()
            }
        } catch { } # A partial status write is retried at the next tick.
    }
    if ($script:worker -and $script:worker.HasExited -and !$script:finished) {
        $script:finished=$true
        $script:logText=$script:outTask.Result+"`r`n"+$script:errTask.Result
        [IO.File]::WriteAllText($script:logFile,$script:logText,(New-Object Text.UTF8Encoding($false)))
        $controls.LogBox.Text=$script:logText
        if ($script:worker.ExitCode -ne 0) {
            $detail=if ($data -and $data.state -eq 'error') { $data.detail } else { '检查程序未正常完成。展开详情或复制日志后发给服主。' }
            Set-Display @{step=0;title='暂时无法完成更新';detail=$detail;state='error';current=0;total=0}
        } else {
            $successDetail=if ($PrepareOnly) { '文件检查已完成。本次测试未打开 PCL。' } else { 'PCL 已打开。登录账号后，点击「启动游戏」。' }
            Set-Display @{step=4;title='准备就绪，出发吧。';detail=$successDetail;state='complete';current=1;total=1}
        }
        $controls.CloseButton.Content='完成'
        $timer.Stop()
        if ($ScreenshotPath) { Save-WindowCapture; $window.Close() }
    }
})
$window.Add_ContentRendered({
    if ($PreviewState) {
        $samples=@{
            checking=@{step=2;title='核对服务器版本';detail='正在获取最新发布清单，请保持网络连接';current=0;total=0;state='running'}
            downloading=@{step=3;title='同步模组与配置';detail='正在下载并校验模组文件';current=18;total=32;state='running'}
            complete=@{step=4;title='准备就绪，出发吧。';detail='PCL 已打开。登录账号后，点击「启动游戏」。';current=1;total=1;state='complete'}
            error=@{step=0;title='暂时无法完成更新';detail='无法连接更新地址。请检查网络后重试，或复制日志发给服主。';current=0;total=0;state='error'}
        }
        $sample=$samples[$PreviewState]; $sample.release='MC 26.3  /  NeoForge 26.3.0.26-beta  /  整合包 0.3.0'
        Set-Display $sample; $script:finished=$true
        $controls.LogBox.Text='Preview only — no downloads or game launch.'
        if ($PreviewState -in @('complete','error')) { $controls.CloseButton.Content='完成' }
        if ($ScreenshotPath) {
            Save-WindowCapture
            $window.Close()
        }
        return
    }
    try {
        $start=New-Object Diagnostics.ProcessStartInfo
        $start.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
        $start.Arguments='-NoProfile -ExecutionPolicy Bypass -File "'+(Join-Path $PSScriptRoot 'Start.ps1')+'" -StatusPath "'+$script:statusFile+'"'
        $start.Arguments+=' -Pack '+$Pack
        if ($PrepareOnly) { $start.Arguments+=' -PrepareOnly' }
        $start.UseShellExecute=$false; $start.CreateNoWindow=$true
        $start.RedirectStandardOutput=$true; $start.RedirectStandardError=$true
        $start.StandardOutputEncoding=[Text.Encoding]::UTF8; $start.StandardErrorEncoding=[Text.Encoding]::UTF8
        $script:worker=New-Object Diagnostics.Process
        $script:worker.StartInfo=$start; $script:worker.Start() | Out-Null
        $script:outTask=$script:worker.StandardOutput.ReadToEndAsync()
        $script:errTask=$script:worker.StandardError.ReadToEndAsync()
        $timer.Start()
    } catch {
        $script:finished=$true
        Set-Display @{step=0;title='无法启动检查程序';detail=$_.Exception.Message;state='error';current=0;total=0}
        $controls.CloseButton.Content='关闭'
    }
})
if ($ScreenshotPath) { $window.WindowStartupLocation='Manual'; $window.Left=-10000; $window.Top=-10000; $window.ShowInTaskbar=$false }
try { $window.ShowDialog() | Out-Null } finally { $timer.Stop(); if ($script:worker) { $script:worker.Dispose() } }
