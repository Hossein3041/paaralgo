#!/usr/bin/env python3

import matplotlib.pyplot as plt
import pandas as pd
import sys
import os

def plot_bereich(df, x_min, x_max, filename_suffix, title_suffix, N_str, Pmax_str, modus, name, use_log_x):
    df_filtered = df[(df['P'] >= x_min) & (df['P'] <= x_max)]
    if df_filtered.empty:
        print(f"[WARNUNG] Keine Daten für Bereich P={x_min} bis {x_max}.")
        return

    plt.figure(figsize=(10, 5))
    plt.plot(df_filtered['P'], df_filtered['Laufzeit_ms'], marker='o',
             label=f"(N={N_str}, Pmax={Pmax_str}, Modus={modus})")

    if use_log_x:
        plt.xscale('log', base=2)

    plt.xticks(df_filtered['P'], df_filtered['P'])
    plt.xlabel('Anzahl Threads (P)')
    plt.ylabel('Laufzeit (ms)')
    plt.title(f"Laufzeit vs Threads {title_suffix}")
    plt.grid(True, which='both', ls='--', lw=0.5)
    plt.legend()
    plt.tight_layout()

    out_png = f"{name}_{filename_suffix}.png"
    plt.savefig(out_png)
    print(f"✅ Saved plot to {out_png}")


def main(tsv_path):
    print(f"[DEBUG] Lese TSV-Datei: {tsv_path}")
    df = pd.read_csv(tsv_path, sep='\t')
    print(f"[DEBUG] Gelesene Spalten: {df.columns.tolist()}")

    base = os.path.basename(tsv_path)
    name, _ = os.path.splitext(base)

    try:
        _, modus_part1, modus_part2, N_str, Pmax_str = name.split('_')
        modus = f"{modus_part1}({modus_part2})" if modus_part1 == "ParMergeSort" else f"{modus_part1}_{modus_part2}"
    except ValueError:
        print("[FEHLER] Dateiname hat nicht das erwartete Format: laufzeit_<MODUS>_<N>_<Pmax>.tsv")
        sys.exit(1)

    print(f"[DEBUG] Generiere Plots für N={N_str}, Pmax={Pmax_str}, Modus={modus}")

    # Plot 1: bis P=25 (lineare Achse)
    plot_bereich(
        df=df,
        x_min=1,
        x_max=25,
        filename_suffix='bis_25',
        title_suffix="(P=1 bis 25)",
        N_str=N_str,
        Pmax_str=Pmax_str,
        modus=modus,
        name=name,
        use_log_x=False  # ❗️LINEAR für bessere Lesbarkeit
    )

    # Plot 2: ab P=25 (logarithmisch)
    plot_bereich(
        df=df,
        x_min=25,
        x_max=1024,
        filename_suffix='bis_1024',
        title_suffix="(P=25 bis 1024)",
        N_str=N_str,
        Pmax_str=Pmax_str,
        modus=modus,
        name=name,
        use_log_x=True  # ❗️LOG für Skalierbarkeit
    )

if __name__ == '__main__':
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <laufzeit_*.tsv>", file=sys.stderr)
        sys.exit(1)
    main(sys.argv[1])
