<#
 build.ps1 - builds unicom-cfx.jar (the Java CFX tag cfx_http5) into .\build\
 Needs a JDK 22 or newer, and two jars to compile against (neither is shipped with the result):
   lucee.jar      - your Lucee engine's (it holds the com.allaire.cfx interfaces every Java CFX tag implements)
   slf4j-api.jar  - any SLF4J API 1.7.x or 2.x (Maven Central: org.slf4j:slf4j-api); at run time the server supplies it
 src\ is compiled for Java 21; src22\ (the Windows calls) for Java 22, so the jar still loads on Java 21 (without the
 Windows-only features).
 Usage:
   powershell -ExecutionPolicy Bypass -File build.ps1 -LuceeJar <path\lucee.jar> -Slf4jApi <path\slf4j-api.jar> [-Jdk <JDK folder>]
#>
param(
    [Parameter(Mandatory = $true)][string] $LuceeJar,
    [Parameter(Mandatory = $true)][string] $Slf4jApi,
    [string] $Jdk = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$bin = if ($Jdk) { Join-Path $Jdk 'bin' } else { '' }
$javac = if ($bin) { Join-Path $bin 'javac.exe' } else { 'javac' }
$jar = if ($bin) { Join-Path $bin 'jar.exe' } else { 'jar' }
$apache = 'httpclient5-5.6.5.jar', 'httpcore5-5.4.4.jar', 'httpcore5-h2-5.4.4.jar'
$cp = (@($LuceeJar, $Slf4jApi) + ($apache | ForEach-Object { Join-Path $here "dist\$_" })) -join ';'
$classes = Join-Path $here 'build\classes'
if (Test-Path $classes) { Remove-Item -Recurse -Force $classes }
New-Item -ItemType Directory -Force $classes | Out-Null
$src = @(Get-ChildItem (Join-Path $here 'src') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac --release 21 -encoding UTF-8 -Xlint:all,-deprecation,-removal -cp $cp -d $classes @src
if ($LASTEXITCODE -ne 0) { throw "javac failed ($LASTEXITCODE)" }
$src22 = @(Get-ChildItem (Join-Path $here 'src22') -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& $javac --release 22 -encoding UTF-8 -Xlint:all,-deprecation,-removal,-restricted -cp "$classes;$cp" -d $classes @src22
if ($LASTEXITCODE -ne 0) { throw "javac (src22) failed ($LASTEXITCODE)" }
$out = Join-Path $here 'build\unicom-cfx.jar'
& $jar --create --file $out -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed ($LASTEXITCODE)" }
"built $out  SHA256 $((Get-FileHash $out).Hash)"
