#!/usr/bin/env node
/**
 * Create or update web admin user (Firebase Authentication email/password).
 *
 *   node tools/create-admin-user.js
 */
'use strict';

const path = require('path');
const modulePath = path.join(__dirname, 'fcm', 'node_modules', 'firebase-admin');
// eslint-disable-next-line import/no-dynamic-require, global-require
const admin = require(modulePath);

const PROJECT_ID = 'bhagwat-geeta-f7087';
const ADMIN_EMAIL = 'hkumar91864@gmail.com';
const ADMIN_PASSWORD = 'hkumar91864@gmail.com';
const ADMIN_UID = 'dldcxUuFW9Qz00Vh7tBhgydn7qq2';

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
  init();
  const auth = admin.auth();

  let user = null;
  try {
    user = await auth.getUserByEmail(ADMIN_EMAIL);
    console.log('User already exists:', user.uid);
  } catch (err) {
    if (err.code !== 'auth/user-not-found') {
      throw err;
    }
  }

  if (user) {
    await auth.updateUser(user.uid, {
      email: ADMIN_EMAIL,
      password: ADMIN_PASSWORD,
      emailVerified: true,
    });
    console.log('Updated password for', ADMIN_EMAIL, 'uid:', user.uid);
    return;
  }

  try {
    user = await auth.getUser(ADMIN_UID);
    await auth.updateUser(ADMIN_UID, {
      email: ADMIN_EMAIL,
      password: ADMIN_PASSWORD,
      emailVerified: true,
    });
    console.log('Linked email/password to existing admin uid:', ADMIN_UID);
    return;
  } catch (err) {
    if (err.code !== 'auth/user-not-found') {
      throw err;
    }
  }

  user = await auth.createUser({
    email: ADMIN_EMAIL,
    password: ADMIN_PASSWORD,
    emailVerified: true,
  });
  console.log('Created new user:', ADMIN_EMAIL, 'uid:', user.uid);
  console.log('Note: new uid differs from Android admin uid; database rules allow this email.');
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
