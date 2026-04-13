# SMS Permission Compliance Plan (Google Play) - T1D Alert

Last updated: 2026-03-29

## Why this is high risk
Google Play treats SMS permissions as highly restricted.

Official policy pages:
- https://support.google.com/googleplay/android-developer/answer/10208820
- https://support.google.com/googleplay/android-developer/answer/9888076

Key rule summary:
- Apps with SMS permissions are usually expected to be the default SMS/Assistant app.
- If not default handler, only specific exception use-cases are allowed and reviewed.
- If app does not qualify, restricted SMS permissions must be removed from manifest.

## Your app's current SMS permissions
From `app/src/main/AndroidManifest.xml`:
- `android.permission.SEND_SMS`
- `android.permission.RECEIVE_SMS`

## Risk assessment for this app
- `SEND_SMS` may be defensible under emergency alerting use-case.
- `RECEIVE_SMS` is higher risk unless your app clearly meets an eligible exception and can justify it strongly.

## Recommended Play-safe path (for first production release)
1. Keep `SEND_SMS` for emergency outbound alerts.
2. Remove `RECEIVE_SMS` and disable inbound SMS-reading feature for Play release.
3. Keep emergency sending behavior clearly user-initiated/critical and well-disclosed.
4. Submit accurate Permissions Declaration Form and Data Safety answers.

## If you keep RECEIVE_SMS
Prepare for stricter review and possible rejection unless you provide strong exception justification and evidence.

## Required disclosure package
- Privacy Policy (already drafted)
- Data Safety form (already drafted)
- App content declarations (already drafted)
- In-app prominent disclosure before requesting SMS-related permission(s), including:
  - what data is accessed (SMS)
  - why (emergency alert function)
  - how it is used/shared (recipients selected by user)

## Suggested declaration wording (draft)
Use case:
- "App sends emergency SMS alerts during critical low-glucose events and possible unconscious-fall incidents."

Core functionality statement:
- "Without SMS sending, the app cannot perform its core emergency alert feature to notify caregivers/emergency recipients during safety-critical events."

Data handling statement:
- "SMS content is generated for emergency safety alerts and sent only to user-configured recipients. Data is not used for ads or sold."

## Immediate technical action options
- Option A (Recommended): Remove `RECEIVE_SMS` from manifest and skip inbound SMS receiver for Play release.
- Option B: Keep both permissions and proceed with high-review-risk declaration.

## Decision log
Current recommendation: Option A for Play production approval probability.
