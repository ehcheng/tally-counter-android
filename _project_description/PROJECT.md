# Tally — Android Tally Counter

> A native Android tally counter inspired by [Taptic: Tally Counter](https://apps.apple.com/us/app/taptic-tally-counter/id1543467427) (iOS). Built with Kotlin, Jetpack Compose, and Material 3.

**Icon:** Tally the Turtle — a friendly green turtle with an abacus shell on a teal background.

---

## 1. What This App Does

Tally is a multi-counter tally app. Users create named activities (e.g. "Push-ups", "Glasses of water", "Inventory count"), tap to increment/decrement, and review per-day history with charts. It is fast, tactile, and visually satisfying.

---

## 2. Design Language

### Visual Identity
- **OLED-first dark theme** — pure black background (`#000000`), dark charcoal cards (`#1A1A1A`), elevated cards (`#222222`)
- **Per-counter color theming** — each counter gets its own accent color from a curated palette (12+ colors)
- **Left accent stripe** — 4dp colored bar on each counter card's left edge
- **Counter-colored buttons** — +1 uses full counter color; -1 uses counter color at 15% alpha
- **Gradient headers** — detail screen uses a gradient from counter color to dark background

### Animations & Feedback
- **Particle burst** on increment — colored particles explode from the +1 button using `Choreographer.FrameCallback` + `drawWithContent` (works reliably on Samsung and all devices)
- **Floating "+N" text** — rises 180dp above the tap point, 900ms duration (outlasts particle fade at 550ms), 26sp scaling to 44sp
- **Count bump animation** — Spring animation (damping 0.4, stiffness 800) scaling to 1.12x on increment
- **Percussive tick sound** — 18ms, layered 4800/6200/2400/8500Hz frequencies, noise burst attack, 1.6KB WAV via SoundPool
- **Haptic feedback** on every tap

### Layout
- **Bottom-anchored cards** — dynamic spacer positions counters around the lower 1/3 of the screen for thumb reach (spacer scales: 1 counter = 50%, 2 = 40%, 3 = 33%, 4 = 20%, 5+ = 10%)
- **56dp tap targets** — well above minimum recommended size for one-handed use
- **Long-press context menu** — Edit/Delete via `combinedClickable` + `DropdownMenu`
- **No FAB** — add button in toolbar

---

## 3. Feature Set

### Counter Management
- Create, edit, rename, reorder, and delete counters
- Per-counter: name, emoji icon, accent color (12+ palette), configurable step value, starting count, start date
- Start date for migration — set when you started tracking in another app, so averages calculate correctly

### Counting Interface
- Large +/- buttons with counter's accent color
- Today's count and all-time total displayed on home cards
- Number formatting with locale-aware commas (e.g. "5,010")
- Counter value never goes below zero

### History & Statistics
- **Stats ribbon**: Per Day / Per Week / Per Month / Per Year rates
  - Rate = totalCount ÷ daysSinceStartDate (capped at total — can never exceed it)
  - totalCount = startingCount + sum(all entries)
- **Mini stat cards**: Min | Today | Max (daily entry values only — excludes starting count)
- **Chart**: Daily counts via Vico (7d / 30d / 90d / All views)
- **History list**: Scrollable daily totals with running total, tap to edit individual entries
- **Bottom bar**: Total count prominently displayed with +/- buttons

### Data & Backup
- **Auto-backup** to `Downloads/tally-auto-backups/` (survives uninstall) + internal backups (last 7)
- **CSV export**: all counter history via system save dialog
- **JSON backup/restore**: full app state export/import
- **Restore from file**: file picker → confirmation dialog → replaces all data
- `android:allowBackup="true"` for Google Drive backup

### Settings
- Sound on/off toggle
- Haptics on/off toggle
- Theme: System / Light / Dark
- Export CSV / Backup JSON / Restore
- Version label visible without scrolling

---

## 4. Tech Stack

| Layer | Choice |
|---|---|
| Language | **Kotlin 2.1.0** |
| UI | **Jetpack Compose** (BOM 2024.02.00, Material 3) |
| Local DB | **Room** (SQLite, KSP annotation processing) |
| Charts | **Vico** (`compose-m3:2.0.1`) |
| Sound | `SoundPool` API |
| Haptics | `View.performHapticFeedback()` |
| Architecture | MVVM — ViewModel + StateFlow + Repository |
| Animation | `Choreographer.FrameCallback` + `drawWithContent` for particles |
| Min SDK | **26** (Android 8.0, ~95% coverage) |
| Target SDK | **35** |
| Build | Gradle 8.11.1 (Kotlin DSL), AGP 8.7.3 |

---

## 5. Data Model

```
Counter
├── id: Long (PK, auto-gen)
├── name: String
├── icon: String (emoji)
├── colorHex: String (e.g. "#FF6B6B")
├── stepValue: Int (default 1)
├── startingCount: Int (default 0, for migration)
├── startDate: String? (ISO "YYYY-MM-DD", optional)
├── sortOrder: Int
├── createdAt: Long (epoch ms)

CounterEntry
├── id: Long (PK, auto-gen)
├── counterId: Long (FK → Counter.id)
├── date: String (ISO "YYYY-MM-DD", local tz)
├── count: Int
├── UNIQUE(counterId, date)
```

**Total calculation:** `total = startingCount + sum(entries)`
**Upsert on tap:** If entry exists for today, `count += stepValue`; else insert with `count = stepValue`.

---

## 6. Screen Map

### Home Screen
- Top bar: "Tally" title, add-counter button, settings gear
- Scrollable list of counter cards, bottom-anchored with dynamic spacer
- Each card: emoji icon, name, subtitle ("±1 · Today: X"), -1 button, count badge, +1 button
- Left accent stripe in counter's color
- Long-press → Edit / Delete context menu

### Counter Detail Screen
- Gradient header with counter name and total
- Stats ribbon (Per Day/Week/Month/Year)
- Chart with time range selector (7d/30d/90d/All)
- Mini stats: Min | Today | Max
- Scrollable history with running totals, tap to edit
- Bottom bar: -1 button, total count, +1 button

### Add / Edit Counter Screen
- Fields: Name, Starting Count, Start Date (optional YYYY-MM-DD), Icon (emoji picker), Color (palette), Step Value
- Dark background consistent with theme

### Settings Screen
- Sound, haptics, theme toggles
- Export/backup/restore actions
- Version label above fold

---

## 7. Out of Scope

- Widgets, Wear OS, cloud sync, accounts, IAP, ads
- Reminders/notifications, counter groups, decimal values
- Shortcuts/intents

---

## 8. Package & Build

- **Package:** `com.echeng.tally.app`
- **Build:** `./gradlew assembleDebug`
- **APK:** `app/build/outputs/apk/debug/app-debug.apk` (~18MB)
- **DB version:** 3 (migrations: v1→v2 adds startingCount, v2→v3 adds startDate)
