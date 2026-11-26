#!/usr/bin/env node
'use strict';

const path = require('path');
const admin = require('firebase-admin');

const keyPath =
  process.env.GOOGLE_APPLICATION_CREDENTIALS ||
  path.join(__dirname, 'fcm', 'service-account.json');

// eslint-disable-next-line import/no-dynamic-require, global-require
const serviceAccount = require(keyPath);

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: 'https://bhagwat-geeta-f7087-default-rtdb.asia-southeast1.firebasedatabase.app',
  });
}

const db = admin.database();

async function main() {
  await db.ref('subscription_config').set({
    heroTitle: 'Unlock Gita PRO',
    heroSubtitle: 'Deepen your spiritual journey with premium Bhagavad Gita content.',
    benefits: {
      b1: 'Ad-free listening and viewing',
      b2: 'Unlimited shloka audio playback',
      b3: 'HD video quality where available',
      b4: 'Download for offline access (coming soon)',
      b5: 'Priority updates and new chapters',
    },
    updatedAt: Date.now(),
  });

  await db.ref('subscription_plans').set({
    monthly: {
      title: 'Monthly',
      subtitle: 'Flexible, cancel anytime',
      priceLabel: '₹99/mo',
      badge: '',
      active: true,
      sortOrder: 1,
    },
    yearly: {
      title: 'Yearly',
      subtitle: 'Best value — save more',
      priceLabel: '₹699/yr',
      badge: 'BEST VALUE',
      active: true,
      sortOrder: 2,
    },
  });

  console.log('subscription_config + subscription_plans seeded');
}

main().catch((e) => {
  console.error(e.message || e);
  process.exit(1);
});
