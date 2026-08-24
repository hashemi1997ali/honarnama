# 🎨 Honarnama Market

Honarnama is a university artwork marketplace consisting of a native Android application, a PHP REST API, an AngularJS administration panel, and a PostgreSQL database.

The project was originally presented in Dey 1399 and has since been cleaned, modernized, and migrated to Neon/PostgreSQL for portfolio and educational use.

---

## ✨ Features

### Android Application

- Browse and search artwork, categories, and art news
- View product details and image galleries
- Customer registration and login
- Local wishlist, shopping cart, and order history
- Submit orders to the backend
- Browse auctions and place bids

### Administration Panel

- Dashboard for products, orders, categories, and news
- Product, category, news, and order management
- Application, currency, tax, shipping, and email settings
- Administrator profile and password management

### Backend

- PHP REST API with parameterized PDO queries
- Neon/PostgreSQL with pooled runtime connections
- Repeatable database migration and initial administrator setup
- Optional SMTP order notifications
- Local development without requiring Apache

---

## 🛠️ Technology Stack

| Part | Technologies |
| --- | --- |
| Android | Java 17, AndroidX, Material Components, Retrofit, Gson, Glide |
| Admin panel | AngularJS 1.8.3, AngularJS Material, HTML, CSS |
| Backend | PHP 8.2+, PDO PostgreSQL |
| Database | Neon/PostgreSQL |
| Automation | GitHub Actions, Gradle |

---

## 📁 Project Structure

```text
honarnama/
├── Market/                  # Android application
├── panel/                   # Admin panel and PHP API
│   ├── database/            # PostgreSQL schema and migration
│   ├── services/            # Backend services
│   └── uploads/             # Ignored runtime uploads
├── .github/workflows/       # Continuous integration
├── .env.example             # Environment template
└── dev-router.php           # Local PHP router
```

---

## 🚀 Getting Started

### Requirements

- PHP 8.2 or later with `PDO` and `pdo_pgsql`
- A Neon/PostgreSQL database
- Android Studio, JDK 17, and Android SDK 37

### Backend and Admin Panel

Create the local environment file:

```bash
cp .env.example .env
```

Set the following values in `.env`:

```env
DATABASE_URL=postgresql://USER:PASSWORD@POOLED-ENDPOINT/DATABASE?sslmode=require
DATABASE_URL_UNPOOLED=postgresql://USER:PASSWORD@DIRECT-ENDPOINT/DATABASE?sslmode=require
SECURITY_CODE=replace-with-a-random-local-value

ADMIN_NAME=Administrator
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=replace-with-at-least-12-characters
```

Use the pooled Neon URL for regular API traffic and the direct URL for migration. Never place database credentials inside the Android application.

Prepare the database and start the local server:

```bash
php panel/database/migrate.php
php -S 127.0.0.1:8000 dev-router.php
```

- Admin panel: `http://127.0.0.1:8000/panel/`
- API: `http://127.0.0.1:8000/panel/services/`

Test the database connection:

```bash
curl http://127.0.0.1:8000/panel/services/checkResponse
```

Running the migration again does not overwrite the password of an existing administrator.

### Android Application

Open the `Market/` directory in Android Studio and add these values to `Market/local.properties`:

```properties
BACKEND_URL=http://10.0.2.2:8000/panel/
SECURITY_CODE=use-the-same-value-as-the-root-env-file
CONTACT_EMAIL=contact@example.com
```

Start the PHP server, select an emulator, and run the `app` configuration. `10.0.2.2` connects the Android Emulator to the host computer.

The debug build permits local HTTP connections; release builds require HTTPS.

---

## ✅ Checks

GitHub Actions validates PHP syntax, applies the PostgreSQL schema, runs Android lint, and builds the debug APK.

Useful local commands:

```bash
find panel -type f -name '*.php' -print0 | xargs -0 -n1 php -l
cd Market && ./gradlew lintDebug assembleDebug
```

---

## 🎓 Project History

Honarnama was presented as a university project in **Dey 1399** (December 2020–January 2021). It was created collaboratively by [Ali Hashemi](https://github.com/hashemi1997ali) and his friend [Ali Ferasatpour](https://www.linkedin.com/in/ali-ferasatpour-a15b08108/).

### 🕊️ In Memory of Ali Ferasatpour

Ali Ferasatpour, a friend and collaborator on this project, has passed away. This repository is preserved in appreciation of his friendship, contribution, and the work created together.

> May his memory always be honored.

---

## 🔐 Security and Usage

- Do not commit `.env`, database URLs, uploads, customer data, or signing keys.
- Treat `SECURITY_CODE` as a compatibility value, not a real client secret.
- Use HTTPS and review authentication and upload handling before public deployment.
- See [SECURITY.md](SECURITY.md) for additional guidance.

No project-wide license has been granted. The repository is available for portfolio and educational review only.
