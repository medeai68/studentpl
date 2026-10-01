# StudentPL — Complete Project Reference

> **Purpose of this file:** Give this entire document to Claude, ChatGPT, or any AI assistant to generate a detailed report, audit, summary, or analysis of the StudentPL Android application. It contains every architectural detail, file listing, database schema, and feature description.

---

## 1. PROJECT OVERVIEW

**StudentPL** is a full-stack Android student planner application built with:
- **Kotlin** + **Jetpack Compose** (Material 3) for the UI
- **Room** (SQLite) for local offline storage
- **MariaDB/MySQL** for remote cloud sync
- **Retrofit** + **DeepSeek API** for AI-powered study assistance
- **MVVM** architecture with Repository pattern

The app allows students to manage tasks, exams, and subjects, sync data to a remote MySQL server, receive push-notification reminders, and chat with an AI assistant about their studies. Each user account has fully isolated data (tasks/exams/subjects are scoped per email).

---

## 2. TECH STACK & DEPENDENCIES

| Category | Technology | Version / Detail |
|---|---|---|
| Language | Kotlin | JVM 17 target |
| UI | Jetpack Compose + Material 3 | BOM managed |
| Navigation | Navigation Compose | Bottom nav + screen routes |
| Local DB | Room | `fallbackToDestructiveMigration()` |
| Remote DB | MariaDB Connector (JDBC) | MySQL-compatible, hosted at `10.29.14.181:3306` |
| AI | DeepSeek Chat API | `deepseek-chat` model, Bearer token auth |
| HTTP | Retrofit + OkHttp + kotlinx.serialization | JSON, logging interceptor |
| DI | Manual (no Hilt/Dagger) | `PlannerApplication` + `by lazy` |
| Async | Kotlin Coroutines + Flow | `viewModelScope`, `Dispatchers.IO` |
| Notifications | Android AlarmManager + NotificationCompat | Exact alarms, boot-reschedule |
| Preferences | Jetpack DataStore | Theme persistence (Light/Dark/System) |
| Build | Gradle (Kotlin DSL) | compileSdk 35, minSdk 24, targetSdk 35 |
| Desugaring | `coreLibraryDesugaring` | Java 8+ APIs on older Android |

---

## 3. PROJECT FILE STRUCTURE

```
studentpl/
├── build.gradle.kts                          # Root build script
├── settings.gradle.kts                       # Project settings
├── gradle.properties                         # Gradle config (JDK path, JVM args)
├── PROJECT_REFERENCE.md                      # This file
├── app/
│   ├── build.gradle.kts                      # App build script (dependencies)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml               # Permissions, activities, receivers
│       ├── res/
│       │   ├── values/colors.xml             # XML color resources
│       │   ├── values/strings.xml            # App name
│       │   ├── values/themes.xml             # Light theme XML
│       │   ├── values-night/themes.xml       # Dark theme XML
│       │   ├── mipmap-*/                     # Launcher icons
│       │   ├── drawable/                     # Adaptive icon layers
│       │   └── xml/backup_rules.xml, data_extraction_rules.xml
│       └── java/com/example/studentpl/
│           ├── MainActivity.kt               # Entry point, NavHost, bottom bar
│           ├── PlannerApplication.kt         # Application class (DI container)
│           │
│           ├── model/
│           │   ├── Task.kt                   # Task entity (Room: id, title, desc, dueDate, completed, userEmail, reminderEnabled, reminderDaysBefore, reminderTime)
│           │   ├── Exam.kt                   # Exam entity (Room: id, subject, examDate, notes, userEmail, reminderEnabled, reminderDaysBefore, reminderTime)
│           │   ├── Subject.kt                # Subject entity
│           │   └── User.kt                   # User entity
│           │
│           ├── data/
│           │   ├── AppDatabase.kt            # Room database (v7)
│           │   ├── DatabaseConnector.kt      # Remote MySQL operations
│           │   ├── dao/
│           │   │   ├── TaskDao.kt            # Task CRUD queries
│           │   │   ├── ExamDao.kt            # Exam CRUD queries
│           │   │   ├── SubjectDao.kt         # Subject CRUD queries
│           │   │   └── UserDao.kt            # User CRUD queries
│           │   └── preferences/
│           │       └── ThemePreferenceManager.kt  # DataStore theme persistence
│           │
│           ├── repository/
│           │   ├── PlannerRepository.kt      # Local data ops (user-scoped flows)
│           │   └── AiRepository.kt           # DeepSeek API calls
│           │
│           ├── network/
│           │   ├── RetrofitClient.kt         # Retrofit + OkHttp setup
│           │   ├── DeepSeekApiService.kt     # API interface (POST /chat/completions)
│           │   └── model/
│           │       └── ChatModels.kt         # ChatRequest, ChatMessage, ChatResponse, Gemini models
│           │
│           ├── viewmodel/
│           │   ├── PlannerViewModel.kt       # All business logic, CRUD, sync
│           │   └── PlannerViewModelFactory.kt
│           │
│           ├── ui/
│           │   ├── theme/
│           │   │   ├── Color.kt              # Full color palette (light + dark + gradients)
│           │   │   ├── Theme.kt              # Material 3 theme (light + dark schemes)
│           │   │   ├── Type.kt               # Typography scale
│           │   │   └── Shape.kt              # Rounded corner shapes
│           │   └── screens/
│           │       ├── HomeScreen.kt         # Dashboard with stats, tasks, exams
│           │       ├── LoginScreen.kt        # Email/password login
│           │       ├── SignupScreen.kt       # Account registration
│           │       ├── TaskManagementScreen.kt   # Task list with filters/sort
│           │       ├── SubjectsScreen.kt     # Subject list
│           │       ├── ExamsScreen.kt        # Exam list with countdown
│           │       ├── ProfileScreen.kt      # User profile + stats + sync
│           │       ├── SettingsScreen.kt     # Theme toggle, app info
│           │       ├── AiAssistantScreen.kt  # DeepSeek AI chat
│           │       ├── AddEditTaskScreen.kt  # Task create/edit form with reminder settings
│           │       ├── AddEditSubjectScreen.kt   # Subject create/edit form
│           │       ├── AddEditExamScreen.kt  # Exam create/edit form with reminder settings
│           │       └── ReminderSettings.kt   # Reusable reminder customization composable
│           │
│           ├── navigation/
│           │   └── Screen.kt                 # Route definitions + bottom nav items
│           │
│           └── util/
│               ├── NotificationHelper.kt     # Notification channel + builder
│               ├── NotificationScheduler.kt  # AlarmManager scheduling
│               └── NotificationReceiver.kt   # BroadcastReceiver for alarms
```

