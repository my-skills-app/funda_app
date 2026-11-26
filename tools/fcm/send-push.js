#!/usr/bin/env node
/**
 * Send push via Firebase Admin SDK (same project as app: bhagwat-geeta-f7087).
 *
 * Topic (all devices subscribed in app):  node send-push.js topic "Title" "Body"
 * Single device:                          node send-push.js token <fcmToken> "Title" "Body"
 *
 * Credentials (pick one):
 *   - Copy your Admin SDK JSON to tools/fcm/service-account.json (gitignored), or
 *   - set GOOGLE_APPLICATION_CREDENTIALS to the full path of that JSON file.
 */
'use strict';

const path = require('path');
const admin = require('firebase-admin');

const TOPIC_DEFAULT = 'all_users';
const CHANNEL_ID = 'app_push_hidden';

function resolveServiceAccountPath() {
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    return process.env.GOOGLE_APPLICATION_CREDENTIALS;
  }
  return path.join(__dirname, 'service-account.json');
}

function initAdmin() {
  const keyPath = resolveServiceAccountPath();
  // eslint-disable-next-line import/no-dynamic-require, global-require
  const serviceAccount = require(keyPath);
  if (serviceAccount.project_id !== 'bhagwat-geeta-f7087') {
    console.warn(
      'Warning: service account project_id is',
      serviceAccount.project_id,
      '(expected bhagwat-geeta-f7087)'
    );
  }
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
  }
}

function buildMessage(title, body, target) {
  const notification = { title, body };
  const android = {
    priority: 'high',
    notification: { channelId: CHANNEL_ID },
  };
  if (target.type === 'topic') {
    return { notification, android, topic: target.value };
  }
  return { notification, android, token: target.value };
}

async function main() {
  const mode = (process.argv[2] || '').toLowerCase();
  if (mode !== 'topic' && mode !== 'token') {
    console.error(
      'Usage:\n' +
        '  node send-push.js topic ["Title"] ["Body"]\n' +
        '  node send-push.js token <fcmToken> ["Title"] ["Body"]\n\n' +
        'Put Admin SDK JSON at tools/fcm/service-account.json or set GOOGLE_APPLICATION_CREDENTIALS.'
    );
    process.exit(1);
  }

  let target;
  let title;
  let body;
  if (mode === 'topic') {
    target = { type: 'topic', value: TOPIC_DEFAULT };
    title = process.argv[3] || 'Bhagavad Gita';
    body = process.argv[4] || 'New update for you';
  } else {
    const token = process.argv[3];
    if (!token) {
      console.error('Missing FCM token. Get it from Firebase RTDB: users/{uid}/fcmToken');
      process.exit(1);
    }
    target = { type: 'token', value: token };
    title = process.argv[4] || 'Bhagavad Gita';
    body = process.argv[5] || 'New update for you';
  }

  initAdmin();
  const message = buildMessage(title, body, target);
  const id = await admin.messaging().send(message);
  console.log('Sent successfully:', id);
  if (target.type === 'topic') {
    console.log('Topic:', target.value);
  }
}

main().catch((err) => {
  console.error('Send failed:', err.message || err);
  process.exit(1);
});
