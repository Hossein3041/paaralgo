#!/usr/bin/env bash
set -euo pipefail

rm -rf bin/*
mkdir -p bin

javac -d bin src/ParMergeMain.java

mkdir -p lib
rm -f lib/ParMergeMain.jar
jar cfe lib/ParMergeMain.jar ParMergeMain -C bin .
