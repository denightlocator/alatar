$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Save-JdkHome([string] $jdkHome) {
    $toolsPath = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\.tools'))
    New-Item -ItemType Directory -Force -Path $toolsPath | Out-Null
    [System.IO.File]::WriteAllText((Join-Path $toolsPath 'java-home.txt'), $jdkHome)
}

function Test-Jdk25([string] $jdkHome) {
    if ([string]::IsNullOrWhiteSpace($jdkHome)) { return $false }
    $java = Join-Path $jdkHome 'bin\java.exe'
    $javac = Join-Path $jdkHome 'bin\javac.exe'
    if (!(Test-Path $java) -or !(Test-Path $javac)) { return $false }
    try {
        $versionText = (& $java -version 2>&1 | Out-String)
    }
    catch {
        # Windows PowerShell 5 can treat java.exe's version output on stderr as a terminating error.
        $versionText = $_.Exception.Message
    }
    if ($versionText -match 'version\s+"(?<major>\d+)') {
        return ([int]$Matches.major -eq 25)
    }
    return $false
}

function Find-Jdk25 {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }
    $localJdk = Join-Path $PSScriptRoot '..\.tools\jdk25'
    $candidates.Add([System.IO.Path]::GetFullPath($localJdk))

    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidates.Add((Split-Path -Parent (Split-Path -Parent $javaCommand.Source)))
    }

    foreach ($programRoot in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if ([string]::IsNullOrWhiteSpace($programRoot)) { continue }
        foreach ($pattern in @('Eclipse Adoptium\jdk-25*', 'Java\jdk-25*')) {
            $parent = Join-Path $programRoot (Split-Path $pattern -Parent)
            $leaf = Split-Path $pattern -Leaf
            if (Test-Path $parent) {
                Get-ChildItem $parent -Directory -Filter $leaf -ErrorAction SilentlyContinue |
                    ForEach-Object { $candidates.Add($_.FullName) }
            }
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (Test-Jdk25 $candidate) { return $candidate }
    }
    return $null
}

$existing = Find-Jdk25
if ($existing) {
    Save-JdkHome $existing
    exit 0
}

$architecture = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64') { 'aarch64' } else { 'x64' }
$api = "https://api.adoptium.net/v3/assets/latest/25/hotspot?architecture=$architecture&image_type=jdk&os=windows&vendor=eclipse"
$installRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\.tools'))
$installPath = Join-Path $installRoot 'jdk25'
$tempPath = Join-Path $env:TEMP ("atlas-jdk25-" + [Guid]::NewGuid().ToString('N'))
$archivePath = Join-Path $tempPath 'jdk25.zip'

try {
    [Console]::Error.WriteLine('Java 25 was not found. Downloading the Temurin JDK 25 portable archive...')
    $asset = (Invoke-RestMethod -Uri $api -Headers @{ 'User-Agent' = 'AtlasClientBuild' })[0]
    $downloadUrl = $asset.binary.package.link
    if ([string]::IsNullOrWhiteSpace($downloadUrl)) { throw 'The JDK download URL was not returned by Adoptium.' }

    New-Item -ItemType Directory -Force -Path $tempPath | Out-Null
    New-Item -ItemType Directory -Force -Path $installRoot | Out-Null
    Invoke-WebRequest -Uri $downloadUrl -OutFile $archivePath -UseBasicParsing | Out-Null
    $expectedChecksum = $asset.binary.package.checksum
    if ($expectedChecksum) {
        $actualChecksum = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actualChecksum -ne $expectedChecksum.ToLowerInvariant()) { throw 'The JDK archive checksum did not match Adoptium.' }
    }
    Expand-Archive -LiteralPath $archivePath -DestinationPath $tempPath -Force | Out-Null

    $jdkDirectory = Get-ChildItem $tempPath -Directory |
        Where-Object { Test-Path (Join-Path $_.FullName 'bin\javac.exe') } |
        Select-Object -First 1
    if (!$jdkDirectory) { throw 'The downloaded archive did not contain a Windows JDK.' }

    if (Test-Path $installPath) { Remove-Item -LiteralPath $installPath -Recurse -Force }
    Move-Item -LiteralPath $jdkDirectory.FullName -Destination $installPath
    if (!(Test-Jdk25 $installPath)) { throw 'The downloaded JDK did not pass the Java 25 check.' }
    $oldJdk21 = Join-Path $installRoot 'jdk21'
    if (Test-Path $oldJdk21) { Remove-Item -LiteralPath $oldJdk21 -Recurse -Force }

    Save-JdkHome $installPath
    exit 0
}
catch {
    [Console]::Error.WriteLine("Java setup failed: $($_.Exception.Message)")
    [Console]::Error.WriteLine('Install a 64-bit JDK 25, then double-click start.bat again.')
    exit 1
}
finally {
    if (Test-Path $tempPath) { Remove-Item -LiteralPath $tempPath -Recurse -Force -ErrorAction SilentlyContinue }
}
