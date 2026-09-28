# Security & Privacy Architecture

## 1. Principles
Financial data is among the most sensitive personal information on a user's device. This application is architected from the ground up on the following security rules:

1. **Local-First & On-Device Processing**:
   - Notifications are parsed on-device in memory.
   - Receipt OCR runs on-device using ML Kit without uploading images to cloud OCR servers.
   - Financial data resides in local SQLite storage.
2. **No Bank Password Storage**:
   - The app never prompts for, stores, or transmits bank login usernames, passwords, or PINs.
3. **No Screen Scraping or Invasive Workarounds**:
   - No Accessibility Services or webview scraping.
4. **No Third-Party Analytics Telemetry on Financial Text**:
   - No tracking SDKs capturing merchant names, amounts, or account balances.

---

## 2. App Lock & Biometrics
- **Lock Modes**: Off, PIN, Biometric (Fingerprint / Face ID).
- **Salted Hashing**: PIN codes are never stored in plaintext; they are hashed with a unique salt before storage.
- **Background Privacy**: Lock state activates when returning from the background if configured.

---

## 3. Permissions Transparency
- `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`: Reading authorized bank transaction notifications.
- `android.permission.CAMERA`: Photographing paper receipts.
- `android.permission.USE_BIOMETRIC`: App lock authentication.
- No `MANAGE_EXTERNAL_STORAGE` permission requested; document picker is used instead.
