#!/usr/bin/env python3
import sys
import os
import matplotlib.pyplot as plt
import pandas as pd

def main(tsv_path):
    # TSV einlesen
    df = pd.read_csv(tsv_path, sep='\t')
    base = os.path.basename(tsv_path)
    name, _ = os.path.splitext(base)

    # N und Pmax aus Dateiname extrahieren
    try:
        _, N_str, Pmax_str = name.split('_')
    except ValueError:
        N_str, Pmax_str = '', ''

    # Sicherstellen, dass P=1 dabei ist (für t(1))
    if 1 not in df['P'].values:
        print("Fehler: kein P=1 in der Datei vorhanden!", file=sys.stderr)
        sys.exit(1)
    t1 = df.loc[df['P'] == 1, 'Laufzeit_ms'].iloc[0]

    # Beschleunigung s(p) = t(1) / t(p)
    df['Speedup'] = t1 / df['Laufzeit_ms']
    # Effizienz e(p) = s(p) / p
    df['Efficiency'] = df['Speedup'] / df['P']

    # Plot
    plt.figure()
    plt.plot(df['P'], df['Efficiency'], marker='o')
    plt.xscale('log', base=2)
    plt.xticks(df['P'], df['P'])
    plt.xlabel('Anzahl Threads (P)')
    plt.ylabel('Effizienz $e(P)=s(P)/P$')
    plt.title(f"Effizienz vs Threads (N={N_str}, Pmax={Pmax_str})")
    plt.ylim(0, 1.05)
    plt.grid(True, which='both', ls='--', lw=0.5)

    # Speichern
    out_png = f"effizienz_{N_str}_{Pmax_str}.png"
    plt.tight_layout()
    plt.savefig(out_png)
    print(f"Saved plot to {out_png}")

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <laufzeit_*.tsv>", file=sys.stderr)
        sys.exit(1)
    main(sys.argv[1])
