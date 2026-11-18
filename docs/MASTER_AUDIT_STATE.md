# Master Audit State — IOC Lens

> Continuous Adversarial Audit & Production-Readiness Engineering Ledger (6-Phase Pipeline)

---

## 1. Discovered Technology Stack & Repository Topology

| Dimension | Discovered Reality | Manifest / Source |
|---|---|---|
| Language & Framework | Kotlin `2.4.20` (KMP + Compose Multiplatform `1.12.1`) | [libs.versions.toml](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/gradle/libs.versions.toml) |
| Build System | Gradle `9.7.1` (Kotlin DSL) | [settings.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/settings.gradle.kts) |
| Modules | `:shared`, `:desktopApp`, `:androidApp`, `iosApp` (Xcode Swift glue) | [settings.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/settings.gradle.kts) |
| Networking | Ktor `3.1.1`, `kotlinx.serialization-json` `1.8.0` | [shared/build.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/build.gradle.kts) |
| Vision / QR | ZXing `3.5.4` + Android CameraX `1.4.2` + Apple Vision (`VNDetectBarcodesRequest`, wired in `iosApp`) | [libs.versions.toml](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/gradle/libs.versions.toml) |
| Animations | Compottie `2.3.2` + 4 bundled Lottie JSONs in `composeResources/files/` | [shared/build.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/build.gradle.kts) |
| Release Tooling | `copy_release_artifacts.py` (Python 3.11) + `.github/workflows/{ci,release}.yml` | [copy_release_artifacts.py](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/copy_release_artifacts.py) |

---

## 2. Adversarial Gap, Bug & Vulnerability Ledger (Audited Across 6 Teams)

### Phase 1: Architecture, Specs, Assets & Git Hygiene
- **Spec vs Code Drift**: `README.md` (EN/FA/RU), `docs/ARCHITECTURE.md`, and `CHANGELOG.md` contradicted each other on provider counts (4 vs 10 vs actual 11 zero-key + 4 legacy authenticated), claimed `SQLDelight` + `SecureStore` + system tray + global hotkey + iOS share extension (non-existent), and embedded external `img.shields.io` / `api.star-history.com` tracker URLs.
- **Template & Root Junk**: Dead `Greeting.kt`, `GreetingUtil.kt`, `compose-multiplatform.xml`, and 5 compiled `Test*.class` files left in repository root.

### Phase 2: Backend, Storage, Concurrency & Data Integrity
- **Plaintext Storage & Reflection Fallback**: `AndroidKeyValueStorage.kt` (`L20-63`), `JvmKeyValueStorage.kt` (`L20-52`), and `IosKeyValueStorage.kt` (`L5-27`) stored API keys and scan history in plaintext `.properties` / `NSUserDefaults` with unsynchronized non-atomic `FileOutputStream` writes and Android hidden-API reflection (`ActivityThread.currentApplication`) falling back to `java.io.tmpdir`.
- **Unbounded HTTP & Missing Resilience**: `ThreatIntelClient.kt` called unbounded `response.bodyAsText()` (OOM risk on `crt.sh`/upstream bombs), threw `IllegalArgumentException` on `JsonNull` casts (`L178, 230, 350, 410, 452, 535, 676, 730, 802`), had zero `FeodoTrackerProvider` cache (`L580`), zero retry/circuit-breaker/SingleFlight deduplication, and `ThreatIntelAggregator.kt` masked full network outages identically to `NO_DATA` without an error flag.
- **Engine Telemetry Slop**: `EngineProbeStatus.kt` (`L25-136`) fabricated initial `isOnline = true` and `pingMs` (`14..65ms`), swallowed `CancellationException` (`L163`), and counted HTTP `400..499` (401/403/429) as `isOnline = true`. `IpWhoIsProvider` was omitted from `DEFAULT_PROBE_ENGINES`.
- **Concurrency & Queue Bugs**: `HomeViewModel.kt` ran blocking `saveScanHistory` inside `MutableStateFlow.update` CAS loops (`L382, 514`), stuck single-file `BatchFilesSelected` permanently in `QueueItemStatus.HASHING` (`L419-431`), swallowed `CancellationException` in `startQueueProcessing` (`L476, 505`), and used non-atomic `SharePayloadReceiver.consume()`.
- **Log Hygiene**: Raw `System.err.println` / `printStackTrace` in `main.kt`, `DesktopDragAndDropCoordinator.kt`, and `FilePickerLauncher.jvm.kt`; exception URLs could leak API keys into `SourceResult.summary`.
- **iOS Hasher Bug**: `ChunkedStreamHasher.ios.kt` (`L48-55`) never checked `ferror()` or `stat()` (`S_ISREG`), returning truncated hashes on I/O failures or directories.

