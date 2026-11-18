# Architecture

```
ioc-lens/
├── shared/                              # Kotlin Multiplatform shared core + Compose UI
│   ├── src/commonMain/kotlin/io/github/mrcoder20/ioclens/
│   │   ├── core/components/             # Vector icons, flags, file pickers, animated backgrounds
│   │   ├── core/localization/           # Tri-lingual dictionaries (EN / FA / RU) + LocaleManager
│   │   ├── core/storage/                # KeyValueStorage abstraction + thread-safe storage
│   │   ├── core/theme/                  # Dark / Classic / Light semantic palette + Typography
│   │   ├── data/remote/                 # Ktor client factory, 15 providers, ThreatIntelAggregator
│   │   ├── data/repository/             # SettingsRepositoryImpl, ThreatIntelRepositoryImpl, ZeroKeyThreatIntelRepositoryImpl
│   │   ├── domain/model/                # AppSettings, Indicator, ParsedIoc, TriageVerdict, SourceResult, EngineProbeStatus
│   │   ├── domain/parser/               # IOCParser (refang/extract) + PrivacyGuardValidator
│   │   ├── domain/repository/           # Repository contracts
│   │   ├── domain/security/             # ChunkedStreamHasher, SharePayloadReceiver, TemporaryFileCleaner
│   │   ├── domain/usecase/              # AnalyzeIndicatorUseCase, GetAppSettingsUseCase, SetIntroCompletedUseCase
│   │   ├── domain/vision/               # BarcodePayload, PlatformVisualDecoder, QuishingAnalyzer
│   │   └── presentation/                # HomeScreen/ViewModel, SettingsScreen, IntroScreen, SplashScreen, CyberLensDialog
│   ├── src/androidMain/                 # Android Keystore AES-256-GCM storage, CameraX, ZXing, safestream
│   ├── src/jvmMain/                     # Desktop AES-256-GCM storage, ZXing, AWT DnD coordinator
│   └── src/iosMain/                     # iOS Keychain/UserDefaults storage, Apple Vision barcode decoder, Darwin Ktor
├── desktopApp/                          # Compose Desktop host window, bounds clamper, CLI & DnD drop handler
├── androidApp/                          # Android MainActivity share target (ACTION_SEND / SEND_MULTIPLE / VIEW)
└── iosApp/                              # SwiftUI wrapper + Info.plist document/URL handlers
```

## Pipeline
`input → extract & refang → classify → Privacy Guard (built-in + user rules) → in-flight dedup / Feodo cache → parallel providers (bounded + circuit-breaker) → verdict synthesis → UI & encrypted history`

## Provider Contract
```kotlin
interface ThreatIntelProvider {
    val name: String
    val isZeroKey: Boolean
    fun isApplicable(indicator: Indicator): Boolean
    suspend fun query(client: HttpClient, indicator: Indicator, settings: AppSettings): SourceResult
}
```

### Active Engines
- **11 Zero-Key Public Engines** (enabled out of the box):
  1. `URLhaus (abuse.ch)` — URL, Domain
  2. `MalwareBazaar (abuse.ch)` — File Hash
  3. `ThreatFox (abuse.ch)` — IP, Domain, URL, File Hash
  4. `RDAP / Registry` — IP, Domain (strict HTTPS redirect validation)
  5. `Cloudflare DoH` — Domain
  6. `Shodan InternetDB` — IP
  7. `AlienVault OTX` — IP, Domain, URL, File Hash (optional API key)
  8. `RIPE Stat` — IP
  9. `crt.sh` — Domain
  10. `Feodo Tracker (abuse.ch)` — IP (TTL-cached blocklist)
  11. `ipwho.is` — IP
- **4 Optional Authenticated Engines** (dispatched only when user configures an API key in Settings):
  `VirusTotal`, `AbuseIPDB`, `Pulsedive`, `IPQualityScore`.

## Platform Abstractions (`expect/actual`)
- `KeyValueStorage` (Android Keystore AES-256-GCM, Desktop AES-256-GCM with owner-only key permissions, iOS Keychain + `NSUserDefaults`)
- `createPlatformHttpClient` (Ktor `CIO` on Android/JVM, `Darwin` `NSURLSession` on iOS, `followRedirects = false` by default)
- `ChunkedStreamHasher` (64 KB streaming SHA-256/MD5 on `Dispatchers.IO`)
- `TemporaryFileCleaner` (canonical cache-path verified auto-cleanup)
- `FilePickerLauncher`, `PlatformVisualDecoder`, `PlatformCameraScanner`, `SystemBarController`, `AppBackHandler`, `Platform`

## Design Principles
1. **Privacy First**: Intercept RFC 1918, CGNAT, loopback, link-local, octal/hex/DWORD obfuscated IPs, IPv6 ULA/link-local/NAT64/6to4/multicast, internal TLDs (`.local .lan .internal .corp .home .home.arpa .localhost .onion`), and user-defined domains/CIDRs before any outbound request.
2. **Explainable**: Every verdict renders per-engine status, detection counts, and localized evidence.
3. **Fault-Tolerant**: Per-provider bounded timeouts, HTTP response byte caps (2 MB), Feodo TTL caching, circuit-breaker cooldowns, and structured network-error surfacing.
4. **Self-Contained**: All fonts, vector icons, badges, and Lottie graphics are bundled locally with zero external runtime CDN dependencies.

## Verdict Bands & Scoring
- **Clean (`0..19`)** | **Low risk (`20..44`)** | **Suspicious (`45..74`)** | **Malicious (`75..100`)** | **No data (`0`)**
- High-fidelity corroborated malicious detections escalate to `Malicious`; Privacy Guard local blocks never leave the device (`isInternalOrPrivate = true`).

## Localization (i18n)
- Centralized Kotlin dictionaries in `shared/src/commonMain/kotlin/io/github/mrcoder20/ioclens/core/localization/AppStrings.kt` for English (`en`), Persian (`fa` RTL), and Russian (`ru` LTR), synchronized via `LocaleManager` and reactive `StateFlow`.
