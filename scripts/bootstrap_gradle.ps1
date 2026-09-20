$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Jar = Join-Path $Root "gradle\wrapper\gradle-wrapper.jar"
$Url = "https://raw.githubusercontent.com/gradle/gradle/v9.3.1/gradle/wrapper/gradle-wrapper.jar"
$Expected = "b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13"
if (!(Test-Path $Jar)) { Invoke-WebRequest -Uri $Url -OutFile $Jar }
$Actual = (Get-FileHash -Algorithm SHA256 $Jar).Hash.ToLower()
if ($Actual -ne $Expected) { throw "Checksum wrapper inválido: $Actual" }
& (Join-Path $Root "gradlew.bat") $args
