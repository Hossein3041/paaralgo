#!/usr/bin/env bash
set -euo pipefail

N1=${1:-921600000}
N2=${2:-1843200000}
#Pmax=${3:-1024}

# ParMergeSort(parMerge)
nohup ./master-skript.sh "$N1" false false  master_pm_par_"$N1".log 2>&1
nohup ./master-skript.sh "$N2" false false  master_pm_par_"$N2".log 2>&1

# ParMergeSort(seqMerge)
nohup ./master-skript.sh "$N1" false true  master_pm_seq_"$N1".log 2>&1
nohup ./master-skript.sh "$N2" false true  master_pm_seq_"$N2".log 2>&1

# Arrays.parallelSort()
nohup ./master-skript.sh "$N1" true true  master_java_par_"$N1".log 2>&1
nohup ./master-skript.sh "$N2" true true  master_java_par_"$N2".log 2>&1


# erste Messung
#echo "Starte Durchlauf für N=$N1 …"
#./master-skript.sh "$N1" "$Pmax" > master_"$N1".log 2>&1
#echo "Fertig mit N=$N1, Log in master_${N1}.log"

# zweite Messung
#echo "Starte Durchlauf für N=$N2 …"
#./master-skript.sh "$N2" "$Pmax" > master_"$N2".log 2>&1
#echo "Fertig mit N=$N2, Log in master_${N2}.log"