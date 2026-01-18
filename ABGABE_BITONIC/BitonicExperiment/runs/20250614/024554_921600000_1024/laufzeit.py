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
    title = f"Laufzeit vs Threads (N={N_str}, Pmax={Pmax_str})"

    x = df['P']
    y = df['Laufzeit_ms']

    plt.figure()
    plt.plot(x, y, marker='o')
    plt.xscale('log', base=2)
    plt.xticks(x, x)
    plt.xlabel('Anzahl Threads (P)')
    plt.ylabel('Laufzeit (ms)')
    plt.title(title)
    plt.grid(True, which='both', ls='--', lw=0.5)

    out_png = name + '.png'
    plt.tight_layout()
    plt.savefig(out_png)
    print(f"Saved plot to {out_png}")

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"Usage: {sys.args[0]} <laufzeit_*.tsv>", file=sys_stderr)
        sys.exit(1)
    main(sys.argv[1])