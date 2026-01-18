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