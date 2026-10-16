#!/bin/sh
# build.sh - builds unicom-cfx.jar (the Java CFX tag cfx_http5) into ./build/  (Linux / macOS - not tested)
# Needs a JDK 22 or newer (javac and jar on the PATH) and two jars to compile against (neither is shipped with the result):
#   lucee.jar      - your Lucee engine's (it holds the com.allaire.cfx interfaces every Java CFX tag implements)
#   slf4j-api.jar  - any SLF4J API 1.7.x or 2.x (Maven Central: org.slf4j:slf4j-api); at run time the server supplies it
# src/ is compiled for Java 21; src22/ (the Windows calls) for Java 22.
# Usage: ./build.sh /path/to/lucee.jar /path/to/slf4j-api.jar
set -e
[ $# -eq 2 ] || { echo "usage: $0 /path/to/lucee.jar /path/to/slf4j-api.jar"; exit 1; }
cd "$(dirname "$0")"
CP="$1:$2:dist/httpclient5-5.6.5.jar:dist/httpcore5-5.4.4.jar:dist/httpcore5-h2-5.4.4.jar"
rm -rf build/classes
mkdir -p build/classes
javac --release 21 -encoding UTF-8 -Xlint:all,-deprecation,-removal -cp "$CP" -d build/classes $(find src -name '*.java')
javac --release 22 -encoding UTF-8 -Xlint:all,-deprecation,-removal,-restricted -cp "build/classes:$CP" -d build/classes $(find src22 -name '*.java')
jar --create --file build/unicom-cfx.jar -C build/classes .
echo "built $(pwd)/build/unicom-cfx.jar"
