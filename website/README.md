# Website (landing page + admin)

**Separate from the Android app** — all web files live here. Do not put these inside `app/` (Gradle/Android code).

```
MoviesTest/
├── app/                    ← Android app only (Java, XML, Gradle)
├── website/                ← This folder — public site + /admin
│   ├── index.html          Landing page
│   ├── admin/
│   │   └── index.html      Admin dashboard (/admin)
│   ├── css/
│   │   └── styles.css
│   └── js/
│       ├── brand.js        App logo URL (ImgBB)
│       ├── screenshots.js  Screenshot URLs (ImgBB)
│       ├── firebase-config.js
│       ├── landing.js
│       └── admin.js
├── tools/                  Scripts to upload images, create admin user
└── firebase.json           Hosting points to website/
```

## URLs

| Page | Path |
|------|------|
| Landing | `/` |
| Admin (hidden from nav) | `/admin` |

## Local preview

```bash
cd website
npx --yes serve .
```

- Home: http://localhost:3000  
- Admin: http://localhost:3000/admin  

## Deploy (Firebase Hosting)

```bash
firebase deploy --only hosting
```

## Upload assets

```bash
node tools/upload-app-logo.js
node tools/upload-landing-images.js
```

## Admin login

- URL: `/admin`
- Email: `hkumar91864@gmail.com`
- Password: (set in Firebase — see `tools/create-admin-user.js`)
