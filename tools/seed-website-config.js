#!/usr/bin/env node
/**
 * Set landing page download URL in Realtime Database (website_config/downloadUrl).
 *
 *   node tools/seed-website-config.js
 *   node tools/seed-website-config.js "https://example.com/app.apk"
 */
'use strict';

const path = require('path');
const admin = require('firebase-admin');

const PROJECT_ID = 'bhagwat-geeta-f7087';
const DEFAULT_URL =
  'https://github.com/my-skills-app/My-Skills--Realtime-Database-/raw/refs/heads/main/apk/file/bhagavatgita.examsaathi.apk';

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
  const url = (process.argv[2] || DEFAULT_URL).trim();
  if (!/^https?:\/\//i.test(url)) {
    throw new Error('URL must start with http:// or https://');
  }
  init();
  await admin.database().ref('website_config/downloadUrl').set(url);
  console.log('website_config/downloadUrl set to:');
  console.log(url);
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