### Phase 3: Anti-Slop UI/UX, Dark/Light Theme, Localization & Client Wiring
- **Dead / Unwired UI**: `OverallVerdictCard` (`HomeScreen.kt:1937-2058`) hid all `verdict.sourceResults` (`LiveProviderDeck` at `L2061` was 100% dead code); `SettingsScreen` lacked Theme selector, Privacy Guard toggle & custom rules UI, per-engine enable/disable toggles, optional API keys, timeout/history settings, and `"Replay Interactive Guide"` trigger (`AppStrings` had 65+ dead properties).
- **Broken Flows**: `OnboardingSlider` (`L111, 114, 137`) never rendered `onSkipClicked` or called `focusRequester.requestFocus()`; `SearchCard` lacked Enter/Search key submit; `FileHashingBanner` could not cancel; `OverallVerdictCard` lacked Copy IOC / Defang / Report export / Re-Scan; Clear History lacked confirmation.
- **Broken Light Mode Contrast**: `AppTheme` lacked `LIGHT`, `PortXLightColorScheme.outline` was invisible `#E2E8F0` on `#F3F5F9`, and 100+ UI elements hardcoded `Color.White`, `CardDark`, `Color(0xFF070B14)`, `Color(0xFF0F172A)`.
- **Localization & RTL/BiDi Bugs**: Cyrillic/Arabic typo `"баз"` in `IntroSlideData.kt:97`; Russian fell back to English across `if (isRtl)` checks (`HomeScreen.kt:690, 1639-1810`, `SettingsScreen.kt:548-719`) and `EngineProbeStatus` lacked `descriptionRu`; `CyberLensDialog` + `QuishingAnalyzer` + `PlatformCameraScanner.*` were hardcoded English; `LocaleManager.setLanguage` was never called; `toPersianDigits` corrupted technical IPs/CVEs/ASNs; LTR isolation missed in RTL inputs/filenames.
- **Unbundled Typography**: `Type.kt` used `FontFamily.Default` with only 6/15 styles and lacked bundled local fonts.