---

## 4. ARCHITECTURE (MVVM + Repository)

```
┌─────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose Screens)                 │
│  HomeScreen, TasksScreen, ExamsScreen, etc.         │
│  Observes LiveData from ViewModel                   │
└──────────────────────┬──────────────────────────────┘
                       │ calls methods
┌──────────────────────▼──────────────────────────────┐
│  ViewModel (PlannerViewModel)                       │
│  - Business logic, user scoping, sync orchestration │
│  - Exposes LiveData<List<T>> to UI                  │
│  - Calls Repository + DatabaseConnector             │
└──┬──────────────────────────────────┬───────────────┘
   │ calls                            │ calls
┌──▼──────────────────┐    ┌─────────▼────────────────┐
│  PlannerRepository  │    │  DatabaseConnector       │
│  - Room DAO wrapper │    │  - Remote MySQL (JDBC)   │
│  - flatMapLatest    │    │  - authenticate()        │
│    user-scoped flows│    │  - syncTask/Exam/Subject │
│  - Local CRUD       │    │  - fetchTasks/Exams/Subj │
└──┬──────────────────┘    └──────────────────────────┘
   │ calls
┌──▼──────────────────┐
│  Room Database      │
│  - TaskDao          │
│  - ExamDao          │
│  - SubjectDao       │
│  - UserDao          │
│  (SQLite on-device) │
└─────────────────────┘
```

### Data flow for login:
1. `login(email, password)` → `DatabaseConnector.authenticate()` → MySQL `simoapp` table
2. On success → `clearAllUserData()` (wipe local Room)
3. `insertUser(user)` → Room `simoapp` table
4. `DatabaseConnector.fetchTasks/Exams/Subjects(email)` → MySQL SELECT
5. Insert fetched data into Room → UI auto-updates via reactive flows

### Data flow for creating an item:
1. User fills form → screen creates entity object (e.g., `Exam(subject=..., examDate=..., notes=...)`)
2. `viewModel.insertExam(exam)`
3. ViewModel gets `userEmail` from current user → copies entity with `userEmail`
4. `repository.insertExam(scoped)` → Room INSERT → returns auto-generated ID
5. `DatabaseConnector.syncExam(scoped.copy(id=...), email)` → MySQL UPSERT
6. Room Flow re-emits → LiveData updates → UI recomposes

