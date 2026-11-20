# Redeus Dictionary

A native Android English dictionary and vocabulary-learning app built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Room**, and **Retrofit**.

## Features

- **AI-Powered Word Lookup:** Rich definitions, US/UK phonetics, parts of speech, usage examples, synonyms/antonyms, mnemonics, and AI or Pexels visual illustrations via Gemini (with OpenRouter fallback).
- **Pronunciation (TTS):** Online Gemini AI voice or built-in Android TTS with US/UK accent and Normal/Slow speed controls.
- **Personal Vocabulary Library:** Bookmarked deck, search history, and advanced filtering by SRS status, part of speech, and custom tags/decks.
- **Spaced-Repetition Smart Review:** Supports both **FSRS** (with configurable target retention) and classic **SM-2**, leech detection, and detailed 30-day/all-time analytics.
- **Practice Modes:** Flip flashcards, multiple-choice quizzes, and audio dictation/spelling tests.
- **Daily Reading:** CEFR-leveled (A1–C1) AI reading passages with comprehension questions and grammar checks.
- **Sentence Mining:** Save context sentences from dictionary lookups or daily media and practice them in Cloze deletion mode.
- **Habit Tracking & Reminders:** 60-day activity heatmap, daily habit goal, and scheduled study reminder notifications.

## Material Design 3 Architecture

- **Color System:** Semantic M3 Light & Dark tonal palettes seeded from Scholarly Indigo (`#3B5BA9`) with Warm Ochre tertiary accents (`#B36B00` / `#F2B84B`), full `surfaceContainer*` hierarchy, Android 12+ **Dynamic Color** (Material You), and selectable theme presets.
- **Typography & Shapes:** Full M3 type scale with selective M3 Expressive focal styling (headwords, streaks, scores), standard M3 shape tokens (`4 / 8 / 12 / 16 / 28dp`), and tonal containment.
- **Adaptive Layouts:** `WindowWidthSizeClass`-driven navigation (`NavigationBar` on compact, `NavigationRail` on medium/expanded) with two-pane layouts on expanded screens (Search, Library list-detail, Daily Reading, Settings).
- **Accessibility & RTL:** `android:supportsRtl="true"`, Arabic/large-font-safe layouts, minimum `48dp` touch targets, accessible labels, and reduced-motion (`ANIMATOR_DURATION_SCALE`) support.

## Setup & Running Locally

### Prerequisites
- Android Studio (or JDK 21 + Android SDK API 36)
- Optional: API keys from [Google AI Studio](https://aistudio.google.com/app/apikey), [OpenRouter](https://openrouter.ai/keys), and [Pexels](https://www.pexels.com/api/) (you can also configure keys in-app under **Settings → Preferences**).

### Configuration
1. Copy `.env.example` to `.env` in the project root:
   ```bash
   cp .env.example .env
   ```
2. Fill in your local keys in `.env` (or leave placeholder values if you prefer configuring keys inside the app's Settings screen).
3. Ensure `local.properties` points to your Android SDK (`sdk.dir=...`). Android Studio creates this automatically on sync.

### Build & Run
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

To install on a connected Android device:
```bash
.\gradlew.bat installDebug
```

## Security Notice

> **Important:** This project uses the Secrets Gradle Plugin to inject values from `.env` into `BuildConfig` at compile time. Because compiled Android APKs can be decompiled, **never ship or distribute release APKs built with real API keys in `.env`**. For public builds, keep `.env` empty or use placeholder values and let users supply their own keys in Settings (or route requests through a backend proxy).

## License

This project is licensed under the [MIT License](LICENSE).
