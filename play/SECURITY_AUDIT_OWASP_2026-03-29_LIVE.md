# Security Audit Report - T1D Alert (Live Run)

Date: 2026-03-29
Scope: Entire Android application repository
Method: Local secure code review + Android lint + dependency graph + ZAP/Nessus tooling checks

## Tools Executed
- `./gradlew lint`
- `./gradlew app:lintRelease`
- `./gradlew app:dependencies --configuration releaseRuntimeClasspath`
- `"/Applications/ZAP.app/Contents/Java/zap.sh" -version`
- `"/Applications/ZAP.app/Contents/Java/zap.sh" -cmd -help`
- `/Library/Nessus/run/sbin/nessuscli -v`
- `/Library/Nessus/run/sbin/nessus-service -h`

## Executive Summary
Current posture is **medium risk** with **2 high**, **3 medium**, and **2 low** findings. The biggest risks are token exposure through legacy URL auth paths and cryptographic storage downgrade fallback.

## Findings (Ordered by Severity)

### 1) High - Legacy mode sends secrets in URL userinfo/query
- OWASP: A02 (Cryptographic Failures), A04 (Insecure Design)
- Evidence:
  - `app/src/main/java/com/example/t1dalert/CgmUtils.java:53`
  - `app/src/main/java/com/example/t1dalert/CgmUtils.java:58`
  - `app/src/main/java/com/example/t1dalert/CgmRepository.java:61`
- Details:
  - In developer fallback mode, API token is injected into URL userinfo and access token into query (`?token=`).
  - URL-based secrets can leak via logs, intermediary systems, and diagnostics.

### 2) High - Silent fallback from encrypted prefs to plaintext prefs
- OWASP: A02 (Cryptographic Failures), A05 (Security Misconfiguration)
- Evidence:
  - `app/src/main/java/com/example/t1dalert/AppPrefsStore.java:28`
  - `app/src/main/java/com/example/t1dalert/AppPrefsStore.java:29`
- Details:
  - If `EncryptedSharedPreferences` setup fails for any reason, code silently falls back to plaintext `MODE_PRIVATE` prefs.
  - This can unintentionally store API/access tokens and shared keys unencrypted.

### 3) Medium - Settings QR payload is unsigned/plain and auto-applied
- OWASP: A04 (Insecure Design), A08 (Software and Data Integrity Failures)
- Evidence:
  - `app/src/main/java/com/example/t1dalert/SettingsQrCodec.java:41`
  - `app/src/main/java/com/example/t1dalert/Settings.java:313`
  - `app/src/main/java/com/example/t1dalert/SettingsSyncHelper.java:35`
- Details:
  - Settings QR includes high-value fields (`api`, `access`, `shared`) as plain JSON string payload.
  - Imported payloads are not authenticated/signed before being persisted.

### 4) Medium - Broadly exported activity without permission gate
- OWASP: A01 (Broken Access Control), A05 (Security Misconfiguration)
- Evidence:
  - `app/src/main/AndroidManifest.xml:52`
- Details:
  - `LiveCgmActivity` is exported and can be launched by other apps.
  - While not directly exposing raw storage, it increases external invocation surface for sensitive app states.

### 5) Medium - Security hardening gap in release build obfuscation
- OWASP: A05 (Security Misconfiguration)
- Evidence:
  - `app/build.gradle:21`
- Details:
  - `minifyEnabled false` for release keeps bytecode easier to reverse engineer.
  - This increases risk of implementation detail disclosure and abuse scripting.

### 6) Low - Developer mode intentionally permits weaker auth path
- OWASP: A04 (Insecure Design)
- Evidence:
  - `app/src/main/java/com/example/t1dalert/CgmRepository.java:61`
  - `app/src/main/res/values/strings.xml:89`
- Details:
  - Developer mode enables legacy fallback by design.
  - If accidentally enabled in production usage, it downgrades transport secrecy posture.

### 7) Low - Sensitive permission surface remains large
- OWASP: A05 (Security Misconfiguration)
- Evidence:
  - `app/src/main/AndroidManifest.xml:16`
  - `app/src/main/AndroidManifest.xml:21`
  - `app/src/main/AndroidManifest.xml:27`
- Details:
  - `SYSTEM_ALERT_WINDOW`, `RECEIVE_SMS`, `SEND_SMS`, location, etc. are high-impact permissions.
  - This can be justified by product behavior, but it increases abuse and policy risk.

## OWASP Top 10 Mapping (2021)
- A01 Broken Access Control: **Medium concern** (exported activity attack surface)
- A02 Cryptographic Failures: **High concern** (URL secret exposure path + crypto-storage downgrade)
- A03 Injection: **No direct evidence in this code audit**
- A04 Insecure Design: **Medium concern** (legacy auth mode + unsigned sensitive QR import)
- A05 Security Misconfiguration: **Medium concern** (minify disabled, broad permission surface)
- A06 Vulnerable and Outdated Components: **Low concern observed** (`onnxruntime-android` already 1.24.3)
- A07 Identification and Authentication Failures: **Low / not primary architecture risk**
- A08 Software and Data Integrity Failures: **Medium concern** (settings import integrity not enforced)
- A09 Security Logging and Monitoring Failures: **Low/Unknown**
- A10 SSRF: **Not applicable** (no server-side request relay in app)

## ZAP/Nessus Notes
- ZAP binary is present and executable (`2.17.0`).
- Nessus CLI/service are present (`10.11.1`; daemon process present).
- This repository is an Android client app and does not contain a fixed web backend target URL for direct ZAP/Nessus active network scanning.
- Therefore, this run used ZAP/Nessus for tooling verification and focused risk assessment on client code/configuration.

## Priority Fix Plan
1. Remove legacy URL-secret auth path or hard-disable it for production flavor.
2. Replace silent plaintext fallback in `AppPrefsStore` with fail-closed handling + user-visible recovery.
3. Add cryptographic authenticity (signature/MAC) and explicit confirmation step for imported settings QR payloads.
4. Set `LiveCgmActivity` to `exported=false` unless an explicit integration requirement exists.
5. Enable release shrinking/obfuscation (`minifyEnabled true`) and verify behavior with rules.

## Build/Scan Status
- `lint` and `lintRelease`: successful.
- No critical build-time security lint errors surfaced, so key risks here are design/configuration level.
