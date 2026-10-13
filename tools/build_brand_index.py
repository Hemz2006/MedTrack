"""Builds app/src/main/assets/brands.db from the A-Z Medicine Dataset of India CSV.

Usage: python -I tools/build_brand_index.py <path to A_Z_medicines_dataset_of_India.csv>

The output is git-ignored: the dataset's licence is unstated, so it stays on this machine.
"""
import csv
import os
import re
import sqlite3
import sys

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "brands.db")
STRENGTH = re.compile(r"^(.*\S)\s*\(([^()]*)\)\s*$")


def ingredient(text):
    """'Amoxycillin  (500mg) ' -> 'Amoxycillin (500mg)'; keeps aliases like 'Vitamin B6 (Pyridoxine)'."""
    text = " ".join(text.split())
    if not text:
        return None
    m = STRENGTH.match(text)
    return f"{m[1]} ({m[2]})" if m else text


def key(name):
    return re.sub(r"[^a-z0-9]", "", name.lower())


def main(src):
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)
    seen = set()
    rows = []
    with open(src, encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f):
            name = " ".join(r["name"].split())
            parts = [p for p in (ingredient(r["short_composition1"]), ingredient(r["short_composition2"])) if p]
            if not name or not parts or not key(name):
                continue
            composition = " + ".join(parts)
            dedupe = (name.lower(), composition.lower())
            if dedupe in seen:
                continue
            seen.add(dedupe)
            maker = " ".join(r["manufacturer_name"].split()) or None
            rows.append((key(name), name, composition, maker, 1 if r["Is_discontinued"].strip().upper() == "TRUE" else 0))
    compositions = {c: i for i, c in enumerate(sorted({r[2] for r in rows}))}
    makers = {m: i for i, m in enumerate(sorted({r[3] for r in rows if r[3]}))}
    db = sqlite3.connect(OUT)
    db.execute("CREATE TABLE compositions (id INTEGER PRIMARY KEY, text TEXT NOT NULL)")
    db.execute("CREATE TABLE makers (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
    # Clustered by search key, so a prefix search needs no separate index.
    db.execute(
        "CREATE TABLE brands (key TEXT NOT NULL, name TEXT NOT NULL, composition INTEGER NOT NULL, maker INTEGER, "
        "discontinued INTEGER NOT NULL, PRIMARY KEY (key, name, composition)) WITHOUT ROWID"
    )
    db.executemany("INSERT INTO compositions VALUES (?, ?)", [(i, c) for c, i in compositions.items()])
    db.executemany("INSERT INTO makers VALUES (?, ?)", [(i, m) for m, i in makers.items()])
    db.executemany(
        "INSERT OR IGNORE INTO brands VALUES (?, ?, ?, ?, ?)",
        [(k, n, compositions[c], makers.get(m), d) for k, n, c, m, d in rows],
    )
    db.execute("PRAGMA user_version = 1")
    db.commit()
    db.execute("VACUUM")
    db.close()
    print(f"{len(rows)} brands -> {os.path.normpath(OUT)} ({os.path.getsize(OUT) // 1024} KB)")


if __name__ == "__main__":
    main(sys.argv[1])
