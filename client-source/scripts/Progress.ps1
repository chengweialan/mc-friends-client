# Shared worker-to-window status. The UI retries if it reads during a write.
function Write-ClientProgress {
    param([int]$Step, [string]$Title, [string]$Detail='', [int]$Current=0, [int]$Total=0, [string]$State='running')
    if (!$script:StatusPath) { return }
    $data = @{step=$Step;title=$Title;detail=$Detail;current=$Current;total=$Total;state=$State;updated=[DateTime]::UtcNow.ToString('o')}
    if ($script:ReleaseLabel) { $data.release=$script:ReleaseLabel }
    $text = $data | ConvertTo-Json -Compress
    [IO.File]::WriteAllText($script:StatusPath, $text, (New-Object Text.UTF8Encoding($false)))
}
