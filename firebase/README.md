# Firebase assets

Server-side configuration for the EasyLink apps, kept in the repo so it's
versioned alongside the code that depends on it.

| File | What it is |
|------|------------|
| `remoteconfig.template.json` | Remote Config parameters — the server-tunable **default** setting values. Keys mirror [`RemoteConfigKeys`](../shared/src/main/java/com/fangjet/shared/config/RemoteConfigKeys.kt). |

## Deploying Remote Config

Once the Firebase project exists and the Firebase CLI is set up (`firebase login`,
`firebase use <project-id>`):

```bash
firebase deploy --only remoteconfig
```

Point `firebase.json` at the template:

```json
{ "remoteconfig": { "template": "firebase/remoteconfig.template.json" } }
```

Or, without the CLI: Firebase console → **Remote Config** → **⋮** → *Publish from
file* → pick `remoteconfig.template.json`.

## How the app uses it

The app never blocks on a fetch. On a fresh install it starts from
[`SettingsDefaults.HARDCODED`](../shared/src/main/java/com/fangjet/shared/config/SettingsDefaults.kt),
reads the last activated Remote Config values on cold start, and refreshes in the
background on launch. Every value is validated/clamped, so a bad console entry can
never break the app — it just falls back to the hardcoded default.

Changing a default here only moves the starting point for people who *haven't* yet
chosen for themselves; it never overrides an existing user or caregiver choice.
