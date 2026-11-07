### Summary of Changes
<!-- Provide a clear, high-level summary of what this PR does -->

### Motivation & Context
<!-- Why is this change required? What problem does it solve? If it fixes an open issue, link it here: Fixes # -->

### Changes Checklist
- [ ] Tests added/updated (parsing, scoring, Privacy Guard)
- [ ] Static verification & tests passed (`./gradlew check`)
- [ ] Shared logic placed in `shared/`; platform code restricted to platform APIs
- [ ] Secrets handled via secure store abstraction (no hardcoded keys)
- [ ] **Privacy-first compliant**: No telemetry, analytics, or hidden background network calls
- [ ] Docs and `CHANGELOG.md` updated if changes are user-facing

### Platform Testing Performed
- [ ] Windows 10/11 (.msi / Desktop)
- [ ] Linux (Ubuntu / Debian .deb)
- [ ] macOS (Intel / Apple Silicon .dmg)
- [ ] Android (API 24 to 37)
- [ ] iOS (Simulator / Physical device)
