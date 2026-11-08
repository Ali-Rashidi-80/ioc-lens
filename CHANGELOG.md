# Changelog
All notable changes follow [Keep a Changelog](https://keepachangelog.com) and [SemVer](https://semver.org).

## [1.0.0] - 2026-11-08

### Added
- **Serverless 10-Provider Engine**:
  - 4 Zero-Key Public Providers enabled out of the box with no API keys required: **abuse.ch URLhaus**, **abuse.ch MalwareBazaar**, **abuse.ch ThreatFox**, and **RDAP** (ARIN/RIPE network registry).
  - Real-time DNS over HTTPS resolution check via **Cloudflare DoH**.
  - 5 Authenticated Providers with optional user-supplied API keys: **VirusTotal** (v3 with URL-safe Base64 identifiers), **AbuseIPDB** (v2), **AlienVault OTX**, **Pulsedive**, and **IPQualityScore**.
- **Fault-Tolerant Aggregator**:
  - Parallel asynchronous lookups under `supervisorScope` with strict 10-second per-provider timeouts.
  - Resilient skip strategy: unreachable, filtered, or rate-limited endpoints are bypassed without hanging or stalling the scan.
  - Explainable weighted scoring with high-confidence malicious escalation logic.
- **Memory-Safe Stream Hashing**:
  - `ChunkedStreamHasher` reading files in 64KB buffers on `Dispatchers.Default` for files up to 100MB+.
  - Android, JVM, and iOS Native (Apple `CoreCrypto` + POSIX streaming) implementations.
- **Native OS Share Sheet Target**:
  - Android `ACTION_SEND` and `ACTION_VIEW` intent filters for `text/plain`, `image/*`, and `*/*` with async stream handling from Telegram and Eitaa.
  - Desktop AWT `DropTarget` drag-and-drop file/text ingestion.
  - iOS `Info.plist` document types and URL schemes.
- **Adaptive Cyberpunk Bottom Navigation Bar**:
  - Dedicated mobile and tablet bottom navigation (< 840dp) with Scanner, Engines, History, and Settings tabs, eliminating drawer dependency on touch screens.
- **Full Persian (RTL) Localization & BiDi Protection**:
  - 100% complete Persian translations for all 10 engine names, 15+ verdict result phrases, settings descriptions, and loading percentages.
  - Unicode BiDi container isolation for IP addresses, URLs, and cryptographic hashes.
- **UI/UX Hardening & PortX Patterns**:
  - Right-aligned close buttons across all sheets, dialogs, settings, and drawer headers.
  - Desktop title bar control isolation in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`.
  - High-contrast `CyberSwitch` with symmetrical 4dp padding and center core diode.
  - Intro Slider "Previous" (قبلی) navigation button and premature dismissal guard.
  - Live pulsing radar scan banner with active engine counter.

### Changed
- Replaced flawed 3-page tab swapping with unified **Single-Hub Dashboard**.
- Removed offscreen multi-Lottie pre-rendering box that caused frame drops in `SplashScreen`.
- Patched critical security flaw in `AnalyzeIndicatorUseCase` where `.startsWith("10.")` matched attacker domains.

### Fixed
- Fixed rate-limit detection on HTTP 200 quota bodies (IPQualityScore & Pulsedive).
- Fixed Android back button handling via `AppBackHandler`.
- Fixed tablet responsive breakpoint deadzone between 720dp and 880dp.
- Fixed settings keystroke persistence on rapid modal dismissal.