### Phase 4: Security (Red/Blue Team), Platform Glue & Build/CI
- **URL Path Injection**: Unsanitized IPv6 `%zoneId` in `IOCParser.kt:110-118` injected raw paths/queries into 6 providers (`ThreatIntelClient.kt:264, 332, 388, 520, 653, 829`), including `$apiKey` path interpolation.
- **Redirect SSRF**: `KtorClientFactory.kt` left `followRedirects = true` (dangerous on `RdapProvider` 302 redirects to internal IPs or `http://`).
- **Windows NTLMv2 SMB Leak**: `DesktopDragAndDropCoordinator.kt:67` & `DesktopDropHandler.kt:64` probed `File(line).exists()` on UNC paths (`\\attacker\share`).
- **Privacy Guard Bypasses**: Missed octal IPv4 (`012.0.0.1`), compressed IPv6 loopback/unspecified (`0::1`, `::0:1`, `0::`), IPv6 multicast (`ff00::/8`), NAT64 (`64:ff9b::/96`), 6to4 (`2002::/16`), `.home.arpa` (RFC 8375), `.onion`, multi-`@` URLs (`substringAfter` vs `substringAfterLast`), and custom rules.
- **100% Broken Temp File Cleanup & Android Confused Deputy**: `MainActivity.kt` (`L100, 115-145`) allowed `file://` URIs, lacked pipe/size caps, interpolated raw `DISPLAY_NAME`, and omitted `isTemporary = true` (alongside `HomeScreen.kt:200, 203` & `TemporaryFileCleaner` omitting `"batch_"` and canonical `Path.startsWith`).
- **Image Bombs & Vision Bugs**: `PlatformVisualDecoder.{jvm,android}.kt` decoded unbounded pixel rasters before downsampling (`OutOfMemoryError`); Android camera flip (`PlatformCameraScanner.android.kt:137-241`) never rebound CameraX; `QuishingAnalyzer.kt` missed unencoded open redirects, `\` authorities, `[IPv6]`, trailing dots, control-char schemes, and extended homographs.
- **iOS Stubs**: `FilePickerLauncher.ios.kt`, `PlatformVisualDecoder.ios.kt`, `PlatformCameraScanner.ios.kt`, `IosSystemBarController.kt`, and `iOSApp.swift` lacked real platform implementations; `KtorClientFactory` used `CIO` instead of `Darwin` on iOS.
- **Build / Packaging / CI Bugs**: Missing `docs/assets/logo/icon.png` breaking Linux DEB; malformed nested `maven.myket.ir` in `settings.gradle.kts:22`; Android release signed with debug key and ignoring `ANDROID_KEYSTORE_*`; broken `release.yml` keystore step condition & `tag_name`; hardcoded `D:\0\000...` and case-colliding copies + broken `CHECKSUMS.txt` in `copy_release_artifacts.py`.

### Phase 5: Tests, Linters & Static Analysis
- Failing `LiveMalwareHuntingVerificationTest` (`testEmotetBankingTrojanSha256Detection`, `testRedLineStealerSha256Detection`) on `:shared:testAndroidHostTest` due to Ktor `MockEngine` concurrency race on `Dispatchers.IO`.
- Flaky `DesktopPolishVerificationTest` screen-resolution assertion (`740.dp`).
- Tautological stub tests (`1 + 2 == 3` in `SharedCommonTest`, `SharedLogicAndroidHostTest`, `SharedLogicIOSTest`, `TrainMarqueeMathTest`, deleted onboarding tests in `Tier1FeatureCoverageTest`).
- Compiler deprecation warnings on legacy providers in test suites; missing Python linters/typechecks/unit tests for `copy_release_artifacts.py`.

---

## 3. Phase Execution Log

### Phase 1 — Status: COMPLETED
- **Baseline Test Run**:
  - `./gradlew.bat :shared:jvmTest :desktopApp:test --no-daemon --console=plain`: Passed prior to edits.
  - `./gradlew.bat :shared:testAndroidHostTest :androidApp:lintDebug --console=plain`: **FAILED** (`283 tests completed, 2 failed`: `LiveMalwareHuntingVerificationTest.testEmotetBankingTrojanSha256Detection` & `testRedLineStealerSha256Detection`).
- **Changes Applied**:
  - Removed dead template files: `shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/Greeting.kt`, `GreetingUtil.kt`, `shared/src/commonMain/composeResources/drawable/compose-multiplatform.xml`.
  - Removed 5 stray `.class` artifacts from repository root (`TestDedicatedDropTargets$1.class`, `TestDialogDnD$1.class`, `TestDnDFullApp$1.class`, `TestDnDReceiver$1.class`, `TestSwingHierarchy.class`).
  - Added 7 self-contained local SVG badges under `docs/assets/badges/` (`release.svg`, `downloads.svg`, `stars.svg`, `ci.svg`, `license.svg`, `kotlin.svg`, `prs.svg`) and removed external `img.shields.io` / `api.star-history.com` tracker embeds from `README.md`.
  - Reconciled `docs/ARCHITECTURE.md` and `README.md` (EN/FA/RU) to accurately document the real `:shared`, `:desktopApp`, `:androidApp`, `iosApp` module topology, 11 Zero-Key + 4 optional API-key engines, AES-256-GCM/Keychain storage, Privacy Guard custom rules, and CLI binary behavior.

### Phase 2 — Status: COMPLETED
- **Fixes Applied**:
  - Added `AppTheme.LIGHT` ([AppTheme.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/core/localization/AppTheme.kt)), `SourceResult.isError`, `TriageVerdict.timestampEpochMs` & `hasNetworkError` ([Verdict.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/domain/model/Verdict.kt)), and user-configurable `AppSettings` / `SettingsRepository` controls (`requestTimeoutMs`, `maxHistoryEntries`, `historyRetentionDays`, `disabledProviders`, `customPrivacyRules`).
  - Added cross-platform `PlatformLock` (`ReentrantLock` / `NSRecursiveLock`), thread-safe `InMemoryKeyValueStorage`, AES-256-GCM encrypted `JvmFileKeyValueStorage` (with Windows/POSIX owner-only ACLs preserving capability SIDs), `PersistentFileKeyValueStorage` (Android Keystore + `AndroidStorageContext.init()`), `IosKeyValueStorage` (`NSFileProtectionComplete` + Keychain delegate), atomic temp-flush writes, and corrupt file quarantine (max 2 backups).
  - Hardened `SettingsRepositoryImpl.kt` (synchronized locks, `LocaleManager` sync, TTL & max history pruning, unified delete by `rawValue` / `normalizedValue`) and added rotating redacted diagnostic logging (`JvmRotatingLogger` at `~/.ioclens/logs/app.log`) in `Platform.*.kt`, `FilePickerLauncher.jvm.kt`, `DesktopDragAndDropCoordinator.kt`, and `desktopApp/.../main.kt`.
  - Fixed `ThreatIntelClient.kt` (2 MB bounded streaming, null-safe JSON parsing, URL path encoding including `IpQualityScoreProvider` key, `FeodoTrackerProvider` engine-scoped 10m cache, RDAP bounded HTTPS redirects), `KtorClientFactory.*.kt` (`followRedirects = false`, `Darwin` engine on `iosMain`), and `ThreatIntelAggregator.kt` (fresh factory helpers, circuit-breaker, SingleFlight dedup, secret redaction, configurable dispatcher).
  - Replaced fabricated online telemetry in `EngineProbeStatus.kt` with honest unprobed defaults (`hasProbed = false`) and added `ipwho.is` + Russian descriptions.
  - Hardened `PrivacyGuardValidator.kt` & `IOCParser.kt` (`SHA384`/`SHA512`, `%zoneId` stripping + private flagging, octal/hex/DWORD IPv4, compressed IPv6, NAT64, 6to4, `ff00::/8`, `.home.arpa`/`.onion`, custom domain/CIDR rule matching) and `ChunkedStreamHasher.ios.kt` (`fstat`/`ferror`).
  - Refactored `HomeUiState.kt`, `HomeEvent.kt`, and `HomeViewModel.kt` (outside-update `saveScanHistory`, unified single/batch queue lifecycle, SHA-256 batch dedup, `RemoveQueueItem`/`RetryQueueItem`/`ReScanIndicator`/`SelectSampleQuery`, temp cleanup on remove/clear/`onCleared`).
  - Stabilized Ktor `MockEngine` tests using `StandardTestDispatcher(testScheduler)`.
- **Phase 2 Verification Gate**:
  - `./gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :desktopApp:test --console=plain`: **PASSED (`BUILD SUCCESSFUL in 2m 31s`, exit code 0)** — zero test failures across `:shared:jvmTest`, `:shared:testAndroidHostTest`, and `:desktopApp:test`.

### Phase 3 — Status: COMPLETED
- **Self-Contained Assets & Typography**:
  - Bundled 5 local font files under `shared/src/commonMain/composeResources/font/`: `noto_sans_regular.ttf`, `noto_sans_bold.ttf`, `noto_sans_arabic_regular.ttf`, `noto_sans_arabic_bold.ttf`, and `cascadia_mono_regular.ttf`.
  - Updated `Type.kt` (`rememberAppFontFamily`, `rememberMonospaceFontFamily`, `getAppTypography`) and `ClipboardHelper.kt` (`rememberClipboardActions()`).
- **Theme, Contrast & Tri-Lingual Localization**:
  - Added `IocSemanticColors` (`DarkIocSemanticColors`, `ClassicIocSemanticColors`, `LightIocSemanticColors`) via `LocalIocColors` in `Color.kt` / `Theme.kt` and updated `LiquidGlowBackground.kt`, `WormDotsIndicator.kt`, `SplashScreen.kt`, `OnboardingSlider.kt`, `IntroScreen.kt`, `SettingsScreen.kt`, `HomeScreen.kt`, and `desktopApp/.../main.kt` for Light/Dark/Classic contrast compliance.
  - Extended `AppStrings.kt` (EN/FA/RU) across light theme, privacy custom rules, timeouts/retention, probe status, copy/export/retry actions, confirmation dialogs, and vision scanner strings.
- **Wiring & Desktop Shell**:
  - Centralized shared singletons in `AppDependencies` ([App.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/App.kt)), wired `MainViewModel.resetIntroForReplay()`, `SplashScreen` fast-path for returning users, `AppBackHandler.jvm.kt` LIFO `Escape` handling, `SettingsScreen` custom privacy rules / timeouts / retention / optional API keys / engine toggles, `HomeScreen.kt` `LiveProviderDeck` (Desktop 2-column & Mobile, including `Feodo Tracker (abuse.ch)` & `ipwho.is`), Copy/Defang/Markdown/Re-Scan/Quick-Sample/Queue-Retry/History-Filter/JSON-Export/Confirmation-Dialog flows, and `desktopApp/.../main.kt` themed undecorated window controls with non-consuming double-click maximize and 6dp edge/corner resize handles.
- **Phase 3 Compile Verification**:
  - `./gradlew.bat :shared:compileKotlinJvm :shared:compileAndroidMain --console=plain`: **PASSED (`BUILD SUCCESSFUL`)** — 0 warnings in Phase 3 files (only 2 pre-existing warnings remaining in `CyberLensDialog.kt:76` and `PlatformCameraScanner.android.kt:162`, scheduled for Phase 4).
  - `./gradlew.bat :desktopApp:compileKotlin --console=plain`: **PASSED (`BUILD SUCCESSFUL`)** — 0 errors, 0 warnings.

### Phase 4 — Status: COMPLETED
- **Desktop UNC/SMB & CLI Hardening**:
  - Added `isUnsafeUncOrRemotePath` / `isUnsafeRemotePath` in [DesktopDropHandler.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/desktopApp/src/main/kotlin/io/github/mrcoder20/ioclens/DesktopDropHandler.kt) and [DesktopDragAndDropCoordinator.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/DesktopDragAndDropCoordinator.kt) blocking `\\UNC`, `//host`, `\\?\UNC\`, WebDAV `@SSL`, and remote `file://` authorities prior to any `File.exists()` / filesystem I/O, plus ignoring CLI flags (`-*`) in `processStartupArg`.
