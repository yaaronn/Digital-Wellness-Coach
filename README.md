# 📱 Digital Wellness Coach

An intelligent Android application that helps users monitor and improve their digital habits by tracking screen time, app usage, focus sessions, and wellness goals.

---

## ✨ Features

- 📊 Track daily app usage
- ⏱️ Monitor total screen time
- 🎯 Set digital wellness goals
- 🔕 Focus mode
- 📈 Analytics dashboard
- 📅 Weekly usage reports
- 🔔 Smart reminders & notifications
- ⚙️ Accessibility and Usage Stats integration
- 💾 Local database using Room
- 🏗️ Clean Architecture with MVVM

---

## 🛠️ Tech Stack

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Hilt Dependency Injection
- Room Database
- WorkManager
- Android UsageStatsManager
- Material 3
- Coroutines & Flow

---

## 📂 Project Structure

```
app/
 ├── data/
 ├── domain/
 ├── presentation/
 ├── services/
 ├── workers/
 ├── notifications/
 └── di/
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio
- JDK 17
- Android SDK 35
- Gradle 8.x

### Clone Repository

```bash
git clone https://github.com/yaaronn/Digital-Wellness-Coach.git
```

Open the project in Android Studio and allow Gradle Sync to complete.

Run the application on a physical Android device (Android 8.0+ recommended).

---

## 📱 Permissions Required

- Usage Access
- Accessibility Service (Optional)
- Notification Permission

---

## 🏗️ Architecture

```
Presentation (Jetpack Compose)
        │
ViewModels (MVVM)
        │
Use Cases
        │
Repositories
        │
Room Database + UsageStatsManager
```

---

## 📸 Screenshots

> Screenshots will be added soon.

---

## 🚧 Current Status

This project is actively under development.

Completed:

- Dashboard
- Analytics
- Reports
- Goals
- Focus Mode
- Profile
- WorkManager Integration
- Room Database
- Hilt Dependency Injection

Currently Improving:

- Xiaomi/MIUI UsageStats compatibility
- Analytics accuracy
- Performance optimization

---

## 🎯 Future Enhancements

- AI-powered digital wellness insights
- Weekly PDF reports
- Cloud backup
- Achievement badges
- Dark/Light themes
- Cross-device synchronization

---

## 👨‍💻 Developer

**Yeshwanth Aaron**

GitHub:
https://github.com/yaaronn

---

## 📄 License

This project is licensed under the MIT License.
