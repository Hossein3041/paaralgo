#!/usr/bin/env bash
set -euo pipefail

rm -rf bin/*
mkdir -p bin

javac -d bin src/BitonicSortMain.java

mkdir -p lib
rm -f lib/BitonicSortMain.jar
jar cfe lib/BitonicSortMain.jar BitonicSortMain -C bin .