### Data flow for deleting an item:
1. User taps delete → confirmation dialog → `viewModel.deleteExam(exam)`
2. ViewModel gets `userEmail`, calls `repository.deleteExam(exam)` → Room `DELETE FROM exams WHERE id = ?`
3. Checks `deleted > 0` (returns early if 0 — nothing was deleted)
4. `DatabaseConnector.deleteExam(exam.id, email)` → MySQL `DELETE`
5. Cancels scheduled notification via `AlarmManager`
6. Room Flow re-emits → UI recomposes

---

## 5. DATABASE SCHEMA

### 5.1 Local Room Database (`student_planner_database`, version 7)

```sql
-- User table (cached from MySQL on login)
CREATE TABLE simoapp (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    email       TEXT,
    passwordHash TEXT
);

-- Tasks (user-scoped, with per-item reminder settings)
CREATE TABLE tasks (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    title              TEXT NOT NULL,
    description        TEXT NOT NULL,
    dueDate            TEXT NOT NULL,
    completed          INTEGER NOT NULL DEFAULT 0,
    userEmail          TEXT NOT NULL DEFAULT '',
    reminderEnabled    INTEGER NOT NULL DEFAULT 1,
    reminderDaysBefore TEXT NOT NULL DEFAULT '0,1,2',
    reminderTime       TEXT NOT NULL DEFAULT '09:00'
);

-- Exams (user-scoped, with per-item reminder settings)
CREATE TABLE exams (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    subject            TEXT NOT NULL,
    examDate           TEXT NOT NULL,
    notes              TEXT NOT NULL,
    userEmail          TEXT NOT NULL DEFAULT '',
    reminderEnabled    INTEGER NOT NULL DEFAULT 1,
    reminderDaysBefore TEXT NOT NULL DEFAULT '0,1,3,7',
    reminderTime       TEXT NOT NULL DEFAULT '08:00'
);

-- Subjects (user-scoped)
CREATE TABLE subjects (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    color       TEXT NOT NULL,
    userEmail   TEXT NOT NULL DEFAULT ''
);
```

### 5.2 Remote MySQL Database (`testdb` on `10.29.14.181:3306`)

```sql
-- User accounts
CREATE TABLE IF NOT EXISTS simoapp (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    email    VARCHAR(255) UNIQUE,
    password VARCHAR(255)
);

-- Tasks (synced, with per-item reminder settings)
CREATE TABLE IF NOT EXISTS tasks (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    remote_id            INT,
    title                VARCHAR(255),
    description          TEXT,
    due_date             VARCHAR(50),
    completed            BOOLEAN,
    user_email           VARCHAR(255),
    reminder_enabled     BOOLEAN DEFAULT TRUE,
    reminder_days_before VARCHAR(100) DEFAULT '0,1,2',
    reminder_time        VARCHAR(10) DEFAULT '09:00',
    UNIQUE KEY (remote_id, user_email)
);

-- Exams (synced, with per-item reminder settings)
CREATE TABLE IF NOT EXISTS exams (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    remote_id            INT,
    subject              VARCHAR(255),
    exam_date            VARCHAR(50),
    notes                TEXT,
    user_email           VARCHAR(255),
    reminder_enabled     BOOLEAN DEFAULT TRUE,
    reminder_days_before VARCHAR(100) DEFAULT '0,1,3,7',
    reminder_time        VARCHAR(10) DEFAULT '08:00',
    UNIQUE KEY (remote_id, user_email)
);

-- Subjects (synced)
CREATE TABLE IF NOT EXISTS subjects (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    remote_id   INT,
    name        VARCHAR(255),
    color       VARCHAR(50),
    user_email  VARCHAR(255),
    UNIQUE KEY (remote_id, user_email)
);
```

> **Key design:** `remote_id` = Room's auto-generated `id`. The `UNIQUE KEY(remote_id, user_email)` ensures one user's item with a given local ID maps to exactly one remote row. UPSERT (`ON DUPLICATE KEY UPDATE`) is used for sync.

---

## 6. NAVIGATION & ROUTES

