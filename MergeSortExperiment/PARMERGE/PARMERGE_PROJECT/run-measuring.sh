#!/usr/bin/env bash
set -euo pipefail

rm -rf logs/*
mkdir -p logs

# Von draußen ausführen als zum Beispiel
# nohup ./run-measuring.sh 921600000 1843200000 &
# nohup ./run-measuring.sh 921600 1843200 2>&1 &

N1=${1:-921600000}
N2=${2:-1843200000}
Pmax=1024

# ParMergeSort(parMerge)
nohup ./master-skript.sh "$N1" "$Pmax" false false > logs/master_pm_par_"$N1".log 2>&1
echo "Fertig mit ParMergeSort(parMerge) für N=${N1}"
sleep 60
nohup ./master-skript.sh "$N2" "$Pmax" false false > logs/master_pm_par_"$N2".log 2>&1
echo "Fertig mit ParMergeSort(parMerge) für N=${N2}"

sleep 60

# ParMergeSort(seqMerge)
nohup ./master-skript.sh "$N1" "$Pmax" false true > logs/master_pm_seq_"$N1".log 2>&1
echo "Fertig mit ParMergeSort(seqMerge) für N=${N1}"
sleep 60
nohup ./master-skript.sh "$N2" "$Pmax" false true > logs/master_pm_seq_"$N2".log 2>&1
echo "Fertig mit ParMergeSort(seqMerge) für N=${N2}"

sleep 60

# Arrays.parallelSort()
nohup ./master-skript.sh "$N1" "$Pmax" true true > logs/master_java_par_"$N1".log 2>&1
echo "Fertig mit Arrays.parallelSort() für N=${N1}"
sleep 60
nohup ./master-skript.sh "$N2" "$Pmax" true true > logs/master_java_par_"$N2".log 2>&1
echo "Fertig mit Arrays.parallelSort() für N=${N2}"

# erste Messung
#echo "Starte Durchlauf für N=$N1 …"
#./master-skript.sh "$N1" "$Pmax" > master_"$N1".log 2>&1
#echo "Fertig mit N=$N1, Log in master_${N1}.log"

# zweite Messung
#echo "Starte Durchlauf für N=$N2 …"
#./master-skript.sh "$N2" "$Pmax" > master_"$N2".log 2>&1
#echo "Fertig mit N=$N2, Log in master_${N2}.log"