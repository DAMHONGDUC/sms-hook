# Features

| Feature | Behaviour | Code |
|---|---|---|
| API configs | URL, method (GET/POST/PUT/PATCH/DELETE), headers, body template, timeout (1–120 s), retries (0–10), enable switch | [apiconfig](../app/src/main/java/com/dd/sms/hook/features/apiconfig) |
| Rules | Sender list + message keyword; `Contains` (case-insensitive, ignores spaces/dashes in numbers) or `Regex` | [SmsMatcher.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/domain/service/SmsMatcher.kt) |
| Getting started | Dashboard card for new users: permissions → first API → first forwarded SMS, with progress; hidden when all done | [GettingStartedCard.kt](../app/src/main/java/com/dd/sms/hook/features/dashboard/presentation/components/GettingStartedCard.kt) |
| Test request | Sends the unsaved config with a sample SMS, shows "It works!" or what to fix; stored in history as `TEST`, excluded from stats | [TestCallDialog.kt](../app/src/main/java/com/dd/sms/hook/features/apiconfig/presentation/editor/TestCallDialog.kt) |
| Delivery | One WorkManager job per matched API; needs network; expedited; exponential backoff from 15 s | [WorkManagerCallScheduler.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/data/work/WorkManagerCallScheduler.kt) |
| Keep-alive | Optional `specialUse` foreground service, restarted on boot and app update | [KeepAliveService.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/platform/KeepAliveService.kt) |
| History | Two tabs. Sent: grouped by day; search, status and per-API filter chips; call detail with delivery timeline of every attempt, edit in the bottom bar and Retry for failed calls only. Queue: read-only list of calls waiting for network, their turn or a retry | [calllog](../app/src/main/java/com/dd/sms/hook/features/calllog) |
| Retry all failed | With one API selected in History, re-queues every SMS whose latest real attempt to it failed and is not already queued, after a confirm | [CallQueueUseCases.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/domain/usecase/CallQueueUseCases.kt) |
| Dashboard | SMS received, API calls, success rate, avg latency, calls per day, top APIs, recent calls (7/30 days) | [dashboard](../app/src/main/java/com/dd/sms/hook/features/dashboard) |
| Failure notification | Posted when the last attempt fails (setting, default on) | [NotificationHelper.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/platform/NotificationHelper.kt) |
| Retention | Daily cleanup of logs and SMS older than 7/30/90 days or never | [LogCleanupWorker.kt](../app/src/main/java/com/dd/sms/hook/features/dispatch/data/work/LogCleanupWorker.kt) |
| Demo data | Dev builds: Settings → Developer adds 3 disabled `Demo · ` APIs, 14 days of SMS (SIM id `-2`) and call history with successes, retries and failures; remove deletes only that data | [DemoDataFactory.kt](../app/src/main/java/com/dd/sms/hook/features/devtools/domain/service/DemoDataFactory.kt) |
| Appearance | Theme System / Light / Dark, optional dynamic color (Android 12+), status-bar icons follow the theme | [AppearanceSection.kt](../app/src/main/java/com/dd/sms/hook/features/settings/presentation/AppearanceSection.kt) |
| Languages | English, Tiếng Việt, 简体中文, Español, हिन्दी, العربية (RTL); picked in Settings or Android 13+ per-app language settings | [AppLanguage.kt](../app/src/main/java/com/dd/sms/hook/shared/presentation/locale/AppLanguage.kt) |

## Template placeholders

| Placeholder | Value |
|---|---|
| `{{sender}}` | Sender number or name |
| `{{body}}` | Full message (multipart joined) |
| `{{received_at}}` | Receive time, epoch ms |
| `{{received_at_iso}}` | Receive time, ISO-8601 UTC |
| `{{sim}}` | SIM subscription id, `-1` if unknown |
| `{{config_name}}` | API name |

| Where | Escaping |
|---|---|
| URL | URL-encoded |
| Body with JSON content type (or starting with `{` / `[`) | JSON-escaped |
| Other body, header values | Raw |
| Unknown `{{name}}` | Left as-is |

## Retry policy

| Result | Retried |
|---|---|
| 2xx | No - success |
| Network error, timeout | Yes |
| 5xx, 408, 425, 429 | Yes |
| Other 4xx | No |
| Attempts | 1 + `max_retries` |
| Manual retry (detail or retry all) | Failed real calls only; a new queued job with its own 1 + `max_retries` attempts |