| Route | Screen | Bottom Nav? | Auth Required? |
|---|---|---|---|
| `login` | LoginScreen | No | No |
| `signup` | SignupScreen | No | No |
| `home` | HomeScreen (Dashboard) | Yes (Home icon) | Yes |
| `tasks` | TasksScreen | Yes (CheckCircle icon) | Yes |
| `subjects` | SubjectsScreen | Yes (MenuBook icon) | Yes |
| `exams` | ExamsScreen | Yes (School icon) | Yes |
| `ai_assistant` | AiAssistantScreen | Yes (AutoAwesome icon) | Yes |
| `profile` | ProfileScreen | No | Yes |
| `settings` | SettingsScreen | No | Yes |
| `add_edit_task/{taskId}` | AddEditTaskScreen | No | Yes |
| `add_edit_subject/{subjectId}` | AddEditSubjectScreen | No | Yes |
| `add_edit_exam/{examId}` | AddEditExamScreen | No | Yes |

**Navigation behavior:**
- After login success → clear entire backstack, navigate to `home`
- After signup success → clear backstack, navigate to `login`
- After logout → clear backstack, navigate to `login`
- Auto-login: if a user exists in local Room on app start, skip auth and go directly to `home`
- Bottom bar: hidden on `login` and `signup` routes; Home tab pops back to Home instead of re-navigating
- Screen transitions: `fadeIn(300ms) + slideInVertically(400ms)`, exit with `fadeOut(200ms)`

---

## 7. FEATURE BREAKDOWN

### 7.1 Authentication
- **Login:** Email + password against remote MySQL `simoapp` table
- **Signup:** Email uniqueness check, then INSERT into remote MySQL
- **Auto-login:** Room-cached user bypasses login screen on relaunch
- **Logout:** Clears ALL local data (user + tasks + exams + subjects)
- **User isolation:** All data queries use `flatMapLatest` on the `user` flow — when a different user logs in, all data switches automatically

### 7.2 Task Management
- Create, edit, delete tasks with title, description, due date, completion status
- Date picker dialog for due date selection
- Filter by All / Pending / Completed via `FilterChip` row
- Sort by date or title
- Search by title or description
- Mark complete/incomplete with animated checkbox
- Spring-animated task cards with swipe-friendly layout
- Delete confirmation dialog

### 7.3 Exam Management
- Create, edit, delete exams with subject name, exam date, study notes
- Days-remaining countdown displayed on each exam card
- Color-coded urgency: green (7+ days), orange (4-7 days), red (0-3 days), gray (past)
- Animated progress bar showing countdown
- Search and sort functionality
- Subject initial circle with gradient background

### 7.4 Subject Management
- Create, edit, delete subjects with name and color
- 10-color palette selector with animated selection indicators
- Live preview circle showing the selected color with the subject initials
- Color circles in the list with gradient backgrounds

### 7.5 Home Dashboard
- **Greeting header:** Dynamic time-based greeting (morning/afternoon/evening/night) with user avatar and theme toggle
- **Streak card:** Shows completed task count (capped at 7-day streak representation)
- **Progress card:** Gradient card with animated `LinearProgressIndicator` showing `completed/total` tasks
- **Quick actions:** "Ask AI" (cyan gradient) and "New Task" (purple gradient) cards
- **Stats row:** Pending tasks, Exams, Completed — each with icon and accent color
- **Today's tasks:** Filtered to today's date, with animated completion
- **Upcoming exams:** Top 4 exams with countdown badges and subject initials

### 7.6 AI Assistant
- Chat interface powered by **DeepSeek Chat API** (`deepseek-chat` model)
- System prompt injected with current subject names for context
- User messages: purple gradient bubble, right-aligned
- AI messages: surface variant bubble, left-aligned
- Typing indicator with `CircularProgressIndicator` + "Thinking..." text
- Suggestion chips for quick prompts ("Generate a study plan", "Summarize my exams", etc.)
- Animated pulsing glow icon when chat is empty
- Clear chat button in top bar

### 7.7 Profile
- Gradient avatar circle with user initials
- Academic stats grid: Tasks Done, Subjects, Exams, Progress %
- Animated overall progress bar
- Sync-to-cloud button
- Logout in top bar

### 7.8 Settings
- Theme toggle: **3-segment segmented control** (Light ☀️ / Dark 🌙 / System 🔆)
- Persisted via Jetpack DataStore
- Account section with profile and notification rows
- About section with version info

### 7.9 Smart Notifications (Multi-Stage)
The notification system fires reminders at **multiple stages** before each due date, escalating in urgency as the date approaches.

**Task reminders — 3 stages** (default, customizable per item):

| Stage | When | Time | Message |
|---|---|---|---|
| Early heads-up | 2 days before due | 10:00 AM | "⏰ *title* is due in 2 days. Plan ahead!" |
| Last call | 1 day before due | 6:00 PM | "📋 *title* is due tomorrow. Finish it up!" |
| Day-of | Due date | 9:00 AM | "🔔 *title* is due today. Don't forget!" |

