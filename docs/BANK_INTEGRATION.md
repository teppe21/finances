# Bank Integration Architecture

## 1. Notification Import vs. Open Banking (PSD2)

| Dimension | Android Notification Import | Open Banking (PSD2 / Berlin Group) |
|---|---|---|
| **Mechanism** | Native `NotificationListenerService` | OAuth 2.0 / AISP Server Integration |
| **Latency** | Instant (real-time notification delivery) | Scheduled sync (typically 4x daily or on user refresh) |
| **Infrastructure** | 100% on-device (Zero backend server required) | Requires registered backend, certificate, regulatory license |
| **Bank Scope** | Any app sending payment notifications | Limited to participating Open Banking institutions |
| **Privacy** | 100% local-first | Data passes through aggregator / backend |
| **Availability** | Available immediately | Optional future plugin layer |

---

## 2. Cross-Source Conflict Resolution
When a user uses both Bank Notifications and Statement / Open Banking imports:
- **Deduplication Engine**: Identifies identical external transaction IDs, exact timestamps, or matches amount + currency within +/- 2 calendar days.
- **User Edits Priority**: Manual user categorizations, notes, or corrections are permanent and are never overwritten by automated reprocessing.
