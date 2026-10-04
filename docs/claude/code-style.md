# Code style rules

| Rule | Reason | Example |
|---|---|---|
| Explicit types on properties and locals when not obvious | Readable without jumping files | `val errors: Set<ApiConfigError> = ...` |
| Declarations first, blank line, then logic | Scan-friendly functions | `CallExecution.run` |
| Every action logs with its data via `AppLogger` | Logs answer "what was sent/returned" | `AppLogger.i(TAG, "saved - {id: $id, url: $url}")` |
| Every `catch` logs the throwable before returning a fallback | Caught errors are otherwise invisible | `AppLogger.e(TAG, "delete failed - {id: $id}", e)` |
| Exception: input validation where failure is the expected answer, with a comment saying so | Runs on every keystroke; logging would flood | `ApiConfigValidator.isValidUrl` |
| No `println`, no raw `Log.*` | One logging entry point | `shared/domain/logging/AppLogger.kt` |
| No magic numbers: spacing in `Dimens`, colours in `Palette`/`StatusColors`, limits in constants | One owner per value | `Dimens.screenGutter`, `HttpConstants.MAX_RETRIES_LIMIT` |
| Every `Text` has an explicit `style =` from `MaterialTheme.typography`; the scale has only 4 sizes (28/18/15/13 sp) and 2 weights (Regular, SemiBold) — never add a size or weight inline | Hierarchy from size, weight, colour | `Text(text, style = MaterialTheme.typography.bodySmall)` |
| Numbers in stats, lists and timelines use `.tabularNumbers()` | Digits line up and do not shift | `StatTile` |
| Spacing on the 4/8 grid via `Dimens`: card padding 24, section gap 24, list gap 12 | Relationship-based rhythm | `Dimens.cardPadding` |
| Primary actions of a detail/editor screen go in a bottom action bar (`ScreenScaffold(bottomBar = …)`), not top-bar icons | Thumb zone | `ApiEditorScreen`, `CallDetailScreen` |
| Feedback that must outlive the screen (e.g. saved) goes through `LocalAppMessenger` | Shown after the screen closes | `ApiEditorScreen` |
| All user-facing strings in all 6 `values*/strings.xml` (en, vi, zh-rCN, es, hi, ar) with the same format args; counts use `<plurals>` with every quantity the locale needs | Missing keys fall back to English; lint `PluralsCandidate`, `MissingQuantity` | `R.plurals.dashboard_range_days` |
| RTL-safe UI: `start`/`end` padding, `Icons.AutoMirrored` for directional icons, numbers with units via `UiFormat` (LTR isolate), canvas drawing mirrored in RTL | Arabic layout | `CallsBarChart`, `UiFormat.duration` |
| Dates formatted with `TimeUtils.*(…, currentLocale())` | Follows the in-app language | `CallLogRow` |
| VM messages are `UiMessage(@StringRes)`, resolved in UI via `LocalResources` | Config-aware strings | `MessageEffect` |
| Status is never colour-only: icon + label | Accessibility | `StatusPill`, `IconBadge` check vs `!` |
| Colours only from `MaterialTheme.colorScheme` / `AppThemeExtras.statusColors`; every screen checked in light and dark | Theme switch | `StatusHeroCard` |
| Date/time logic only in `shared/domain/time/TimeUtils.kt` | One place for time math | `TimeUtils.startOfRange` |
| Comments ≤ 3 lines, explain why | Short, useful comments | - |