**Exam reminders — 4 stages** (default, customizable per item):

| Stage | When | Time | Message |
|---|---|---|---|
| Early warning | 7 days before | 12:00 PM | "📅 *subject* exam in 7 days. Start a study plan!" |
| Ramp up | 3 days before | 10:00 AM | "⏳ *subject* exam in 3 days. Ramp up your prep!" |
| Final review | 1 day before | 8:00 PM | "📚 *subject* exam tomorrow. Final review!" |
| Day-of | Exam day | 7:00 AM | "🎯 *subject* exam today. Stay calm!" |

**Smart Daily Summary** (8:00 PM every day):
- Dynamically queries the database at alarm time
- Lists tasks due **tomorrow** by name
- Lists exams in the **next 3 days**
- Warns about **overdue tasks** (past due, not completed)
- Falls back to encouraging message if all caught up: "All caught up! Take some time to review or plan ahead. 🌟"

**Notification ID scheme** (collision-free):
```
Task at day offset D:  task.id + (D * 10_000)
Exam at day offset D:  exam.id + 50_000 + (D * 10_000)
Daily summary:         999_999
```

**Boot reschedule:** `NotificationReceiver` listens for `BOOT_COMPLETED` and re-registers all alarms. Notification channel: "Student Planner Reminders" (DEFAULT importance). Tapping any notification opens `MainActivity`.

### 7.10 Per-Item Reminder Customization
Every task and exam carries its own reminder settings, editable when creating or editing the item.

**Fields on each Task/Exam entity:**
| Field | Type | Default (Task) | Default (Exam) | Purpose |
|---|---|---|---|---|
| `reminderEnabled` | Boolean | `true` | `true` | Master toggle — turn off all reminders for this item |
| `reminderDaysBefore` | String | `"0,1,2"` | `"0,1,3,7"` | Comma-separated day offsets before the due date |
| `reminderTime` | String | `"09:00"` | `"08:00"` | HH:mm 24-hour time for all reminders on this item |

**UI — ReminderSettingsSection composable** (reusable, in both AddEditTaskScreen and AddEditExamScreen):
- Collapsible card with enable/disable `Switch`
- Summary line showing current configuration (e.g., "Day of, 1 day before at 09:00")
- Multi-select `FilterChip` row for day selection:
  - Tasks: Day of, 1 day before, 2 days before, 3 days before
  - Exams: Day of, 1 day before, 3 days before, 7 days before
- `TimePickerDialog` button for selecting reminder hour/minute (24h format)
- Spring-animated expand/collapse

**Scheduler behavior:**
- Reads `reminderEnabled` — if false, schedules nothing and cancels existing alarms
- Parses `reminderDaysBefore` into a list of day offsets
- For each offset, schedules one `AlarmManager.setExactAndAllowWhileIdle` alarm at the specified time
- All past alarms are automatically skipped (not scheduled)
- On update/edit, old alarms are cancelled and new ones registered
- On delete, all alarms for that item are cancelled (scans offsets 0–14 to catch all possible IDs)

