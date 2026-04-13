# Security Audit Report - T1D Alert

Date: 2026-03-29
Scope: Entire Android application source in this repository
Method: Local static review + Android lint + dependency inspection

## Tools Attempted
- ZAP: not installed in PATH (`zap.sh` / `zap` not found)
- Nessus: only `/Applications/Nessus/Nessus Client.url` found, no local CLI scanner binary
- Android lint: executed (`./gradlew app:lintDebug`)
- Gradle dependency tree: executed (`./gradlew app:dependencies --configuration releaseRuntimeClasspath`)
- Manual secure-code grep and review: executed

## Executive Summary
Critical security posture is **moderate risk** with several high-priority issues focused on:
- sensitive data exposure patterns (tokens in URL, plaintext local storage, backup exposure)
- trust defaults for inbound emergency SMS
- outdated native dependency version

No evidence of classic code injection or unsafe WebView patterns was found.

## Findings (Ordered by Severity)

### 1) High - Credentials included directly in URL construction
- OWASP: A02 Cryptographic Failures, A04 Insecure Design
- Evidence: `CgmUtils.buildNightscoutEntriesUrl` injects API token into URL authority and access token into query string
  - `app/src/main/java/com/example/t1dalert/CgmUtils.java:43-49`
- Why this matters:
  - URL tokens can leak via logs, proxies, analytics, crash reports, browser history-style captures, or intermediary systems.
- Recommendation:
  - Move credentials to headers (`Authorization`) instead of URL/userinfo/query.
  - Do not embed secrets in URL strings at all.

### 2) High - Sensitive secrets stored in plaintext SharedPreferences
- OWASP: A02 Cryptographic Failures
- Evidence:
  - API/access tokens and phone number stored in SharedPreferences:
    - `app/src/main/java/com/example/t1dalert/MainActivity.java:109-113`
    - `app/src/main/java/com/example/t1dalert/Settings.java:161-166`
  - Shared alert key stored in SharedPreferences:
    - `app/src/main/java/com/example/t1dalert/AlertKeyManager.java:24`
- Why this matters:
  - Secrets are retrievable from app data on compromised/debuggable/rooted devices.
- Recommendation:
  - Use `EncryptedSharedPreferences` and Android Keystore-backed master key.
  - Separate operational non-secret prefs from secret material.

### 3) High - Backup/data extraction currently open with sensitive prefs likely included
- OWASP: A05 Security Misconfiguration, A02 Cryptographic Failures
- Evidence:
  - `android:allowBackup="true"`:
    - `app/src/main/AndroidManifest.xml:32`
  - Backup/data extraction rules are default template with no excludes:
    - `app/src/main/res/xml/data_extraction_rules.xml:6-12`
    - `app/src/main/res/xml/backup_rules.xml:8-12`
- Why this matters:
  - Sensitive local data can be included in cloud/device transfer backups unless explicitly excluded.
- Recommendation:
  - Disable backup for production, or explicitly exclude shared prefs containing secrets.

### 4) Medium - Trust model allows any sender when trusted list is empty
- OWASP: A01 Broken Access Control, A04 Insecure Design
- Evidence:
  - `isTrustedSender` returns true for empty allowlist:
    - `app/src/main/java/com/example/t1dalert/IncomingAlertSmsReceiver.java:82-84`
- Why this matters:
  - Any SMS with matching emergency prefix can trigger high-urgency alerts until allowlist is configured.
- Recommendation:
  - Fail-closed default: require explicit trusted sender setup before accepting inbound alerts.

### 5) Medium - Location single-update call lacks explicit per-call permission guard (lint error)
- OWASP: A05 Security Misconfiguration (robustness/security boundary handling)
- Evidence:
  - `requestSingleUpdate` call flagged MissingPermission by lint:
    - `app/src/main/java/com/example/t1dalert/FallDetectionService.java:414`
- Why this matters:
  - Runtime permission revocation edge cases can cause unexpected behavior/failures.
- Recommendation:
  - Add explicit permission checks inside `requestSingleUpdateIfPossible` before requesting updates.

### 6) Medium - Outdated native runtime dependency
- OWASP: A06 Vulnerable and Outdated Components
- Evidence:
  - `com.microsoft.onnxruntime:onnxruntime-android:1.19.2` in build file
    - `app/build.gradle:39`
  - Lint reports newer available version (1.24.3)
- Why this matters:
  - Older native libraries increase risk of known CVEs and compatibility issues.
- Recommendation:
  - Upgrade ONNX Runtime and retest ML path.

### 7) Low - Broad sensitive permissions increase attack and policy surface
- OWASP: A05 Security Misconfiguration
- Evidence:
  - Sensitive permissions in manifest:
    - `app/src/main/AndroidManifest.xml:16-28`
- Why this matters:
  - Broader permission surface increases abuse impact and review/policy risk.
- Recommendation:
  - Keep least-privilege permissions only; strip nonessential ones per release flavor.

## OWASP Top 10 Mapping Summary

- A01 Broken Access Control: **Medium concern** (inbound SMS trust-open default)
- A02 Cryptographic Failures: **High concern** (plaintext prefs, URL token exposure)
- A03 Injection: **No direct evidence** in current code scan
- A04 Insecure Design: **Medium concern** (token-in-URL design, trust defaults)
- A05 Security Misconfiguration: **High concern** (backup defaults + sensitive permissions)
- A06 Vulnerable and Outdated Components: **Medium concern** (ONNX runtime lag)
- A07 Identification and Authentication Failures: **Low/Not primary** in current architecture
- A08 Software and Data Integrity Failures: **No direct evidence** from static scan
- A09 Security Logging and Monitoring Failures: **Low/Unknown** (no centralized security telemetry)
- A10 Server-Side Request Forgery: **Not applicable/low** (no server-side request broker in app)

## Prioritized Remediation Plan
1. Remove secrets from URLs; switch to header auth.
2. Migrate secret storage to EncryptedSharedPreferences.
3. Lock down backup/data extraction for secret prefs.
4. Make inbound SMS trust fail-closed by default.
5. Fix location permission guard lint error.
6. Upgrade ONNX runtime and rerun lint/tests.

## Audit Limitations
- Dynamic DAST with ZAP/Nessus was not runnable due missing local scanner executables/CLI.
- No running backend service in this repo to target with web scanner probes.
- Findings are based on local source + lint + dependency inspection only.
