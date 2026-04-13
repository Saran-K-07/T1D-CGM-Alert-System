# Privacy Policy for T1D Alert

Last updated: 2026-03-29

## 1) Overview
T1D Alert helps users monitor glucose values and send emergency alerts during low-glucose and potential unconscious-fall situations. This policy explains what data the app processes, why it is processed, and how users can control it.

## 2) Data We Process
The app may process the following data categories:

- User-entered configuration: Nightscout URL, API token, access token, user phone number.
- Emergency contacts: phone numbers and contact names entered/scanned by the user.
- Health-related data: glucose readings (SGV), trend direction, and prediction output shown in-app.
- Location data: last known location used in high-priority emergency alerts.
- Messaging data: emergency SMS content sent by the app and trusted incoming emergency SMS content.
- Diagnostic counters: local app metrics such as SMS/decrypt failures.

## 3) How Data Is Used
We process data only to provide core app functions:

- Fetch and display glucose readings.
- Detect critical low-glucose events and trigger emergency alerts.
- Detect potential unconscious-fall events and escalate emergency messaging.
- Send emergency SMS alerts to user-configured recipients.
- Display incoming trusted emergency alerts.

## 4) Data Sharing
- The app sends emergency SMS messages to recipients explicitly configured by the user.
- The app does not include third-party ads SDKs.
- The app does not use third-party analytics SDKs.
- The app does not sell user data.

## 5) Data Storage and Retention
- App settings and operational state are stored locally on-device (SharedPreferences).
- Data remains until the user edits/removes it in-app, clears app storage, or uninstalls the app.

## 6) Security
- The app uses a shared alert-key mechanism for secure alert message encoding/decoding between trusted instances.
- Some emergency escalation messages can be sent as plain SMS (carrier transport).
- Users should configure secure endpoints (HTTPS) for Nightscout URLs where possible.

## 7) Permissions
The app may request permissions including (as applicable):

- SEND_SMS, RECEIVE_SMS
- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
- SYSTEM_ALERT_WINDOW
- POST_NOTIFICATIONS
- CAMERA
- READ_CONTACTS

Permissions are requested to enable emergency and setup features. If denied, related functionality may be limited.

## 8) Children's Privacy
This app is intended for emergency diabetes monitoring workflows and is not directed to children under 13.

## 9) Your Choices
You can:

- Edit/remove app settings and contacts in the app.
- Revoke permissions from Android Settings.
- Clear app storage or uninstall the app to remove local data.

## 10) Contact
For privacy questions or requests, contact:

- Name: <YOUR_NAME_OR_ORG>
- Email: <YOUR_SUPPORT_EMAIL>
- Address: <OPTIONAL_POSTAL_ADDRESS>

## 11) Changes To This Policy
We may update this policy from time to time. The "Last updated" date will reflect the latest revision.
