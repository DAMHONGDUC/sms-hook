# Architecture rules

Full picture: [docs/architecture.md](../architecture.md).

| Rule | Reason | Example |
|---|---|---|
| Code goes in `features/<feature>/{domain,data,presentation,di}` | Feature-first layout | `features/calllog/presentation/detail/CallDetailScreen.kt` |
| `domain` has no Android/Room/OkHttp imports | Pure, unit-testable core | `SmsMatcher`, `RequestFactory` |
| Repository interface in `domain/repository`, impl in `data/repository`, bound with `@Binds` in the feature's `di/` module | Dependencies point inward | `ApiConfigRepository` ← `ApiConfigRepositoryImpl` |
| Repositories return domain models, never entities | Room stays in `data` | `ApiConfigMapper.toDomain` |
| Cross-feature imports only touch the other feature's `domain` (or `calllog/presentation/components`, the one shared UI entry) | Features don't fuse | `dashboard` uses `CallLogRepository` |
| Code used by 2+ features with no business owner goes in `shared/{presentation,domain,data}/<topic>`, by the layer that uses it | Shared code keeps the same layers as a feature | `shared/domain/time/TimeUtils.kt` |
| Business models and repositories stay in the owning feature's `domain`, never in `shared` | One owner per model | `ApiConfig` in `apiconfig/domain` |
| `shared/` never imports `features/` (only `shared/data/db` lists entities for Room); wiring that knows every feature lives at the root (`navigation/`, `bootstrap/`) | No cycles | `AppRoot`, `AppBootstrap` |
| A UI piece needed by 2+ features moves to `shared/presentation/ui` and takes plain values | One owner | `StatusPill(label, tone)` |
| ViewModels expose `StateFlow` state + `Channel` events; screens call VM methods only | MVVM | `ApiEditorViewModel.events` |
| Get VMs with `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` | Hilt 1.4 API | any screen |
| Routes are `@Serializable` types in `navigation/Routes.kt`; read args with `savedStateHandle.toRoute<>()` | Type-safe navigation | `CallDetailRoute(id)` |
| Background calls only through `CallScheduler.enqueue` | Durable, retrying delivery | `HandleIncomingSmsUseCase` |
| Schema change = bump `AppDatabase.version` + migration; never edit version 1 | Installed DBs already ran v1 | `app/schemas/.../1.json` |

## Never

| Don't | Instead |
|---|---|
| Call the network from `SmsReceiver` or a ViewModel for real SMS | Enqueue via `CallScheduler` |
| Add a Room foreign key from `call_logs` | History must outlive deleted configs |
| Put build config in `local.properties` or Gradle files, mix secrets into `env/env.<flavor>.properties`, or read/print `env/*.properties` | Non-secret values → `env/env.dev.properties` and `env/env.prod.properties`, signing secrets → `env/key.properties`; document every new key in the `docs/setup.md` key table and read it only through `loadEnvFile` in `app/build.gradle.kts` |
| Put config constants on models/entities | `shared/domain/constants/AppConstants.kt` or the feature's own constants object; only a canonical empty value or id sentinel may live in a model's `companion object` (`SmsFilter.ANY`, `ApiConfig.NEW_ID`) |
