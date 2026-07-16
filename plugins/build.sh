#!/bin/bash
# Builds MapathonQA.jar from MapathonQA/src, compiled against lib/josm-tested.jar
# lib/josm-tested.jar is gitignored - download it from https://josm.openstreetmap.de/josm-tested.jar
set -e

if [ ! -f lib/josm-tested.jar ]; then
    echo "lib/josm-tested.jar not found. Download it from https://josm.openstreetmap.de/josm-tested.jar"
    exit 1
fi

mkdir -p MapathonQA/build

echo "Compiling..."
javac --release 17 -cp lib/josm-tested.jar -d MapathonQA/build MapathonQA/src/*.java

echo "Packaging..."
jar cfm MapathonQA.jar MapathonQA/MANIFEST.MF -C MapathonQA/build .

echo "Done. Copy MapathonQA.jar to ~/.local/share/JOSM/plugins/ and restart JOSM."
