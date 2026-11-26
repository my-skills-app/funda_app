#!/usr/bin/env node
/**
 * Deploy Realtime Database rules using Firebase Admin service account.
 *
 *   node tools/deploy-database-rules.js
 */
'use strict';

const fs = require('fs');
const path = require('path');
const https = require('https');
const admin = require('firebase-admin');

const PROJECT_ID = 'bhagwat-geeta-f7087';
const DATABASE_HOST =
  'bhagwat-geeta-f7087-default-rtdb.asia-southeast1.firebasedatabase.app';

function init() {
  const keyPath =
    process.env.GOOGLE_APPLICATION_CREDENTIALS ||
    path.join(__dirname, 'fcm', 'service-account.json');
  const serviceAccount = require(keyPath);
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
      databaseURL: `https://${DATABASE_HOST}`,
    });
  }
  return serviceAccount;
}

function putRules(accessToken, rulesJson) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(rulesJson);
    const req = https.request(
      {
        hostname: DATABASE_HOST,
        path: '/.settings/rules.json',
        method: 'PUT',
        headers: {
          Authorization: 'Bearer ' + accessToken,
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(body),
        },
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => {
          data += chunk;
        });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            resolve(data || 'OK');
            return;
          }
          reject(new Error('HTTP ' + res.statusCode + ': ' + data));
        });
      }
    );
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  const serviceAccount = init();
  const rulesPath = path.join(__dirname, '..', 'database.rules.json');
  const rules = JSON.parse(fs.readFileSync(rulesPath, 'utf8'));
  const token = await admin.credential.cert(serviceAccount).getAccessToken();
  await putRules(token.access_token, rules);
  console.log('Database rules deployed to', DATABASE_HOST);
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
