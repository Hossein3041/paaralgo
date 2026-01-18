#!/usr/bin/env bash
#set -x
set -euo pipefail

# === Parameter ===
#N=20971520
N=${1:-1048576}
#Pmax=1024
Pmax=${2:-1024}
SEED=0

# Nur Zweierpotenzen bis Pmax
THREADS=(1 2 4 8 16 32 64 128 256 512 1024)

# Laufverzeichnis anlegen
DATE_DIR=$(date +%Y%m%d)
BASE_DIR="runs/${DATE_DIR}"
mkdir -p "$BASE_DIR"

# Zeitstempel für diesen Run
TIME_STAMP=$(date +%H%M%S)
RUN_DIR="${BASE_DIR}/${TIME_STAMP}_${N}_${Pmax}"
mkdir -p "$RUN_DIR"

# TSV-Datei mit Header
TSV="laufzeit_${N}_${Pmax}.tsv"
echo -e "N\tP\tLaufzeit_ms" > "$RUN_DIR/$TSV"

# Python-Skripte kopieren
cp scripts/*.py "$RUN_DIR/"

# Datei, in der wir das aktuelle P speichern:
STATUS_FILE="$RUN_DIR/.current_P"
echo "unbekannt" > "$STATUS_FILE"

# Heartbeat im Hintergrund: Liest für gegebene Zeit das STATUS_FILE und gibt Status aus
(
    while true; do
        now=$(date +'%a %d.%m.%Y %T %Z')
        current_p=$(<"$STATUS_FILE")
        echo "[INFO] $now → Läuft noch: N=${N}, P=${current_p}"
        sleep 300
    done
) &
HEARTBEAT_PID=$!

# Mess-Schleife
for P in "${THREADS[@]}"; do
    # Schreibe das aktuelle P in die Datei
    echo "$P" > "$STATUS_FILE"
    
    echo -n "[$(date +%H:%M:%S)] Messen mit P=$P ..."
    java -Xmx192g -jar lib/BitonicSortMain.jar \
        --size "$N" --threads "$P" --seed "$SEED" \
        >> "$RUN_DIR/$TSV"
    echo "[$(date +%H:%M:%S)] P=${P} done"
done

kill $HEARTBEAT_PID

echo "Messung komplett. Ergebnisse liegen in $RUN_DIR/$TSV"