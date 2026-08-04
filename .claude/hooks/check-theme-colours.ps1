<#
    Flags hard-coded colours in Compose UI files outside ui/theme/.

    The app supports light theme, dark theme and dynamic colour. A literal Color(0xFF...) in a
    screen or component looks right in whichever theme it was written against and wrong in the
    other two — and nothing fails, so it survives review and ships.

    ui/theme/ is exempt: that is where the palette, the card colours and the service accent tiles
    legitimately live.

    Registered as a PostToolUse hook in .claude/settings.json. Exits 2 with the offending lines on
    stderr so the agent sees them and can fix them immediately; exits 0 for everything else,
    including edits to unrelated files.
#>

$ErrorActionPreference = 'Stop'

try {
    $payload = [Console]::In.ReadToEnd() | ConvertFrom-Json
} catch {
    exit 0
}

$editedPath = $payload.tool_input.file_path
if (-not $editedPath) { exit 0 }

$normalised = $editedPath -replace '\\', '/'

# Only Compose UI sources, and never the theme package itself.
if ($normalised -notmatch '/com/subkan/ui/.*\.kt$') { exit 0 }
if ($normalised -match '/com/subkan/ui/theme/') { exit 0 }
if (-not (Test-Path $editedPath)) { exit 0 }

$offenders = Select-String -Path $editedPath -Pattern 'Color\(\s*0x[0-9A-Fa-f]{6,8}' -AllMatches

if (-not $offenders) { exit 0 }

$report = @("Hard-coded colours found in $normalised.")
foreach ($hit in $offenders) {
    $report += ("  line {0}: {1}" -f $hit.LineNumber, $hit.Line.Trim())
}
$report += 'Use a MaterialTheme.colorScheme role instead, or move the value into ui/theme/ if it is'
$report += 'genuinely theme-independent (see .claude/rules/compose-ui.md).'

[Console]::Error.WriteLine($report -join "`n")
exit 2
