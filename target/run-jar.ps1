# PowerShell script to run RocketMQTransaction JAR
$ErrorActionPreference = "Stop"

# Check if Java is installed
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java not found. Please install JDK/JRE 8 and ensure 'java' is in the system PATH."
    exit 1
}

# Verify Java version
$javaVersionOutput = Start-Process -FilePath "java" -ArgumentList "-version" -NoNewWindow -RedirectStandardError "java_version.txt" -Wait -PassThru
Get-Content "java_version.txt" | ForEach-Object {
    if ($_ -match 'version "([^"]+)"') {
        $javaVersion = $Matches[1]
        if ($javaVersion -notmatch "^1\.8\.0") {
            Write-Warning "Java version is not 1.8.0. Found: $javaVersion. Program may not work as expected."
        } else {
            Write-Host "Java version: $javaVersion"
        }
    }
}
Remove-Item "java_version.txt" -ErrorAction SilentlyContinue

# Check for JAR file in current directory
$jarFile = "RocketMQTransaction-1.0-SNAPSHOT.jar"
if (-not (Test-Path $jarFile)) {
    Write-Error "JAR file not found: $jarFile. Please ensure it is in the same directory as this script."
    exit 1
}

# Check for config.json in current directory
$configFile = "config.json"
if (-not (Test-Path $configFile)) {
    Write-Error "config.json not found in the current directory. Please ensure it is present."
    exit 1
}

# Check for logback.xml in current directory
$logbackFile = "logback.xml"
if (-not (Test-Path $logbackFile)) {
    Write-Error "logback.xml not found in the current directory. Please ensure it is present."
    exit 1
}

# Run the JAR
Write-Host "Running $jarFile..."
java -jar $jarFile
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to run $jarFile. Exit code: $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Program completed successfully."