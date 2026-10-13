# MedTrack — Design Spec

Date: 2026-10-13
Status: Draft for review

## 1. Purpose

A personal Android app for one user to log their medications, see what each one is
(active ingredients, chemical formula, what it's used for), keep a photo and personal
notes for each, and get reliable reminders to take them — optionally triggered by meals
logged in Google's Health Connect.

**Success means:** no dose is silently missed, and opening any medication shows at a
glance what it is, what's in it, and how the user takes it.

### Decisions made during brainstorming

| Topic | Decision |
|---|---|
| Platform | Android phone only |
| Stack | Native Kotlin, Jetpack Compose, Room, AlarmManager, Health Connect SDK |
| Users | Single user, no accounts, data on device only |
| Lookup source | Free official APIs by **generic name**: NIH RxNorm, PubChem, MedlinePlus Connect. Brand name is a user-entered label. |
| Schedule types | Daily times, weekdays + times, every N hours, after-meal; all with optional start/end date (courses) |
| Dose actions | Taken / Skip / Snooze, with a recorded history |
| Meal trigger | "After meal" schedule type with delay and a fallback time |

### Out of scope for v1

iOS, cloud sync/accounts, export/import, multiple schedule types per medication,
refill/stock tracking, drug-interaction warnings.

## 2. Screens

1. **Today (home)** — today's doses in time order, each with photo, name, dose, time
   and status (upcoming / taken / skipped / missed). Tapping a dose opens Taken/Skip.
   Shows a warning banner when Health Connect is needed but unavailable, or when a
   required permission is missing.
2. **Medications** — list of all medications with photo thumbnails; active ones first,
   paused ones below. "+" adds a new one.
3. **Add / Edit medication**
   - Generic name with autocomplete suggestions (RxNorm).
   - Looked-up fields (ingredients, formulas, used-for note) fill in as each source
     returns; all editable; each labeled with its source.
   - Optional brand name and dose text (e.g. "Pan 40, 1 tablet").
   - Photo from camera or gallery (one per medication).
   - Free-text notes (e.g. "empty stomach, with water").
   - Schedule editor (see §4) with optional start/end date.
4. **Medication detail** — all fields above plus this medication's dose history;
   actions: edit, pause/resume, refresh info, delete (with confirmation).
5. **Settings** — permission status and grant buttons (notifications, exact alarms,
   full-screen alarms, battery-optimization exclusion); Health Connect connect/disconnect.

## 3. Data model (Room, on device)

**Medication**
- `id`, `genericName`, `brandName?`, `doseText?`, `notes?`
- `ingredients`: list of `{name, strength?, formula?}`
- `usedFor?`
- `lookupStatus` per source: `NOT_RUN | OK | NOT_FOUND | FAILED`; `rxcui?`
- `photoPath?` (file in app-private storage)
- `active` (false = paused)
- `createdAt`

**Schedule** (exactly one per medication)
- `medicationId`
- `type`: `DAILY_TIMES | WEEKDAYS | EVERY_N_HOURS | AFTER_MEAL`
- `times`: list of local times (DAILY_TIMES, WEEKDAYS)
- `weekdays`: set of days (WEEKDAYS)
- `intervalHours` + `anchor` date-time (EVERY_N_HOURS; doses continue across days from the anchor)
- `mealType`: `BREAKFAST | LUNCH | DINNER | ANY` + `delayMinutes` + `fallbackTime` (AFTER_MEAL)
- `startDate`, `endDate?`

**DoseLog**
- `id`, `medicationId`, `dueAt`
- `status`: `PENDING | TAKEN | SKIPPED | MISSED`
- `actedAt?`, `trigger`: `SCHEDULE | MEAL | FALLBACK`

Rules:
- A `PENDING` row is written when a reminder fires.
- A `PENDING` dose older than **2 hours** becomes `MISSED`. This check runs when the
  app opens and whenever alarms are rescheduled.
- Pausing a medication cancels its alarms and keeps its history. Deleting it removes the
  medication, its schedule, its logs and its photo after confirmation.

## 4. Reminders

### Scheduling (`schedule/`, pure Kotlin)
`nextDose(schedule, after: Instant, zone): Instant?` returns the next due time, or null
when the course has ended. Handles all four types, start and end dates, weekday
wraparound, midnight crossing and DST. For AFTER_MEAL it returns the next **fallback**
time; meal-triggered times come from `health/`.

### Alarms (`reminders/`, Android)
- For each active medication, exactly one exact alarm is set, for its next dose
  (`AlarmManager.setExactAndAllowWhileIdle`). When the alarm fires, the next one is booked.
- On fire: write a PENDING DoseLog, then show a high-priority notification with a
  full-screen intent (alarm screen when locked) and sound. It displays the photo, name,
  dose and notes, with **Taken**, **Skip** and **Snooze (10 min)** actions. Snooze can be
  repeated.
- Doses due in the same minute are grouped into one alarm, with per-medication Taken
  buttons and a "Take all" button.
- All alarms are rebuilt on: boot (`BOOT_COMPLETED`), time or time-zone change, app
  update, and any medication or schedule edit.

### After-meal (`health/`)
- A periodic WorkManager job (~15 min, the Android minimum) reads `NutritionRecord`s
  from Health Connect that were logged since the last check.
- A meal matches if its `mealType` equals the schedule's (or the schedule is `ANY`).
- On the first match of the day for a schedule that hasn't been handled yet, the
  reminder is booked at `mealTime + delay`, or fires immediately if that time has passed.
  Further meals that day don't fire it again.
- If no match has arrived by `fallbackTime`, the fallback alarm fires (trigger =
  FALLBACK). Once a meal has triggered the reminder, that day's fallback is cancelled.
- If Health Connect is missing or permission is revoked: only the fallback fires, and the
  Today screen shows a warning.

### Permissions
`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`,
`RECEIVE_BOOT_COMPLETED`; Health Connect `READ_NUTRITION` and
`READ_HEALTH_DATA_IN_BACKGROUND`, requested only when meal reminders are enabled.
A first-run setup flow requests each one and links to the battery-optimization
exclusion (needed on Xiaomi, Oppo, Vivo and Samsung).

## 5. Medication lookup (`lookup/`)

Single entry point: `lookup(genericName): LookupResult` (ingredients, formulas,
used-for note, and a per-source status).

1. **RxNorm** (`rxnav.nlm.nih.gov/REST`): suggestions via `approximateTerm`; resolve
   the chosen name to an `rxcui`; get its ingredients (`related?tty=IN+MIN`) and strengths.
2. **PubChem** (`pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/{ingredient}/property/MolecularFormula/JSON`):
   formula per ingredient.
3. **MedlinePlus Connect** (by `rxcui`, JSON response): a short "used for" summary,
   with HTML stripped and the text trimmed to the first 1–2 sentences.

Behaviour:
- Sources run in order (each needs the previous result); the UI shows each one's
  result as soon as it arrives.
- No API keys and no cost. Results are stored with the medication and refetched only
  when the user taps "Refresh info".
- Offline or a source fails: the result is kept as partial, and a per-field retry is
  offered. Saving is never blocked.
- Name not found: the message reads "Couldn't find this name – check the spelling or
  enter details manually."
- The used-for text is shown with "General information, not medical advice."

## 6. Photos

Camera (via `TakePicture` to a temp file) or gallery (Photo Picker). The image is
resized to a longest side of ~1200 px, saved as JPEG in app-private storage, and the
original isn't referenced. If there's no photo, a coloured placeholder with the
medication's initial is shown.

## 7. Data safety

All data stays on the device. Android Auto Backup is enabled for the database and the
photos directory. If the user has Google backup off, uninstalling the app loses data
(accepted for v1).

## 8. Code structure

| Package | Responsibility | Depends on |
|---|---|---|
| `data/` | Room DB, DAOs, repositories, photo storage | — |
| `lookup/` | RxNorm / PubChem / MedlinePlus clients → `LookupResult` | HTTP client |
| `schedule/` | Pure next-dose calculation | `java.time` only |
| `reminders/` | AlarmManager, notifications, action receivers, reschedule-on-boot | `data/`, `schedule/` |
| `health/` | Health Connect meal polling → books meal reminders | `data/`, `reminders/` |
| `ui/` | Compose screens + ViewModels | `data/`, `lookup/`, `reminders/` |

Min SDK 28 (Health Connect requirement); target the latest stable SDK.

## 9. Testing

- **`schedule/`** (unit, most thorough): each schedule type; midnight crossing; a
  course's last day and end date; weekday wraparound; DST forward and back; every-N-hours
  across days; fallback vs. meal arriving first; no duplicate reminder from multiple meals.
- **`lookup/`** (unit, with recorded API responses): found, not found, combination drug,
  one source failing, offline.
- **`data/`** (Room instrumented tests): save/edit, pause keeps history, delete cascades,
  PENDING→MISSED after 2 h.
- **`reminders/`, `health/`** (a few instrumented tests): booking an alarm, handling the
  Taken action.
- **Manual on-device checklist:** reboot, airplane mode, battery saver, locked screen,
  time-zone change, a Health Connect meal logged from another app, permission revoked.
