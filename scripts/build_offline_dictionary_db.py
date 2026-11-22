#!/usr/bin/env python3
"""
Builds the pre-packaged offline SQLite dictionary database for Redeus Dictionary:
  - 6 Essential English Books (Books 1-6, A1-C1, 3,600 words + 26 Life Topics)
  - 3 IELTS Books (Books 7-9: Barron's IELTS, 1200 IELTS Listening, IELTS Vocabulary Bank, 5,991 words)
  - General English -> Arabic Dictionary (87,422 entries)

Output: app/src/main/assets/databases/sara_dictionary.db
"""

import json
import os
import re
import sqlite3
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
OUT_DB = REPO_ROOT / "app" / "src" / "main" / "assets" / "databases" / "sara_dictionary.db"

DEFAULT_GENERAL_ENGLISH_JS = Path("/home/antigravity/ahmdmusa/weSchoolzagstudentHub/lib/generalEnglishData.js")
DEFAULT_SARA_CONTENT_JSON = Path("/home/antigravity/ahmdmusa/SaraEnglishApp/resources/content.json")
DEFAULT_SARA_TOPICS_JSON = Path("/home/antigravity/ahmdmusa/SaraEnglishApp/data/topics.json")
DEFAULT_SARA_GENERAL_DICT_JSON = Path("/home/antigravity/ahmdmusa/SaraEnglishApp/config/general_dictionary.json")


def load_books_from_js(js_path: Path):
    """Evaluate generalEnglishData.js via Node to get the exact BOOKS array as JSON."""
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


def normalize_pos(raw_pos):
    if not raw_pos:
        return ""
    p = str(raw_pos).strip()
    if not p or p.lower() == "vocabulary":
        return ""
    mapping = {
        "adj": "adjective",
        "adv": "adverb",
        "n": "noun",
        "v": "verb",
        "prep": "preposition",
        "conj": "conjunction",
        "pron": "pronoun",
        "interj": "interjection",
    }
    low = p.lower()
    return mapping.get(low, low)


def normalize_ipa(raw_ipa):
    if not raw_ipa:
        return ""
    s = str(raw_ipa).strip()
    if not s:
        return ""
    if not s.startswith("/"):
        s = "/" + s
    if not s.endswith("/"):
        s = s + "/"
    return s


def short_tag_for_book(book_num: int, title: str) -> str:
    if book_num <= 6:
        return f"Book {book_num}"
    if book_num == 7:
        return "Barron's IELTS"
    if book_num == 8:
        return "IELTS Listening"
    if book_num == 9:
        return "IELTS Bank"
    return title


def display_title_for_book(book_num: int, title: str) -> str:
    if book_num <= 6:
        return f"Essential English — Book {book_num}"
    return title.strip()


