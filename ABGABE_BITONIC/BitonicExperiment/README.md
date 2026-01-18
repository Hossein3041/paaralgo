Für die Messung ausführen:
nohup ./master-skript.sh > master.log 2>&1 &

Oder mit Parametern ausführen:
nohup ./master-skript.sh 921600000 1024 > master_921600000.log 2>&1 &



_______________________________________________________________________
Für die Messung mit N1 und N2 ausführen (mit Parametern):
nohup ./run-two.sh 921600000 1843200000 1024 &



_______________________________________________________________________
Falls alles erfolgreich, sind wir mit der Messung fertig. Danach gehen wir in den richtigen Verzeichnis rein, wo .tsv drinne ist, und führen aus:
python3 laufzeit.py laufzeit_N_Pmax.tsv             => laufzeit_N_Pmax.png
python3 beschleunigung.py laufzeit_N_Pmax.tsv       => beschleunigung_N_Pmax.png
python3 effizienz.py laufzeit_N_Pmax.tsv            => effizienz_N_Pmax.png



_______________________________________________________________________
Um Prozess zu killen:
ps aux | grep master-skript.sh | grep -v grep
oder
ps aux | grep master-skript.sh

Dann kill <PID>

_______________________________________________________________________
Per SCP Kopieren

Von woanders hierher kopieren-Verzeichnisse:

Von Multicore-Rechner auf Hopper:

 scp -r PASoSe2503@172.19.49.35:/nfshome/users/PASoSe25/PASoSe2503/PA/BitonicExperiment/runs/20250614 .

 scp -r PASoSe2503@172.19.49.35:/nfshome/users/PASoSe25/PASoSe2503/PA/BitonicExperiment/runs/20250614/041633_1843200000_1024 .

 Von Hopper auf Ubuntu:

 scp -r hopper:/home/hosakbari/6-Semester/PA/BitonicExperiment/runs/20250614 .

 scp -r hopper:/home/hosakbari/6-Semester/PA/BitonicExperiment/runs/20250614/041633_1843200000_1024 .
 ---------------------------

 Von Ubuntu auf Hopper:

 Von Hopper auf Multicore-Rechner:

 _______________________________________________________________________

Erklärung Projektstruktur:

# Bitonic Merge Experiment

Dieses Repository enthält die Implementierung, Messskripte und Visualisierungen für den parallelen Bitonic-Merge-Sort (BIM). Ziel ist es, die Laufzeit, Beschleunigung und Effizienz auf einem Multicore-Rechner zu ermitteln und mit anderen Merge-Split-Algorithmen (Odd–Even Merge-Splitting) zu vergleichen.

---

## Projektstruktur

:~/6-Semester/PA/BitonicExperiment$ tree
.
├── BitonicSortMain.java
├── README.md
├── bin
│   ├── BitonicSortMain$BitonicSortThread.class
│   └── BitonicSortMain.class
├── compile.sh  # kompiliert src/ → bin/ + erstellt lib/*.jar
├── lib
│   └── BitonicSortMain.jar # ausführbares JAR, erzeugt durch compile.sh
├── master-skript.sh    # steuert die gesamte Messung für ein N und P_max
├── nohup.out
├── notizen.txt
├── run-two.sh   # führt master-skript.sh nacheinander für N1 und N2 aus
├── runs    # hier landen die Messergebnisse
│   └── 20250614    # Laufzeit pro Tag
│       ├── 024554_921600000_1024   # Laufverzeichnis pro Aufruf
│       │   ├── beschleunigung.py
│       │   ├── beschleunigung_921600000_1024.png
│       │   ├── effizienz.py
│       │   ├── effizienz_921600000_1024.png
│       │   ├── laufzeit.py
│       │   ├── laufzeit_921600000_1024.png
│       │   └── laufzeit_921600000_1024.tsv
│       └── 041633_1843200000_1024
│           ├── beschleunigung.py
│           ├── beschleunigung_1843200000_1024.png
│           ├── effizienz.py
│           ├── effizienz_1843200000_1024.png
│           ├── laufzeit.py
│           ├── laufzeit_1843200000_1024.png
│           └── laufzeit_1843200000_1024.tsv
├── scripts # erzeugt Laufzeit-, Speed-Up- und Effizienz-Plots
│   ├── beschleunigung.py
│   ├── effizienz.py
│   └── laufzeit.py
└── src
    └── BitonicSortMain.java    # Java-Implementierung des BitonicSort

8 directories, 28 files

## Vorbereitung & Kompilieren

1. **Java-Klasse kompilieren**  
   ```bash
   ./compile.sh
- Kompiliert src/BitonicSortMain.java nach bin/
- Erzeugt ausführbares JAR lib/BitonicSortMain.jar

## Messung auf dem Multicore-Server
- Einzeleinsatz für ein N und Pmax

- Hintergrundprozess mit Heartbeat und Log-Ausgabe
nohup ./master-skript.sh <N> <Pmax> > master.log 2>&1 &

- Beispiel
nohup ./master-skript.sh 921600000 1024 > master_921600000.log 2>&1 &

Der master-skript.sh
- legt ein Verzeichnis runs/datum/Zeit_<N>_<Pmax>/ an
- schreibt die TSV-Headerdatei
- kopiert die Python-Skripte dorthin
- startet für jede Zweierpotenz P = 1,2,4,…,Pmax die Messung
- protokolliert jeweils N, P, Laufzeit_ms in die TSV

- Zwei Aufrufe (N1 und N2) hintereinander
./run-two.sh 921600000 1843200000 1024

- Das führt automatisch erst master-skript.sh 921600000 1024 und dann master-skript.sh 1843200000 1024 aus.

## Auswertung & Visualisierung
- Wechsle in das entsprechende Run-Verzeichnis, z. B.:
cd runs/20250614/024554_921600000_1024

- Führe dort die Plots:
python3 laufzeit.py laufzeit_921600000_1024.tsv
python3 beschleunigung.py laufzeit_921600000_1024.tsv
python3 effizienz.py laufzeit_921600000_1024.tsv

- Dadurch entstehen jeweils PNG-Dateien:
laufzeit_*.png
beschleunigung_*.png
effizienz_*.png
