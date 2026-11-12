# Use the wrapper belonging to the task's working directory.
$ErrorActionPreference = 'Stop'
$buildDirectory = (Get-Location).Path
$wrapper = Join-Path $buildDirectory 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper)) {
    throw "No Gradle wrapper in task working directory: $buildDirectory"
}

$properties = Join-Path $buildDirectory 'gradle.properties'
$javaVersion = if (Test-Path -LiteralPath $properties) {
    $versionMatch = Select-String -LiteralPath $properties -Pattern '^java_version=(\d+)\s*$' | Select-Object -First 1
    if ($versionMatch) { $versionMatch.Matches.Groups[1].Value }
}
if ($javaVersion) {
    $candidates = [System.Collections.Generic.List[string]]::new()
    foreach ($candidate in @([Environment]::GetEnvironmentVariable("JAVA_HOME_${javaVersion}_X64"), $env:JAVA_HOME)) {
        if ($candidate) { $candidates.Add($candidate) }
    }
    foreach ($directory in @((Join-Path $env:USERPROFILE '.jdks'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'), (Join-Path $env:ProgramFiles 'Java'), (Join-Path $env:ProgramFiles 'Microsoft'))) {
        if (Test-Path -LiteralPath $directory) {
            Get-ChildItem -LiteralPath $directory -Directory | ForEach-Object { $candidates.Add($_.FullName) }
        }
    }
    $jdk = $candidates | Select-Object -Unique | Where-Object {
        $release = Join-Path $_ 'release'
        (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe')) -and
        (Test-Path -LiteralPath $release) -and
        (Select-String -LiteralPath $release -Pattern ('^JAVA_VERSION="' + $javaVersion + '(\.|"|-)') -Quiet)
    } | Select-Object -First 1
    if (-not $jdk) { throw "JDK $javaVersion not found. Set JAVA_HOME_${javaVersion}_X64 to its installation directory." }
    $env:JAVA_HOME = $jdk
}

& $wrapper @args
exit $LASTEXITCODE
