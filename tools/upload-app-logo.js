#!/usr/bin/env node
/**
 * Upload app logo to ImgBB and write website/js/brand.js
 *
 *   node tools/upload-app-logo.js [path-to-logo]
 */
'use strict';

const fs = require('fs');
const path = require('path');
const https = require('https');

const IMGBB_KEY = process.env.IMGBB_API_KEY;
const DEFAULT_LOGO = process.env.LOGO_PATH
  || 'C:\\Users\\apple\\Downloads\\app images screen shorts\\photo_6188209225796882320_y.jpg';
const OUT_FILE = path.join(__dirname, '..', 'website', 'js', 'brand.js');

function uploadBase64(base64, name) {
  return new Promise((resolve, reject) => {
    const body = new URLSearchParams({ key: IMGBB_KEY, image: base64, name }).toString();
    const req = https.request(
      {
        hostname: 'api.imgbb.com',
        path: '/1/upload',
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Content-Length': Buffer.byteLength(body),
        },
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          try {
            const json = JSON.parse(data);
            if (!json.success) {
              reject(new Error(json.error?.message || 'Upload failed'));
              return;
            }
            resolve(json.data.display_url || json.data.url);
          } catch (e) {
            reject(e);
          }
        });
      }
    );
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  if (!IMGBB_KEY) {
    throw new Error('Set IMGBB_API_KEY environment variable before running.');
  }
  const logoPath = process.argv[2] || DEFAULT_LOGO;
  if (!fs.existsSync(logoPath)) {
    throw new Error('Logo not found: ' + logoPath);
  }
  const base64 = fs.readFileSync(logoPath).toString('base64');
  const url = await uploadBase64(base64, 'bhagavat-gita-logo.jpg');
  const js =
    '/** App logo on ImgBB — auto-upload via tools/upload-app-logo.js */\n'
    + 'window.APP_LOGO_URL = '
    + JSON.stringify(url)
    + ';\n';
  fs.writeFileSync(OUT_FILE, js, 'utf8');
  console.log('Logo uploaded:', url);
  console.log('Wrote', OUT_FILE);
}

main().catch((err) => {
  console.error(err.message || err);
  process.exit(1);
});
