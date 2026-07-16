#!/usr/bin/env python3
"""
Convert chapters.json into a prebuilt SQLite database (bible.db) that Room can
seed from via `createFromAsset("bible.db")`, and drop it into the tool's assets/.

Usage:
    python3 scripts/build_bible_db.py                # uses default paths
    python3 scripts/build_bible_db.py in.json out.db

Room compatibility notes (why this works without any identity-hash dance):
  * We set `PRAGMA user_version = 1` to match `@Database(version = 1)`. If this
    is left at 0, Room thinks a migration is needed and will crash or wipe data.
  * We do NOT create Room's `room_master_table`. When it's absent, Room creates
    it and stamps its own identity hash on first open — no schema validation
    error. (If you ever bump the Room @Database version, bump user_version too.)
  * Column names/types below must stay in sync with the @Entity in the app.
"""

import json
import re
import sqlite3
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
IN_PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else REPO_ROOT / "chapters.json"
OUT_PATH = (
    Path(sys.argv[2])
    if len(sys.argv) > 2
    else REPO_ROOT / "tool" / "src" / "main" / "assets" / "bible.db"
)

VERSE_ID_RE = re.compile(r"^([A-Z0-9]+)\.(\d+)\.(\d+)$")


def main() -> None:
    chapters = json.loads(IN_PATH.read_text(encoding="utf-8"))
    print(f"Loaded {len(chapters)} chapters from {IN_PATH}")

    # Assign each book a canonical number (1-66) from its first appearance.
    # chapters.json is already in canonical order, so this is stable.
    book_numbers: dict[str, int] = {}
    rows = []
    for chapter in chapters:
        book_id = chapter["chapterId"].split(".")[0]
        if book_id not in book_numbers:
            book_numbers[book_id] = len(book_numbers) + 1
        book_no = book_numbers[book_id]
        book_name = chapter["bookName"]

        for v in chapter["verses"]:
            m = VERSE_ID_RE.match(v["verseId"])
            if not m:
                raise ValueError(f"Unexpected verseId: {v['verseId']!r}")
            _, chap_no, verse_no = m.groups()
            chap_no, verse_no = int(chap_no), int(verse_no)
            # Stable, unique id derived from the reference (book<=66, chap<~150, verse<~176).
            row_id = book_no * 1_000_000 + chap_no * 1_000 + verse_no
            rows.append(
                (
                    row_id,
                    book_no,
                    book_id,
                    book_name,
                    chap_no,
                    verse_no,
                    v["text"],  # kept verbatim: trailing "\n\n" encodes paragraph breaks
                )
            )

    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    if OUT_PATH.exists():
        OUT_PATH.unlink()

    db = sqlite3.connect(OUT_PATH)
    try:
        db.executescript(
            """
            CREATE TABLE verses (
                id        INTEGER NOT NULL PRIMARY KEY,
                book      INTEGER NOT NULL,
                book_id   TEXT    NOT NULL,
                book_name TEXT    NOT NULL,
                chapter   INTEGER NOT NULL,
                verse     INTEGER NOT NULL,
                text      TEXT    NOT NULL
            );
            CREATE INDEX index_verses_book_chapter ON verses(book, chapter);
            """
        )
        db.executemany(
            "INSERT INTO verses (id, book, book_id, book_name, chapter, verse, text) "
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            rows,
        )
        # Must match @Database(version = 1); see module docstring.
        db.execute("PRAGMA user_version = 1")
        db.commit()
    finally:
        db.close()

    size_mb = OUT_PATH.stat().st_size / 1_000_000
    print(f"Wrote {len(rows)} verses across {len(book_numbers)} books")
    print(f"-> {OUT_PATH} ({size_mb:.1f} MB)")


if __name__ == "__main__":
    main()
