# Security Audit Report (Post-Patch) - T1D Alert

Date: 2026-03-29
Scope: Entire Android app repository (post-remediation state)

## Tool Execution
- ZAP app executed: `/Applications/ZAP.app/Contents/Java/zap.sh -version` -> `2.17.0`.
- Nessus CLI executed: `/Library/Nessus/run/sbin/nessuscli --version` -> `10.11.1`.
- Nessus service scan execution blocked by root/service permissions in this environment.
- Android release lint executed: `./gradlew app:lintRelease` (success, 0 errors).
- Dependency audit executed: `./gradlew app:dependencies --configuration releaseRuntimeClasspath`.

## Findings (Ordered by Severity)

### 1) High - Access token still sent in URL query
- OWASP: A02 Cryptographic Failures, A04 Insecure Design
- Evidence:
  - `app/src/main/java/com/example/t1dalert/CgmUtils.java` (`?token=` in primary URL builder)
- Risk:
  - Query tokens can leak through logs, intermediaries, and telemetry.
- Recommended fix:
  - Prefer header-only auth when Nightscout instance supports it.
  - Keep URL token only as explicit opt-in fallback.

### 2) Medium - Developer Mode enables legacy URL credential pattern
- OWASP: A04 Insecure Design, A05 Security Misconfiguration
- Evidence:
  - `app/src/main/java/com/example/t1dalert/CgmRepository.java` (legacy fallback path)
  - `app/src/main/java/com/example/t1dalert/CgmUtils.java` (legacy URL includes userinfo + token)
  - `app/src/main/java/com/example/t1dalert/Settings.java` + `KEY_DEVELOPER_MODE`
- Risk:
  - If enabled, security posture downgrades intentionally.
- Recommended fix:
  - Keep Developer Mode off in production.
  - Add a clear in-app warning banner while enabled.

### 3) Medium - SMS permission attack/policy surface remains high
- OWASP: A05 Security Misconfiguration
- Evidence:
  - `SEND_SMS` / `RECEIVE_SMS` in manifest.
- Risk:
  - Abuse potential and strict Play policy scrutiny remain.
- Recommended fix:
  - Use product-flavor split: Play flavor without `RECEIVE_SMS` if feasible.
  - Keep strict sender validation and explicit user disclosures.

### 4) Low-Medium - Backup enabled but now controlled
- OWASP: A05 Security Misconfiguration
- Evidence:
  - `allowBackup=true` in manifest.
  - Sensitive prefs excluded in backup/data extraction XML.
- Status:
  - Previously high risk; now substantially reduced.
- Recommended fix:
  - Consider `allowBackup=false` for maximum hardening.

### 5) Low - Pre-release crypto dependency version
- OWASP: A06 Vulnerable and Outdated Components
- Evidence:
  - `androidx.security:security-crypto:1.1.0-alpha06`.
- Risk:
  - Alpha dependency in production path.
- Recommended fix:
  - Move to stable `1.1.0`.

## Verified Fixes Since Prior Audit
- Secrets now routed through encrypted prefs wrapper (`EncryptedSharedPreferences`).
- Inbound SMS trust default changed to fail-closed when allowlist is empty.
- Missing location permission guard fixed before single-update location request.
- ONNX runtime upgraded to `1.24.3`.
- Release lint now has 0 errors.

## OWASP Top 10 Mapping (Current State)
- A01 Broken Access Control: Low-Medium (improved; sender allowlist now fail-closed)
- A02 Cryptographic Failures: Medium-High (token in URL still present)
- A03 Injection: No direct evidence
- A04 Insecure Design: Medium (developer fallback mode + token strategy)
- A05 Security Misconfiguration: Medium (sensitive permissions; backup still enabled)
- A06 Vulnerable/Outdated Components: Low-Medium (alpha crypto dep)
- A07 Identification/Auth Failures: Low (not core app auth model)
- A08 Software/Data Integrity Failures: No direct evidence
- A09 Security Logging/Monitoring Failures: Unknown/Low
- A10 SSRF: Not applicable to local app architecture

## Practical Next Steps
1. Add a strict production flavor that disables Developer Mode and legacy fallback.
2. Move auth to header-only where server supports it; require explicit opt-in for URL token mode.
3. Replace `security-crypto` alpha with stable release.
4. Decide final Play strategy for `RECEIVE_SMS` permission.
