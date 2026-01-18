#!/usr/bin/env bash
set -e

javac ParMergeMain.java ParMergeSort.java ParMergeAux.java ParCopy.java

java ParMergeMain
