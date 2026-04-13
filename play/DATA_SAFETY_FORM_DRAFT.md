# Google Play Data Safety Draft - T1D Alert

Last updated: 2026-03-29

Use this as a draft while filling the Play Console Data safety form. Validate each answer with legal/compliance review before submission.

## 1) Does your app collect or share user data?
Suggested answer: **Yes**

Reasoning:
- The app processes user data for core emergency functionality.
- Emergency SMS sends data to user-configured recipients.

## 2) Data Types Mapping (Draft)

| Play Category | Data in This App | Collected | Shared | Purpose |
|---|---|---|---|---|
| Personal info | Phone number (user and emergency contacts) | Yes | Yes (via SMS to user-selected recipients) | App functionality, Safety & emergency |
| Health and fitness | Glucose values/trends, low-glucose emergency state | Yes | Yes (included in emergency SMS) | App functionality, Safety & emergency |
| Location | Last known location link for critical alerts | Yes (if permission granted) | Yes (in high-priority emergency SMS) | App functionality, Safety & emergency |
| Messages | Emergency SMS body (sent), trusted incoming alert content (received) | Yes | Yes (outgoing SMS) | App functionality, Safety & emergency |
| App info and performance | Local failure counters/metrics | Yes (on-device) | No (based on current code) | App functionality, Diagnostics |

Notes:
- No ads SDK found in current dependencies.
- No third-party analytics SDK found in current dependencies.

## 3) Security Practices (Draft)

- Data encrypted in transit: **Review before selecting Yes**.
  - SMS transport is carrier-controlled and not end-to-end encrypted by default.
  - Nightscout URL is user-provided and may be HTTP or HTTPS.
- Data deletion request support: **Yes (user-controlled)** via edit/remove data, clear app storage, uninstall.

## 4) Processing Purposes (Draft)
For each declared data type, primary purpose is:
- **App functionality**
- **Safety & emergency response**

Optional secondary purpose where relevant:
- **Diagnostics** (local operational counters)

## 5) Data Handling Clarifications To Keep Consistent
- Do not claim advertising use.
- Do not claim data sale.
- Do not claim broad third-party sharing.
- Clearly state that emergency sharing is to user-configured recipients for safety use.

## 6) Final Console Checklist
- [ ] Data safety answers match real runtime behavior.
- [ ] Privacy policy URL points to a public hosted policy.
- [ ] Permission declarations and Data safety answers are consistent.
- [ ] If behavior changes, update this file and Play Console answers together.
