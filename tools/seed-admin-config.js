#!/usr/bin/env node
/**
 * One-time: store ImgBB key in Realtime Database (admin_config).
 * Uses tools/fcm/service-account.json (Admin SDK bypasses RTDB rules).
 *
 *   node tools/seed-admin-config.js
 */
'use strict';

const path = require('path');
const admin = require('firebase-admin');

const PROJECT_ID = 'bhagwat-geeta-f7087';
const ADMIN_UID = 'dldcxUuFW9Qz00Vh7tBhgydn7qq2';
const IMGBB_KEY = process.env.IMGBB_API_KEY;

function init() {
  const keyPath =
    process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    path.join(__dirname, 'fcm', 'service-account.json');
  // eslint-disable-next-line import/no-dynamic-require, global-require
  const serviceAccount = require(keyPath);
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: `https://${PROJECT_ID}-default-rtdb.asia-southeast1.firebasedatabase.app`,
    });
  }
}

async function main() {
  if (!IMGBB_KEY) {
    throw new Error('Set IMGBB_API_KEY environment variable before running.');
  }
  init();
  await admin.database().ref('admin_config').set({
    adminUid: ADMIN_UID,
    imgbbApiKey: IMGBB_KEY,
    updatedAt: Date.now(),
  });
  console.log('admin_config written (adminUid + imgbbApiKey)');
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
