#!/usr/bin/env python3
"""
Builds the pre-packaged offline WebP image database for Redeus Dictionary:
  - Reads all 3,600 curated vocabulary illustrations for Essential English Books 1-6
    from weSchoolzagstudentHub/public/vocab/images/{1..6} mapped via generalEnglishData.js
  - Compresses each image to crisp WebP (max width 320px, quality 60)
  - Stores in a single indexed SQLite database:
      app/src/main/assets/databases/word_images.db
      Table: images(word_lower TEXT PRIMARY KEY, webp BLOB, width INTEGER, height INTEGER)

Because lookup is keyed by word_lower, all 3,600 Essential Book words AND ~2,545
overlapping IELTS / 87K Dictionary words share these local offline illustrations
automatically with O(1) lookup and zero network calls.
"""

import io
import json
import sqlite3
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_DB = REPO_ROOT / "app" / "src" / "main" / "assets" / "databases" / "word_images.db"

DEFAULT_GENERAL_ENGLISH_JS = Path("/home/antigravity/ahmdmusa/weSchoolzagstudentHub/lib/generalEnglishData.js")
DEFAULT_IMAGES_ROOT = Path("/home/antigravity/ahmdmusa/weSchoolzagstudentHub/public")

MAX_WIDTH = 320
WEBP_QUALITY = 58


def load_books_from_js(js_path: Path):
    node_code = (
        f"const fs = require('fs');"
        f"const src = fs.readFileSync({json.dumps(str(js_path))}, 'utf8');"
        f"const m = src.replace(/^export\\s+const\\s+/gm, 'const ') + '\\nmodule.exports = {{ BOOKS }};';"
        f"const mod = {{}};"
        f"new Function('module', 'exports', m)(mod, {{}});"
        f"process.stdout.write(JSON.stringify(mod.exports.BOOKS));"
    )
    res = subprocess.run(["node", "-e", node_code], capture_output=True, text=True, check=True)
    return json.loads(res.stdout)


def compress_to_webp(src_path: Path):
    with Image.open(src_path) as im:
        rgb = im.convert("RGB")
        if rgb.width > MAX_WIDTH:
            new_h = max(1, int(round(rgb.height * MAX_WIDTH / rgb.width)))
            rgb = rgb.resize((MAX_WIDTH, new_h), Image.Resampling.LANCZOS)
        buf = io.BytesIO()
        rgb.save(buf, format="WEBP", quality=WEBP_QUALITY, method=5)
        return buf.getvalue(), rgb.width, rgb.height


def build_images_db():
    books = load_books_from_js(DEFAULT_GENERAL_ENGLISH_JS)

    word_to_img = {}
    for b in books:
        for u in b.get("units", []):
            for w in u.get("words", []):
                word_lower = str(w.get("word", "")).strip().lower()
                rel_img = str(w.get("image", "") or "").strip()
                if not word_lower or not rel_img:
                    continue
                if word_lower in word_to_img:
                    continue
                full_path = DEFAULT_IMAGES_ROOT / rel_img.lstrip("/")
                if full_path.is_file():
                    word_to_img[word_lower] = full_path

    OUT_DB.parent.mkdir(parents=True, exist_ok=True)
    if OUT_DB.exists():
        OUT_DB.unlink()

    conn = sqlite3.connect(str(OUT_DB))
    cur = conn.cursor()
    cur.executescript(
        """
        PRAGMA journal_mode = OFF;
        PRAGMA synchronous = OFF;
        PRAGMA page_size = 4096;

        CREATE TABLE meta (
            key TEXT PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        );

        CREATE TABLE images (
            word_lower TEXT PRIMARY KEY NOT NULL,
            webp BLOB NOT NULL,
            width INTEGER NOT NULL,
            height INTEGER NOT NULL
        );
        """
    )

    inserted = 0
    failed = 0
    total_raw_webp = 0
    for word_lower, path in sorted(word_to_img.items()):
        try:
            webp_bytes, w, h = compress_to_webp(path)
            cur.execute(
                "INSERT OR REPLACE INTO images (word_lower, webp, width, height) VALUES (?, ?, ?, ?)",
                (word_lower, sqlite3.Binary(webp_bytes), w, h),
            )
            inserted += 1
            total_raw_webp += len(webp_bytes)
        except Exception as e:
            failed += 1
            print(f"WARN: failed to process {word_lower} ({path}): {e}")

    meta_rows = [
        ("db_version", "1"),
        ("total_images", str(inserted)),
        ("max_width", str(MAX_WIDTH)),
        ("webp_quality", str(WEBP_QUALITY)),
        ("generated_at", datetime.now(timezone.utc).isoformat()),
    ]
    cur.executemany("INSERT INTO meta (key, value) VALUES (?, ?)", meta_rows)
    conn.commit()
    cur.execute("VACUUM")
    conn.commit()
    conn.close()

    size_mb = OUT_DB.stat().st_size / (1024 * 1024)
    print(f"Built {OUT_DB} ({size_mb:.2f} MB)")
    print(f"  Total bundled images: {inserted} (failed: {failed}, avg {total_raw_webp/max(1,inserted)/1024:.1f} KB)")


if __name__ == "__main__":
    build_images_db()