- **Android Intent, FilePicker & Temp Cleanup Hardening**:
  - Unified `TemporaryFileCleaner` ([TemporaryFileCleaner.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/domain/security/TemporaryFileCleaner.kt), `.jvm.kt`, `.android.kt`, `.ios.kt`) with managed prefixes (`picked_`, `shared_stream_`, `ioc_drop_`, `ioc_pick_`, `batch_`, `share_`), strict canonical temp-root containment checks, and `sweepStaleTempFiles`.
  - Updated [AndroidManifest.xml](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/androidApp/src/main/AndroidManifest.xml) (`allowBackup=false`, `fullBackupContent=false`, `Theme.DeviceDefault.NoActionBar`, removed catch-all `http`/`https` `VIEW` filters).
  - Hardened [MainActivity.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/androidApp/src/main/kotlin/io/github/mrcoder20/ioclens/MainActivity.kt) (`AndroidStorageContext.init(applicationContext)` first in `onCreate`, async stale temp sweep, `content://`-only external URI check, `dataDir` block, sanitized names, `File.createTempFile`, `256 MB` cap, `ACTION_SEND_MULTIPLE` batching with `isTemporary = true`) and [FilePickerLauncher.android.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/androidMain/kotlin/io/github/mrcoder20/ioclens/core/components/FilePickerLauncher.android.kt).
