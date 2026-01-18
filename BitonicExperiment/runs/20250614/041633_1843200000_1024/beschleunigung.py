#!/usr/bin/env python3
import sys
import os
import matplotlib.pyplot as plt
import pandas as pd

def main(tsv_path):
    df = pd.read_csv(tsv_path, sep='\t')
    base = os.path.basename(tsv_path)
    name, _ = os.path.splitext(base)
    
    try:
        _, N_str, Pmax_str = name.split('_')
    except ValueError:
        N_str, Pmax_str = '', ''
    
    if 1 not in df['P'].values:
        print("Fehler: kein P=1 in der Datei vorhanden!", file=sys.stderr)
        sys.exit(1)
    t1 = df.loc[df['P'] == 1, 'Laufzeit_ms'].iloc[0]

    df['Speedup'] = t1 / df['Laufzeit_ms']

    plt.figure()
    plt.plot(df['P'], df['Speedup'], marker='o')
    plt.xscale('log', base=2)
    plt.xticks(df['P'], df['P'])
    plt.xlabel('Anzahl Threads (P)')
    plt.ylabel('Beschleunigung $s(P)=t(1)/t(P)$')
    plt.title(f"Beschleunigung vs Threads (N={N_str}, Pmax={Pmax_str})")
    plt.grid(True, which='both', ls='--', lw=0.5)

    # Ausgabe-Pfad: beschleunigung_<N>_<Pmax>.png
    out_png = f"beschleunigung_{N_str}_{Pmax_str}.png"
    plt.tight_layout()
    plt.savefig(out_png)
    print(f"Saved plot to {out_png}")

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <laufzeit_*.tsv>", file=sys.stderr)
        sys.exit(1)
    main(sys.argv[1])