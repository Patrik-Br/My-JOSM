@echo off
REM Builds MapathonQA.jar from MapathonQA/src, compiled against lib/josm-tested.jar
REM lib/josm-tested.jar is gitignored - download it from https://josm.openstreetmap.de/josm-tested.jar

if not exist lib\josm-tested.jar (
    echo lib\josm-tested.jar not found. Download it from https://josm.openstreetmap.de/josm-tested.jar
    exit /b 1
)

if not exist MapathonQA\build mkdir MapathonQA\build

echo Compiling...
javac --release 17 -cp lib\josm-tested.jar -d MapathonQA\build MapathonQA\src\*.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo Packaging...
jar cfm MapathonQA.jar MapathonQA\MANIFEST.MF -C MapathonQA\build .

echo Done. Copy MapathonQA.jar to %%APPDATA%%\JOSM\plugins\ and restart JOSM.