- **Vision, Quishing & Zero-Deprecation Hardening**:
  - Hardened [QuishingAnalyzer.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/domain/vision/QuishingAnalyzer.kt) (control-char stripping, `substringAfterLast('@')`, IPv6 literals, unencoded + double-encoded redirect detection, expanded confusable scripts, real `currentTimeMillis()`).
  - Updated [CyberLensDialog.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/CyberLensDialog.kt), [PlatformCameraScanner.jvm.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/PlatformCameraScanner.jvm.kt), [PlatformCameraScanner.android.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/androidMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/PlatformCameraScanner.android.kt), and `PlatformVisualDecoder.{jvm,android}.kt`: eliminated both remaining deprecation warnings (`LocalClipboardManager` and `setTargetResolution`), fixed CameraX lens-flip rebinding, removed auto clipboard read on JVM mount, enforced `20 MB` & `12000px / 36M px` bounds, defanged unmasked URL display, and wired `AppStrings` + `LocalIocColors`.
- **iOS Platform Parity & Build/CI/Python Remediation**:
  - Implemented iOS Kotlin bridges (`FilePickerLauncher.ios.kt`, `PlatformVisualDecoder.ios.kt`, `PlatformCameraScanner.ios.kt`, `IosSystemBarController.kt`) backed by native Keychain (`SecItem*`), `UIDocumentPickerViewController`, Apple Vision (`VNDetectBarcodesRequest`), camera capture, and `.onOpenURL` in [iOSApp.swift](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/iosApp/iosApp/iOSApp.swift) + [Info.plist](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/iosApp/iosApp/Info.plist).
  - Fixed [settings.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/settings.gradle.kts), [desktopApp/build.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/desktopApp/build.gradle.kts) (removed JNA, `icon-512.png`, `upgradeUuid`, `bundleID`), [androidApp/build.gradle.kts](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/androidApp/build.gradle.kts) & [proguard-rules.pro](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/androidApp/proguard-rules.pro) (env-driven keystore + R8/shrinkResources), `.github/workflows/{ci,release}.yml`, and rewrote [copy_release_artifacts.py](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/copy_release_artifacts.py).
