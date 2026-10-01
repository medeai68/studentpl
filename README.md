# StudentPL 📚

**StudentPL** is a full-featured Android student planner app. Manage your tasks, exams and subjects, get smart multi-stage reminders, sync everything to a MySQL/MariaDB server, and chat with a DeepSeek-powered AI study assistant — all in a beautiful Material 3 interface built with Jetpack Compose.

> Offline-first: all data lives in a local Room database and syncs to a remote MariaDB/MySQL server. Every user account has fully isolated data.

## ✨ Features

### 📋 Task Management
- Create, edit and delete tasks with title, description and due date
- Filter by All / Pending / Completed, sort by date or title, search
- Animated completion checkboxes and spring-animated task cards

### 📝 Exam Management
- Track exams with subject name, date and study notes
- Days-remaining countdown with color-coded urgency (green / orange / red / gray)
- Animated progress bars

### 📚 Subjects
- Organize subjects with a 10-color palette and live preview

### 🏠 Home Dashboard
- Time-based greeting, streak and progress cards, quick actions
- Today's tasks and upcoming exams at a glance

### 🤖 AI Study Assistant
- Chat powered by the **DeepSeek Chat API** (`deepseek-chat` model)
- Suggestion chips ("Generate a study plan", "Summarize my exams", …)
- Context-aware: the AI knows your current subjects

### 🔔 Smart Notifications (multi-stage)
- **Tasks:** 3 escalating reminders (2 days before → 1 day before → day of)
- **Exams:** 4 escalating reminders (7 days → 3 days → 1 day → exam day)
- **Daily summary** at 8 PM listing tomorrow's tasks, upcoming exams and overdue items
- Scheduled with `AlarmManager` — reminders survive app restarts and device reboots

### ☁️ Cloud Sync
- Remote MySQL/MariaDB database with per-user data isolation
- Log in on any device and sync tasks, exams and subjects

### 🌗 Themes
- Light / Dark / System theme switcher, persisted with Jetpack DataStore

## 🛠 Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin (JVM 17) |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| Local DB | Room (SQLite) |
| Remote DB | MariaDB/MySQL (JDBC) |
| AI | DeepSeek Chat API via Retrofit + OkHttp |
| Serialization | kotlinx.serialization |
| Async | Kotlin Coroutines + Flow / LiveData |
| Notifications | AlarmManager + NotificationCompat |
| Preferences | Jetpack DataStore |
| Build | Gradle Kotlin DSL + version catalog |

SDK: `compileSdk 35` · `minSdk 24` · `targetSdk 35`

## 🏛 Architecture

```
┌──────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose screens)          │
│  Observes LiveData from the ViewModel        │
└─────────────────────┬────────────────────────┘
                      │ calls
┌─────────────────────▼────────────────────────┐
│  PlannerViewModel — business logic,          │
│  user scoping, sync orchestration            │
└──┬───────────────────────┬───────────────────┘
   │                       │
┌──▼──────────────────┐  ┌─▼──────────────────┐  ┌────────────────────┐
│  PlannerRepository  │  │  DatabaseConnector │  │  AiRepository      │
│  Room DAO wrapper   │  │  Remote MySQL      │  │  DeepSeek API      │
│  user-scoped flows  │  │  (JDBC)            │  │  (Retrofit)        │
└─────────────────────┘  └────────────────────┘  └────────────────────┘
```

Data flow: UI → ViewModel → Repository → Room (local) + MySQL (remote sync). Room `Flow`s re-emit on every change so the UI always stays up to date.

## 📁 Project Structure

```
app/src/main/java/com/example/studentpl/
├── MainActivity.kt            # Entry point, NavHost, bottom bar
├── PlannerApplication.kt      # DI container
├── model/                     # Room entities (Task, Exam, Subject, User)
├── data/
│   ├── AppDatabase.kt         # Room database (v7)
│   ├── DatabaseConnector.kt   # Remote MySQL operations
│   ├── dao/                   # Room DAOs
│   └── preferences/           # DataStore theme persistence
├── repository/                # PlannerRepository, AiRepository
├── network/                   # Retrofit client, DeepSeek API service
├── viewmodel/                 # PlannerViewModel + factory
├── ui/
│   ├── theme/                 # Material 3 theme, colors, typography
│   └── screens/               # Home, Login, Tasks, Exams, Subjects, AI, ...
├── navigation/                # Routes + bottom nav items
└── util/                      # Notification scheduler & receiver
```

## 🚀 Getting Started

### Requirements
- Android Studio (Ladybug or newer recommended)
- JDK 17
- Android device or emulator running API 24+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/medeai68/studentpl.git
   ```

2. **Create `local.properties`** in the project root (this file is gitignored):
   ```properties
   sdk.dir=C\:\\Path\\To\\Your\\Android\\Sdk

   # Optional — needed for the AI Assistant feature
   DEEPSEEK_API_KEY=sk-your-deepseek-api-key
   ```
   > Without a key, the rest of the app works normally; the AI chat shows a hint instead.

3. **Configure the remote database** in [DatabaseConnector.kt](app/src/main/java/com/example/studentpl/data/DatabaseConnector.kt):
   - Set `HOST`, `PORT`, `USER`, `PASSWORD` and `DB_NAME` to match your MariaDB/MySQL server.
   - The connector tries hosts in order: `10.0.2.2` (emulator loopback), your PC's LAN IP (physical devices on the same Wi-Fi), then `localhost`. Add your LAN IP to the `hosts` list if needed.
   - Tables are created automatically on first connection.

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   …or just open the project in Android Studio and press **Run ▶**.

## 🔐 Security Notes

- `local.properties` (SDK path + API keys) is gitignored — never commit it.
- User passwords are stored in plain text in the MySQL `simoapp` table for simplicity; use proper password hashing before any production deployment.
- Notifications require the `POST_NOTIFICATIONS` and exact-alarm (`SCHEDULE_EXACT_ALARM`) permissions.

## 📖 Documentation

For a deep dive into every architecture decision, the full database schema, notification ID scheme and feature breakdown, see [PROJECT_REFERENCE.md](PROJECT_REFERENCE.md).