### 7.11 Theme System
- **3 modes:** Light, Dark, System (follows device setting)
- **Light palette:** Purple primary (#6C5CE7), Cyan secondary (#00B8D4), Coral tertiary (#FF6B6B), warm white background (#FAFAFE)
- **Dark palette:** Lavender primary (#B4A4FF), bright secondary (#5CD6FF), pink tertiary (#FF8A8A), deep purple background (#0F0D18)
- **Gradient colors:** Predefined gradient stops for cards (purple→cyan, purple→coral, dark surfaces)
- **Shapes:** Rounded corners throughout (6dp to 32dp, no cut corners)
- **Typography:** Full Material 3 type scale from displayLarge (36sp) to labelSmall (10sp)

---

## 8. COMPOSE UI PATTERNS

### Animations Used
| Screen | Animation | Type |
|---|---|---|
| Home | Staggered entrance | `AnimatedVisibility` with increasing `tween` delays |
| Home | FAB scale-in | `scaleIn(spring(dampingRatio=MediumBouncy))` |
| Home | Progress bar fill | `animateFloatAsState(1000ms)` |
| Home | Task completion scale | `animateFloatAsState(spring)` on card scale |
| Login/Signup | Staggered field reveal | `slideInVertically(tween)` with sequential delays |
| Login/Signup | Background gradient drift | `rememberInfiniteTransition` on gradient offset |
| Tasks/Exams/Subjects | List item entry | `fadeIn + slideInVertically` |
| AI Chat | Chat bubble slide-in | `slideInHorizontally(spring)` from respective side |
| AI Chat | Glow pulse on icon | `rememberInfiniteTransition` on alpha |
| Bottom Nav | Slide in/out | `slideInVertically(spring(LowBouncy))` |
| Bottom Nav | Label expand | `expandVertically(spring) + fadeIn` on selected item |
| Profile | Avatar entry | `scaleIn(tween(500)) + fadeIn` |

### Common Component Patterns
- **Cards:** `RoundedCornerShape(16dp)`, `CardDefaults.elevation(1-2dp)`, surface color
- **Buttons:** `RoundedCornerShape(14-16dp)`, 56dp height for primary actions
- **FAB:** `RoundedCornerShape(18dp)`, primary color, 28dp icon
- **Search fields:** `OutlinedTextField` with `RoundedCornerShape(14dp)`, leading search icon, trailing clear button
- **Gradients:** `Brush.linearGradient` on cards, headers, avatars, chat bubbles
- **Delete confirmations:** `AlertDialog` with cancel/delete buttons

---

## 9. PERMISSIONS (AndroidManifest.xml)

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Show task/exam reminder notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Schedule exact-time task/exam reminders |
| `USE_EXACT_ALARM` | Enhanced exact alarm permission |
| `INTERNET` | MySQL JDBC connection + DeepSeek API calls |
| `RECEIVE_BOOT_COMPLETED` | Re-register alarms after device reboot |

---

## 10. KNOWN ARCHITECTURAL NOTES

1. **No dependency injection framework** — `PlannerApplication` acts as a manual service locator using `by lazy`
2. **Direct JDBC for MySQL** — uses MariaDB Connector/J directly, not via Room. Connections are opened and closed per operation on `Dispatchers.IO`
3. **`fallbackToDestructiveMigration()`** — Room wipes local data on schema version change. Acceptable because MySQL is the source of truth
4. **User passwords stored in plaintext** — `simoapp.password` column has no hashing. This is a known security gap
5. **`usesCleartextTraffic=true`** — allows HTTP connections (needed for JDBC to MySQL on LAN)
6. **No encryption** — local Room DB has no SQLCipher encryption; remote MySQL has no SSL
7. **Gemini model classes exist but are unused** — `GeminiRequest`, `GeminiResponse`, etc. are defined in ChatModels.kt but the app currently only uses DeepSeek
8. **Notification ID collision prevention** — uses a structured offset scheme: tasks at `id + (D * 10_000)`, exams at `id + 50_000 + (D * 10_000)`, daily summary at `999_999`. This supports up to 14 day-offset stages with no collisions
9. **Per-item reminder persistence** — reminder settings (`reminderEnabled`, `reminderDaysBefore`, `reminderTime`) are stored on each Task/Exam entity and synced to MySQL. The scheduler reads these fields at schedule-time rather than using hardcoded values
10. **`firstOrNull()` in CRUD** — `requireUserEmail()` collects the first emission from the `user` Flow, which can theoretically suspend if the flow hasn't emitted. In practice, Room Flows cache the last value and emit immediately

---

## 11. BUILD & RUN INSTRUCTIONS

```bash
# Prerequisites
- Android Studio (Hedgehog or newer)
- JDK 17 (set in gradle.properties)
- Android SDK 35
- MariaDB/MySQL server accessible at configured host

# Set API key
echo "DEEPSEEK_API_KEY=sk-your-key-here" >> local.properties

# Build
./gradlew assembleDebug

# Output
app/build/outputs/apk/debug/app-debug.apk

# For emulator MySQL access
adb reverse tcp:3306 tcp:3306
```

---

## 12. FILE-SIZE SUMMARY

| Layer | Files | Lines (approx) |
|---|---|---|
| Models | 4 | 110 |
| DAOs | 4 | 130 |
| Database + Connector | 2 | 540 |
| Repository | 2 | 140 |
| Network | 3 | 100 |
| ViewModel | 2 | 380 |
| UI Theme | 4 | 260 |
| UI Screens | 12 | 3,200+ |
| Navigation | 1 | 45 |
| Utilities | 3 | 290 |
| Manifest + Config | 5 | 150 |
| **Total** | **42** | **~5,350** |

---

> **Generated:** 2026-06-07
> **App version:** 1.0
> **Room DB version:** 7
> **Target SDK:** 35
> **Min SDK:** 24