- **Phase 4 Compile Verification**:
  - `./gradlew.bat :shared:compileKotlinJvm :shared:compileAndroidMain :desktopApp:compileKotlin :androidApp:compileDebugKotlin --console=plain`: **PASSED (`BUILD SUCCESSFUL in 11m 31s`, exit code 0)** — **0 errors and 0 Kotlin compiler warnings** across all compiled targets.

### Phase 5 — Status: COMPLETED
- **Test Refactoring & Tautology Removal**:
  - Extracted shared [MarqueeMath.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/core/components/MarqueeMath.kt) used by both `ControlledMarqueeText.kt` and `TrainMarqueeRow.kt`, and rewired `TrainMarqueeMathTest.kt` to test `MarqueeMath` directly.
  - Updated `DesktopPolishVerificationTest.kt` (resolution-agnostic `WindowBoundsClamper` bounds + `DesktopDropHandler` UNC/SMB & CLI flag rejection tests and zero smart-cast warnings), `SharedCommonTest.kt`, `SharedLogicAndroidHostTest.kt`, `SharedLogicIOSTest.kt`, `Tier1FeatureCoverageTest.kt` (`createDefaultZeroKeyProviders()`, `IntroSlideData.getSlides()`), and `HomeNavigationTest.kt` (`filterScanHistory()`).
