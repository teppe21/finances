# Bank Notification Import & Ingestion

## 1. Overview
The Bank Notification Import system provides real-time, zero-friction transaction tracking by reading payment notifications posted by authorized banking applications on Android.

---

## 2. Android NotificationListenerService Architecture
Unlike `expo-notifications` (which only manages application-level local/push notifications), this system uses a true native Android `NotificationListenerService`:

```kotlin
// android/app/src/main/java/com/sajatpenzugyek/app/notification/BankNotificationListenerService.kt
class BankNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) { ... }
}
```

Registered in `AndroidManifest.xml` with:
```xml
<service
    android:name=".notification.BankNotificationListenerService"
    android:label="Pénzügyi Értesítésfigyelő"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

---

## 3. Supported Bank Notification Profiles

| Bank / App | Package Name | Sample Notification Patterns |
|---|---|---|
| **Revolut** | `com.revolut.revolut` | `Fizetés a következőnek: LIDL, 14 500 Ft értékben`<br/>`Paid 4,200 HUF to Wolt`<br/>`Visszatérítés: 3 450 Ft` |
| **OTP Bank** | `hu.otpbank.smartbank`<br/>`hu.otpbank.mobilbank` | `Kártyás vásárlás: -4.500 Ft, Időpont: 2026.03.01, Hely: SPAR BUDAPEST`<br/>`Jóváírás: +650 000 Ft, Közlemény: Munkabér` |
| **OTP Simple** | `hu.otpbank.simple` | `Sikeres fizetés: 3 800 HUF - McDonald's` |
| **Erste Bank** | `hu.erstebank.george.app` | `Sikeres kártyás vásárlás 22 000 Ft összegben a MOL Nyrt. elfogadóhelynél.` |
| **MBH Bank** | `hu.mbhbank.app`<br/>`hu.takarek.mobilbank` | `Kártyás tranzakció -8 900 Ft, Yettel Magyarország, Kártya: *5678` |
| **Wise** | `com.transferwise.android` | `You spent 12.50 EUR at Spotify`<br/>`Elköltöttél 4 490 Ft összeget itt: Netflix` |
| **Generic** | *Configured by user* | Extracts amounts, currencies, directions, and cleans merchant name. |

---

## 4. Privacy & Source Filtering
- **Strict Package Filter**: Non-bank applications (WhatsApp, Messenger, Instagram, Gmail, SMS, YouTube) are discarded immediately in memory before any parsing or database storage occurs.
- **User Control**: Users toggle exactly which banking apps are allowed to be monitored.
- **No Remote Telemetry**: Raw notification bodies are never transmitted to external servers.

---

## 5. Confidence Scoring & Review Queue
Notifications are scored based on semantic clarity:
- **High Confidence (>= 0.85)**: Both amount and known merchant extracted unambiguously. If auto-import is enabled, committed directly.
- **Medium Confidence (0.50 - 0.84)**: Sent to the **Review Required** inbox for 1-tap user confirmation.
- **Non-Financial / Low (< 0.50)**: OTP authentication codes, security alerts, and marketing texts are automatically ignored.

---

## 6. Android OEM Restrictions & Troubleshooting
Certain manufacturers (e.g. Xiaomi MIUI, Samsung OneUI, Huawei HarmonyOS) apply aggressive background battery management that may sleep the `NotificationListenerService`:

1. **Battery Optimization**: Set the app's battery management to *Unrestricted / No restrictions*.
2. **Auto-Start**: Enable *Auto-start* permission in device settings if applicable.
3. **Diagnostics Screen**: The built-in diagnostics page verifies live listener connection status and last event timestamp.
