# EasyLink Care — pre-launch checklist

Play Store requirements and product gaps that must close before EasyLink
Care ships. (The launcher's own submission checklist is separate — this
is only what Care needs.)

## Play policy requirements

- [ ] **In-app account deletion** — Care users sign in with email, which
  makes it an "account" under Play's Data deletion policy. Needs a
  "Delete my account" flow that removes the Firebase Auth user, their
  uid from every `links/{linkId}.caregiverUids`, and any link trees
  where they were the only caregiver. Decide what happens to a paired
  elder's data when the last caregiver deletes their account.
- [ ] **Web deletion resource** — page at easylinkcare.com/delete-account
  (instructions + contact email satisfies the policy; a request form is
  nicer). URL goes in Care's Play Console Data safety form.
- [ ] **Data safety form** — Care collects: caregiver email (account),
  elder config/status/events (Firestore), crash logs (Crashlytics).
- [x] **Privacy policy link in-app** — pairing screen + dashboard footer
  (shared `PrivacyPolicyLink` composable).

## Support tooling

- [ ] **linkId lookup affordance** — the elder's launcher auth is
  anonymous, so a deletion/support email cannot be matched to a
  `links/{linkId}` document by identity alone. Add a way to read the
  linkId in-app (e.g. small "Support code" line on the launcher's
  Connect Family screen and/or Care's dashboard) so a user can quote it
  when asking for their data to be deleted.
- [ ] **Single deletion routine** — one operation that takes a linkId and
  deletes the entire tree (`config`, `status`, `events`, the link doc).
  Used by both support requests and the account-deletion flow. Becomes
  mandatory-not-optional once medication sync puts med data in
  Firestore, which changes the answer to "delete my medication data"
  from "it's only on your phone" to "we hold a copy."

## Product gaps (dashboard tiles currently "coming soon")

- [ ] Medication syncing + adherence view
- [ ] Alerts (FCM push for SOS/fall events)

## Firebase/infra

- [ ] If Care gets its own Play listing: Play App Signing mints a new
  SHA-1 → register in Firebase + the restricted Android API key
  (same step the launcher needs at its first upload).
