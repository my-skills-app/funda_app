# FCM sender (Firebase Admin SDK)

Your file `bhagwat-geeta-f7087-firebase-adminsdk-....json` is **not for the Android app**.  
The app already uses `app/google-services.json` + `PushNotificationHelper` on the device.

Admin SDK JSON is only for **you (admin)** to send notifications from a PC or server.

## Setup (one time)

1. Copy your Admin SDK JSON from Downloads into this folder and rename it:

   `tools/fcm/service-account.json`

   (This path is gitignored — never commit it.)

2. Install dependencies:

   ```bash
   cd tools/fcm
   npm install
   ```

## Send to all users

The app subscribes every device to topic **`all_users`**.

```bash
node send-push.js topic "शीर्षक" "संदेश"
```

## Send to one user

1. Firebase Console → Realtime Database → `users/{uid}/fcmToken`
2. Run:

   ```bash
   node send-push.js token PASTE_FCM_TOKEN_HERE "Title" "Message"
   ```

## Alternative: Firebase Console

Messaging → New campaign works without this script. Admin JSON is optional if you only use the console.

## Security

- Do **not** put `service-account.json` inside `app/` or commit it to Git.
- If this key was shared publicly, regenerate it: Firebase Console → Project settings → Service accounts → Generate new private key.
