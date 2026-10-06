#!/bin/sh
# Compiles every .java file into ./out   (needs JDK 11 or newer)
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name '*.java') && echo "Build OK"