- **New Adversarial, Storage & Packaging Test Suites**:
  - Added [AdversarialHardeningVerificationTest.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonTest/kotlin/io/github/mrcoder20/ioclens/security/AdversarialHardeningVerificationTest.kt) (`commonTest`).
  - Added [JvmStorageSecurityAndConcurrencyTest.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmTest/kotlin/io/github/mrcoder20/ioclens/storage/JvmStorageSecurityAndConcurrencyTest.kt) (`jvmTest`).
  - Added [tests/__init__.py](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/tests/__init__.py) and [tests/test_copy_release_artifacts.py](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/tests/test_copy_release_artifacts.py).
- **Final Phase 5 Verification Gate Outputs**:
  - `./gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :desktopApp:test :shared:compileCommonMainKotlinMetadata :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease --console=plain`: **PASSED (`BUILD SUCCESSFUL in 13m 32s`, exit code 0)** — **0 Kotlin compiler warnings**, **0 Android Lint errors** (`lint-results-debug.xml`: `errors=0`, `warnings=45`), release R8 minification & resource shrinking verified:
    - `:shared:jvmTest`: `328` tests across `35` suites — `0` failures, `0` errors, `0` skipped.
    - `:shared:testAndroidHostTest`: `288` tests across `26` suites — `0` failures, `0` errors, `0` skipped.
    - `:desktopApp:test`: `10` tests across `1` suite — `0` failures, `0` errors, `0` skipped.
    - Host platform scope note (Windows): iOS Kotlin/Native targets (`:shared:compileKotlinIosArm64`, `:shared:compileKotlinIosSimulatorArm64`) and Xcode Swift (`iosApp/iosApp/iOSApp.swift`) were not compiled locally on Windows (configured for `macos-latest` in `.github/workflows/ci.yml`); shared KMP metadata compilation (`:shared:compileCommonMainKotlinMetadata`) passed.
  - Python static checks & test gates (`copy_release_artifacts.py`, `tests/`):
    - `python -m compileall -q copy_release_artifacts.py tests`: exit code `0`.
    - `ruff check copy_release_artifacts.py tests`: `All checks passed!`
    - `black --check copy_release_artifacts.py tests`: `3 files would be left unchanged.`
    - `isort --check-only --profile black copy_release_artifacts.py tests`: exit code `0`.
    - `mypy --strict copy_release_artifacts.py tests`: `Success: no issues found in 3 source files`.
    - `pylint copy_release_artifacts.py tests`: **`10.00/10`**.
    - `pytest -v tests` & `python -m unittest discover -s tests -v`: **`3 passed`** (`0` failures, `0` skipped).

### Phase 6 — Status: COMPLETED
- **Git Hygiene & Workspace Cleanliness**:
  - Updated [.gitignore](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/.gitignore) (`__pycache__/`, `*.py[cod]`, `.pytest_cache/`, `.mypy_cache/`, `.ruff_cache/`) and [.gitattributes](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/.gitattributes) (`*.py`, `*.sh`, `gradlew` LF; `*.ttf`, `*.ico`, `*.icns` binary).
  - Verified `git status --short`: workspace contains only tracked/intentional source, font, badge, doc, and test files; isolated Python tooling venv kept outside the repository in session scratch storage. No automatic `git commit` or `git push` executed.

