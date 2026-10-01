param([ValidateSet('','friends','fool')][string]$Pack='')
$ErrorActionPreference='Stop'
if (!$Pack) {
    Add-Type -AssemblyName PresentationFramework
    [xml]$view=@'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml" Title="Friends MC · 选择世界" Width="650" Height="390" WindowStartupLocation="CenterScreen" ResizeMode="NoResize" Background="#0C141B" Foreground="#EEF4F7" FontFamily="Microsoft YaHei UI">
 <StackPanel Margin="32">
  <TextBlock Text="今天，去哪个世界？" FontSize="28" FontWeight="SemiBold" Margin="0,0,0,10"/>
  <TextBlock Text="各自更新 · 独立存档 · 保留个人设置" Foreground="#A3BEBE" Margin="0,0,0,24"/>
  <Button x:Name="Friends" Padding="20,14" Margin="0,0,0,12" Background="#193D37" Foreground="White" BorderThickness="0" HorizontalContentAlignment="Left"><StackPanel><TextBlock Text="好友生存服" FontSize="19"/><TextBlock Text="Minecraft 26.3 · NeoForge" Foreground="#B8D6CE" Margin="0,5,0,0"/></StackPanel></Button>
  <Button x:Name="Fool" Padding="20,14" Background="#343050" Foreground="White" BorderThickness="0" HorizontalContentAlignment="Left"><StackPanel><TextBlock Text="愚者 · The Fool" FontSize="19"/><TextBlock Text="Minecraft 1.20.1 · Forge · 独立整合包" Foreground="#CCC6E3" Margin="0,5,0,0"/></StackPanel></Button>
  <TextBlock Text="进服前确认服主当前开放的整合包；此处选择不会切换远程服务器。" Foreground="#879BA9" FontSize="11" Margin="0,20,0,0"/>
 </StackPanel>
</Window>
'@
    $window=[Windows.Markup.XamlReader]::Load((New-Object Xml.XmlNodeReader $view))
    $script:selected=''
    $window.FindName('Friends').Add_Click({$script:selected='friends';$window.Close()})
    $window.FindName('Fool').Add_Click({$script:selected='fool';$window.Close()})
    $null=$window.ShowDialog()
    $Pack=$script:selected
    if (!$Pack) { exit 0 }
}
& (Join-Path $PSScriptRoot 'ProgressWindow.ps1') -Pack $Pack
