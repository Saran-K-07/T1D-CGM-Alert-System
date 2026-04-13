# Google Play Content Forms Draft - T1D Alert

Last updated: 2026-03-29

Use this as the source of truth while completing Play Console "App content" sections.

## 1) Privacy Policy
- Status: Draft created at `play/PRIVACY_POLICY.md`
- Action: Host publicly (for example on GitHub Pages or your website) and paste URL in Play Console.

## 2) App Access
Suggested response:
- App does not require a developer-provided username/password account.
- Core emergency flow depends on user-supplied Nightscout endpoint/tokens and local permissions.

Reviewer note template:
- "After install, grant requested permissions. Open Settings to configure Nightscout URL/API/access token. Low-glucose and emergency SMS behaviors are permission- and configuration-dependent."

## 3) Ads Declaration
Suggested response:
- "No, this app does not contain ads."

## 4) Content Rating
Suggested response profile (to complete questionnaire accurately):
- Medical/safety utility app.
- Contains emergency alert language and potentially distressing medical context.
- No gambling, sexual content, or user-generated social content.

## 5) Target Audience
Suggested response:
- Adults (18+), caregivers, and patients using medical-alert workflows.
- Not designed primarily for children.

## 6) News Apps
Suggested response:
- "No, this is not a news app."

## 7) Sensitive Permissions / Declarations (Critical)
Your manifest includes sensitive permissions requiring careful justification:
- SEND_SMS
- RECEIVE_SMS
- SYSTEM_ALERT_WINDOW
- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
- READ_CONTACTS (declared)

Action items:
- [ ] Complete relevant Play permissions declaration forms.
- [ ] Ensure declarations exactly match in-app behavior.
- [ ] Remove unused sensitive permissions (recommended: verify if `READ_CONTACTS` is truly needed).

## 8) Health / Medical Positioning
Recommended store disclosure text:
- App is a monitoring and emergency assistance utility.
- Not a substitute for professional medical diagnosis or treatment.
- In emergencies, contact local emergency services immediately.

## 9) Consistency Checks Before Submission
- [ ] Play listing text, privacy policy, and Data Safety answers are consistent.
- [ ] All requested permissions are explained in-store and in-policy.
- [ ] SMS/location usage is clearly tied to emergency functionality.
