# Firebase Cloud Functions — push notifications

App writes to `push_broadcasts` → function `onPushBroadcastQueued` sends FCM to topic `all_users`.

## Quick deploy (from project root)

```bash
cd e:\MoviesTest
npm install
npm run firebase:login
npm run firebase:deploy:all
```

Requires **Blaze** plan on `bhagwat-geeta-f7087`.

## Fix DEVELOPER_ERROR in Logcat

Add debug SHA-1 in Firebase Console → Project settings → Your apps → Android → Add fingerprint:

`37:36:16:B8:CA:00:F2:66:4C:35:64:4F:9B:CE:06:53:99:68:12:AF`

Then **re-download** `google-services.json` into `app/` (must include `oauth_client` with `"client_type": 1`).

## Error: Permission denied / codebase / 403

### A) App se "Send notification" fail (Realtime Database)

Console → **Realtime Database** → **Rules** → puri file replace karo ya `database.rules.json` se copy karo.

Zaroori block:

```json
"push_broadcasts": {
  ".read": "auth != null",
  "$pushId": {
    ".read": "auth != null",
    ".write": "auth != null && auth.uid === 'dldcxUuFW9Qz00Vh7tBhgydn7qq2'",
    ".validate": "newData.child('createdBy').val() === auth.uid"
  }
}
```

**Publish** dabao. App mein dubara login karke try karo.

### Push image upload (ImgBB key in Firebase)

Realtime Database path **`admin_config`** — sirf admin UID read/write (see `database.rules.json`).

```json
"admin_config": {
  "adminUid": "dldcxUuFW9Qz00Vh7tBhgydn7qq2",
  "imgbbApiKey": "YOUR_IMGBB_API_KEY"
}
```

Seed once from PC (service account):

```bash
node tools/seed-admin-config.js
```

Rules deploy: `firebase deploy --only database`

Rules deploy (CLI):

```bash
firebase login
firebase use bhagwat-geeta-f7087
firebase deploy --only database
```

---

### B) `firebase deploy --only functions` par 403 / permission

Aapke Google account ko project par ye roles chahiye (Owner add kare):

1. [Google Cloud IAM](https://console.cloud.google.com/iam-admin/iam?project=bhagwat-geeta-f7087)
2. Apni email → **Edit** → add:
   - **Firebase Admin** (or Editor)
   - **Cloud Functions Admin**
   - **Service Account User**

Phir:

```bash
firebase logout
firebase login
cd e:\MoviesTest\functions
npm install
cd ..
firebase deploy --only functions
```

- Project **Blaze (pay-as-you-go)** plan par hona chahiye.
- Agar `Unknown service account` aaye: Console → App Engine → ek baar enable karo.

Debug:

```bash
firebase deploy --only functions --debug
```

---

### C) Functions deploy ke bina push bhejna (PC)

```bash
cd e:\MoviesTest\tools\fcm
node send-push.js topic "Title" "Message"
```

(App mein **This device only** test ke liye kaam karta hai bina deploy ke.)
