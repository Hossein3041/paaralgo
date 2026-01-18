#!/usr/bin/env python3

import matplotlib.pyplot as plt
import pandas as pd
import sys
import os

def parse_filename(name):
    parts = name.split('_')
    if len(parts) == 5:
        _, modus_part1, modus_part2, N_str, Pmax_str = parts
        modus = f"{modus_part1}({modus_part2})"
    elif len(parts) == 4:
        _, modus_full, N_str, Pmax_str = parts
        modus = modus_full
    else:
        print("[FEHLER] Dateiname hat nicht das erwartete Format: laufzeit_<MODUS>_<N>_<Pmax>.tsv")
        sys.exit(1)
    return modus, N_str, Pmax_str

def plot_bereich(df, x_min, x_max, filename_suffix, title_suffix, N_str, Pmax_str, modus, name, use_log_x, column, ylabel, title_prefix, ylimit=None):
    df_filtered = df[(df['P'] >= x_min) & (df['P'] <= x_max)]
    if df_filtered.empty:
        print(f"[WARNUNG] Keine Daten für Bereich P={x_min} bis {x_max}.")
        return

    plt.figure(figsize=(10, 5))
    plt.plot(df_filtered['P'], df_filtered[column], marker='o',
             label=f"(N={N_str}, Pmax={Pmax_str}, Modus={modus})")

    if use_log_x:
        plt.xscale('log', base=2)

    plt.xticks(df_filtered['P'], df_filtered['P'])
    plt.xlabel('Anzahl Threads (P)')
    plt.ylabel(ylabel)
    plt.title(f"{title_prefix} {title_suffix}")
    if ylimit:
        plt.ylim(*ylimit)
    plt.grid(True, which='both', ls='--', lw=0.5)
    plt.legend()
    plt.tight_layout()

    out_png = f"{name}_{filename_suffix}.png"
    plt.savefig(out_png)
    print(f"✅ Saved plot to {out_png}")

def main(tsv_path, mode):
    df = pd.read_csv(tsv_path, sep='\t')
    base = os.path.basename(tsv_path)
    name, _ = os.path.splitext(base)
    if mode != 'laufzeit':
        name = name.replace('laufzeit', mode)
    modus, N_str, Pmax_str = parse_filename(name)

    if 1 not in df['P'].values:
        print("Fehler: kein P=1 in der Datei vorhanden!", file=sys.stderr)
        sys.exit(1)

    t1 = df.loc[df['P'] == 1, 'Laufzeit_ms'].iloc[0]
    df['Speedup'] = t1 / df['Laufzeit_ms']
    if mode == 'effizienz':
        df['Efficiency'] = df['Speedup'] / df['P']

    if mode == 'laufzeit':
        col, ylabel, prefix, ylim = 'Laufzeit_ms', 'Laufzeit (ms)', 'Laufzeit vs Threads', None
    elif mode == 'beschleunigung':
        col, ylabel, prefix, ylim = 'Speedup', 'Beschleunigung $s(P) = t(1) / t(P)$', 'Beschleunigung vs Threads', None
    elif mode == 'effizienz':
        col, ylabel, prefix, ylim = 'Efficiency', 'Effizienz $e(P)=s(P)/P$', 'Effizienz vs Threads', (0, 1.05)
    else:
        print("[FEHLER] Unbekannter Modus")
        sys.exit(1)

    plot_bereich(df, 1, 25, 'bis_25', "(P=1 bis 25)", N_str, Pmax_str, modus, name, use_log_x=False, column=col, ylabel=ylabel, title_prefix=prefix, ylimit=ylim)
    plot_bereich(df, 25, 1024, 'bis_1024', "(P=25 bis 1024)", N_str, Pmax_str, modus, name, use_log_x=True, column=col, ylabel=ylabel, title_prefix=prefix, ylimit=ylim)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <laufzeit_*.tsv> <modus>", file=sys.stderr)
        print("Modus: laufzeit | beschleunigung | effizienz")
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