### Final Adversarial Sweep (چک نهایی) — Status: COMPLETED
- **Concurrency, Safety & Spec Fixes Applied**:
  - Synchronized `LottiePreloader` caches (`PlatformLock`) and propagated `CancellationException` in [SlideAnimatedGraphic.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/core/components/SlideAnimatedGraphic.kt).
  - Propagated `CancellationException` and added structured `logDiagnostic` logging across engine probe coroutines in [HomeScreen.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/home/HomeScreen.kt) (`probeAllEngines`) and [SettingsScreen.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/settings/SettingsScreen.kt) (`probeEngines`, `probeOneEngine`).
  - Eliminated all 7 remaining `!!` non-null assertions in production Kotlin across [CyberLensDialog.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/CyberLensDialog.kt), [QuishingAnalyzer.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/domain/vision/QuishingAnalyzer.kt), [ThreatIntelRepositoryImpl.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/data/repository/ThreatIntelRepositoryImpl.kt), and [ZeroKeyThreatIntelRepositoryImpl.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/data/repository/ZeroKeyThreatIntelRepositoryImpl.kt) (also using fresh `createDefaultZeroKeyProviders()` per aggregator instance).
  - Replaced empty/broad `catch` blocks with narrow exception handling and rate-limited/structured diagnostics in [DesktopDragAndDropCoordinator.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmMain/kotlin/io/github/mrcoder20/ioclens/presentation/vision/DesktopDragAndDropCoordinator.kt) (`acceptDragSafely`), [PlatformVisualDecoder.jvm.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmMain/kotlin/io/github/mrcoder20/ioclens/domain/vision/PlatformVisualDecoder.jvm.kt), and [PlatformVisualDecoder.android.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/androidMain/kotlin/io/github/mrcoder20/ioclens/domain/vision/PlatformVisualDecoder.android.kt) (`ReaderException` separation).
  - Synchronized Ktor `MockEngine` dispatchers with `StandardTestDispatcher(testScheduler)` in [ZeroKeyClientTddTest.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/commonTest/kotlin/io/github/mrcoder20/ioclens/data/ZeroKeyClientTddTest.kt) and [ZeroKeyProviderAdversarialStressTest.kt](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/shared/src/jvmTest/kotlin/io/github/mrcoder20/ioclens/data/ZeroKeyProviderAdversarialStressTest.kt), eliminating virtual-time timeout races.
  - Formatted [copy_release_artifacts.py](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/copy_release_artifacts.py) with `black` and prepended `[1.1.0]` release documentation in [CHANGELOG.md](file:///d:/0/000/amir-hossein-ghafari/projects/ioc-lens/CHANGELOG.md).
- **Fresh Live Gate Results (After Final Edits)**:
  - `./gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :desktopApp:test :shared:compileCommonMainKotlinMetadata :androidApp:lintDebug :androidApp:assembleDebug :androidApp:assembleRelease --console=plain`: **PASSED (`BUILD SUCCESSFUL in 10m 37s`, exit code `0`)** — `0` Kotlin compiler warnings, `0` Android Lint errors (`lint-results-debug.xml`: `errors=0`, `warnings=45`), R8 release build & resource shrinking passed:
    - `:shared:jvmTest`: `328` tests across `35` suites — `0` failures, `0` errors, `0` skipped.
    - `:shared:testAndroidHostTest`: `288` tests across `26` suites — `0` failures, `0` errors, `0` skipped.
    - `:desktopApp:test`: `10` tests across `1` suite — `0` failures, `0` errors, `0` skipped.
  - Python strict gate (`copy_release_artifacts.py`, `tests/`): `compileall` (exit `0`), `ruff` (`All checks passed!`), `black --check` (`3 files would be left unchanged.`), `isort --check-only` (exit `0`), `mypy --strict` (`Success: no issues found in 3 source files`), `pylint` (**`10.00/10`**), `pytest -v tests` & `unittest` (**`3 passed`**, `0` failures, `0` skipped).
  - Static hygiene scan: `DOUBLE_BANG_COUNT=0` (production Kotlin), `EMPTY_CATCH_COUNT=0`, `TODO_COUNT=0`, `RAW_PRINT_COUNT=0`, `BROKEN_LINKS_COUNT=0`, `EXTERNAL_CDN_COUNT=0`, `CONFLICT_MARKER_COUNT=0`.
  - Host scope note (Windows): iOS Kotlin/Native targets (`:shared:compileKotlinIosArm64`, `:shared:compileKotlinIosSimulatorArm64`) and Xcode Swift (`iosApp/iosApp/iOSApp.swift`) remain uncompiled/untested on Windows (`macos-latest` CI job targeted in `.github/workflows/ci.yml`).
