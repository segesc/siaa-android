$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Jar = Join-Path $Root "gradle\wrapper\gradle-wrapper.jar"
$Url = "https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"
$Expected = "497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"
if (!(Test-Path $Jar)) { Invoke-WebRequest -Uri $Url -OutFile $Jar }
$Actual = (Get-FileHash -Algorithm SHA256 $Jar).Hash.ToLower()
if ($Actual -ne $Expected) { throw "Checksum wrapper inválido: $Actual" }
& (Join-Path $Root "gradlew.bat") $args
