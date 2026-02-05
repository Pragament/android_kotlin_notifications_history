# Notifications History

Android Kotlin app to capture, store, and manage notification history with powerful filtering and organization features.

## Features

- 📱 **Capture All Notifications** - Automatically saves notifications from all apps
- 🔍 **Smart Filtering** - Filter by app, read/unread status, or date
- ✅ **Mark Read/Unread** - Easily manage notification read status
- ⏰ **Snooze Notifications** - Snooze with preset durations (15min, 1hr, 3hr, tomorrow)
- 🗂️ **Organized History** - Notifications grouped by date with visual indicators
- 🌓 **Dark Mode** - Supports system, light, and dark themes
- 🎨 **Material 3 Design** - Modern UI with dynamic colors

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Database**: Room
- **DI**: Hilt
- **Background Work**: WorkManager
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/pragament/notificationshistory/
├── data/
│   ├── dao/
│   │   └── NotificationDao.kt
│   ├── database/
│   │   └── AppDatabase.kt
│   ├── entity/
│   │   └── NotificationEntity.kt
│   └── repository/
│       └── NotificationRepository.kt
├── di/
│   └── AppModule.kt
├── service/
│   └── NotificationListenerService.kt
├── ui/
│   ├── components/
│   │   ├── FilterBottomSheet.kt
│   │   ├── NotificationItem.kt
│   │   └── SnoozeDialog.kt
│   ├── navigation/
│   │   └── AppNavHost.kt
│   ├── screens/
│   │   ├── NotificationsScreen.kt
│   │   └── SettingsScreen.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/
│       ├── NotificationsViewModel.kt
│       └── SettingsViewModel.kt
├── worker/
│   ├── CleanupWorker.kt
│   └── SnoozeWorker.kt
├── MainActivity.kt
└── NotificationsHistoryApp.kt
```


## Permissions

The app requires **Notification Listener Service** permission to capture notifications. Users will be prompted to grant this permission in device settings.