def build_db():
    books = load_books_from_js(DEFAULT_GENERAL_ENGLISH_JS)
    with open(DEFAULT_SARA_CONTENT_JSON, "r", encoding="utf-8") as f:
        sara_content = json.load(f)
    with open(DEFAULT_SARA_TOPICS_JSON, "r", encoding="utf-8") as f:
        topics_list = json.load(f)
    with open(DEFAULT_SARA_GENERAL_DICT_JSON, "r", encoding="utf-8") as f:
        general_dict = json.load(f)

    # Map word id and lowercase headword -> topic_id & IPA from SaraEnglishApp content.json
    topic_by_id = {}
    topic_by_word = {}
    ipa_by_word = {}
    pos_by_word = {}

    for w in sara_content.get("words", []):
        wid = str(w.get("id", "")).strip()
        hw = str(w.get("headword", "")).strip().lower()
        tid = str(w.get("topic_id", "") or "").strip()
        if wid and tid:
            topic_by_id[wid] = tid
        if hw and tid and hw not in topic_by_word:
            topic_by_word[hw] = tid
        ipa = normalize_ipa(w.get("ipa"))
        if hw and ipa and hw not in ipa_by_word:
            ipa_by_word[hw] = ipa
        pos = normalize_pos(w.get("pos"))
        if hw and pos and hw not in pos_by_word:
            pos_by_word[hw] = pos

    # Also harvest IPA & POS from all 9 books so Books 8 & 9 can inherit known IPA
    for b in books:
        for u in b.get("units", []):
            for w in u.get("words", []):
                hw = str(w.get("word", "")).strip().lower()
                ipa = normalize_ipa(w.get("ipa"))
                pos = normalize_pos(w.get("pos"))
                if hw and ipa and hw not in ipa_by_word:
                    ipa_by_word[hw] = ipa
                if hw and pos and hw not in pos_by_word:
                    pos_by_word[hw] = pos

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

        CREATE TABLE books (
            book_num INTEGER PRIMARY KEY NOT NULL,
            title TEXT NOT NULL,
            short_tag TEXT NOT NULL,
            cefr TEXT NOT NULL,
            category TEXT NOT NULL,
            unit_count INTEGER NOT NULL,
            word_count INTEGER NOT NULL
        );

        CREATE TABLE units (
            id TEXT PRIMARY KEY NOT NULL,
            book_num INTEGER NOT NULL,
            unit_num INTEGER NOT NULL,
            title TEXT NOT NULL,
            word_count INTEGER NOT NULL
        );

        CREATE TABLE topics (
            id TEXT PRIMARY KEY NOT NULL,
            name_en TEXT NOT NULL,
            name_ar TEXT NOT NULL,
            word_count INTEGER NOT NULL
        );

        CREATE TABLE curriculum_words (
            id TEXT PRIMARY KEY NOT NULL,
            word TEXT NOT NULL,
            word_lower TEXT NOT NULL,
            ipa TEXT NOT NULL,
            pos TEXT NOT NULL,
            definition_en TEXT NOT NULL,
            translation_ar TEXT NOT NULL,
            example_en TEXT NOT NULL,
            book_num INTEGER NOT NULL,
            unit_num INTEGER NOT NULL,
            topic_id TEXT NOT NULL,
            word_order INTEGER NOT NULL,
            cefr TEXT NOT NULL,
            category TEXT NOT NULL
        );

        CREATE TABLE general_dict (
            word_lower TEXT PRIMARY KEY NOT NULL,
            translation_ar TEXT NOT NULL
        );
        """
    )

    topic_counts = {}
    total_curriculum_words = 0

    for b in books:
        book_num = int(b["bookNum"])
        raw_title = str(b.get("title", f"Book {book_num}")).strip()
        title = display_title_for_book(book_num, raw_title)
        short_tag = short_tag_for_book(book_num, raw_title)
        cefr = str(b.get("cefr", "")).strip()
        category = "essential" if book_num <= 6 else "ielts"
        units = b.get("units", [])
        book_word_count = 0

        for u in units:
            unit_num = int(u["unitNum"])
            unit_title = str(u.get("title", f"Unit {unit_num}")).strip()
            unit_id = f"{book_num}-{unit_num}"
            words = u.get("words", [])

            cur.execute(
                "INSERT INTO units (id, book_num, unit_num, title, word_count) VALUES (?, ?, ?, ?, ?)",
                (unit_id, book_num, unit_num, unit_title, len(words)),
            )

            for order_idx, w in enumerate(words, start=1):
                wid = str(w.get("id", f"{book_num}-{unit_num}-{order_idx}")).strip()
                word_text = str(w.get("word", "")).strip()
                if not word_text:
                    continue
                word_lower = word_text.lower()
                ipa = normalize_ipa(w.get("ipa")) or ipa_by_word.get(word_lower, "")
                pos = normalize_pos(w.get("pos")) or pos_by_word.get(word_lower, "")
                definition_en = str(w.get("enDef", "") or "").strip()
                translation_ar = str(w.get("arDef", "") or "").strip()
                if not translation_ar and word_lower in general_dict:
                    translation_ar = str(general_dict[word_lower]).strip()
                example_en = str(w.get("enExample", "") or "").strip()

                topic_id = ""
                if book_num <= 6:
                    topic_id = topic_by_id.get(wid) or topic_by_word.get(word_lower, "")
                    if topic_id:
                        topic_counts[topic_id] = topic_counts.get(topic_id, 0) + 1

                cur.execute(
                    """
                    INSERT OR REPLACE INTO curriculum_words (
                        id, word, word_lower, ipa, pos, definition_en, translation_ar,
                        example_en, book_num, unit_num, topic_id, word_order, cefr, category
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        wid,
                        word_text,
                        word_lower,
                        ipa,
                        pos,
                        definition_en,
                        translation_ar,
                        example_en,
                        book_num,
                        unit_num,
                        topic_id,
                        order_idx,
                        cefr,
                        category,
                    ),
                )
                book_word_count += 1
                total_curriculum_words += 1

        cur.execute(
            """
            INSERT INTO books (book_num, title, short_tag, cefr, category, unit_count, word_count)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (book_num, title, short_tag, cefr, category, len(units), book_word_count),
        )

    for t in topics_list:
        tid = str(t["id"]).strip()
        cur.execute(
            "INSERT INTO topics (id, name_en, name_ar, word_count) VALUES (?, ?, ?, ?)",
            (tid, str(t["name_en"]).strip(), str(t["name_ar"]).strip(), topic_counts.get(tid, 0)),
        )

    dict_rows = []
    for k, v in general_dict.items():
        kl = str(k).strip().lower()
        vt = str(v).strip()
        if kl and vt:
            dict_rows.append((kl, vt))

    cur.executemany(
        "INSERT OR REPLACE INTO general_dict (word_lower, translation_ar) VALUES (?, ?)",
        dict_rows,
    )

    # Create indexes for fast offline search and Anki course browsing
    cur.executescript(
        """
        CREATE INDEX idx_cw_word_lower ON curriculum_words(word_lower);
        CREATE INDEX idx_cw_book_unit ON curriculum_words(book_num, unit_num, word_order);
        CREATE INDEX idx_cw_topic ON curriculum_words(topic_id, book_num, word_order);
        CREATE INDEX idx_cw_category ON curriculum_words(category);
        CREATE INDEX idx_units_book ON units(book_num, unit_num);
        """
    )

    meta_entries = [
        ("db_version", "1"),
        ("total_books", str(len(books))),
        ("total_curriculum_words", str(total_curriculum_words)),
        ("total_topics", str(len(topics_list))),
        ("total_general_dict_words", str(len(dict_rows))),
        ("generated_at", datetime.now(timezone.utc).isoformat()),
    ]
    cur.executemany("INSERT INTO meta (key, value) VALUES (?, ?)", meta_entries)

    conn.commit()
    cur.execute("VACUUM")
    conn.commit()
    conn.close()

    size_mb = OUT_DB.stat().st_size / (1024 * 1024)
    print(f"Built {OUT_DB} ({size_mb:.2f} MB)")
    print(f"  Books: {len(books)}")
    print(f"  Curriculum words: {total_curriculum_words}")
    print(f"  Topics: {len(topics_list)}")
    print(f"  General dictionary entries: {len(dict_rows)}")


if __name__ == "__main__":
    build_db()
