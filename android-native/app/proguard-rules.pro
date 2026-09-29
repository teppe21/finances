# Proguard / R8 rules for Finances Native App

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** INSTANCE;
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Entities, DAOs, and Domain Models
-keep class com.teppe21.finances.data.local.entity.** { *; }
-keep interface com.teppe21.finances.data.local.dao.** { *; }
-keep class com.teppe21.finances.domain.model.** { *; }

# Google ML Kit Text Recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# AndroidX CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Native Services, Receivers & Converters
-keep class com.teppe21.finances.native.notification.BankNotificationListenerService { *; }
-keep class com.teppe21.finances.data.local.database.Converters { *; }
-keep class com.teppe21.finances.MainActivity { *; }
-keep class com.teppe21.finances.FinancesApp { *; }
