#!/bin/sh
# Compiles and runs the Minecraft-free lore checks with plain javac (no Gradle or Minecraft needed).
set -e
cd "$(dirname "$0")/.."
out=$(mktemp -d)
javac -d "$out" src/main/java/com/cstestcs/bioniclemasks/lore/*.java tools/loretest/LoreCheck.java
java -cp "$out" LoreCheck
