# 🎨 Honarnama Market

A native Android marketplace for discovering, selling, ordering, and auctioning artwork, backed by a PHP REST API, a Neon PostgreSQL database, and an AngularJS administration panel.

Honarnama (هنرنما) brings the customer experience, catalogue management, artwork news, orders, auctions, and administration tools together in one university project that has been preserved and modernized for portfolio review.

---

## 📖 Project Overview

Honarnama lets customers browse artwork by category, search the catalogue, review product details, maintain a wishlist and shopping cart, submit orders, follow art news, and participate in auctions. Administrators can manage products, categories, orders, news, application settings, and optional email notifications from a browser-based dashboard.

The repository contains three connected parts:

```text
honarnama/
├── Market/                  # Native Android application
├── panel/                   # AngularJS admin panel and PHP REST API
│   ├── database/            # PostgreSQL schema and migration command
│   ├── services/            # API, database, email, and domain logic
│   └── uploads/             # Ignored runtime upload directories
├── .github/workflows/       # PHP/PostgreSQL and Android CI
├── .env.example             # Safe environment-variable template
├── dev-router.php           # Router for PHP's local development server
└── README.md
```

Runtime uploads, database credentials, local Android settings, build outputs, signing keys, and personal project material are intentionally excluded from Git.

---

## ✨ Features

### 🛍️ Android Marketplace

- Browse published artwork and product categories
- Search products and view detailed descriptions and image galleries
- Create a customer account and sign in
- Save products to a local wishlist
- Manage a local shopping cart and checkout form
- Submit orders to the PHP backend and review local order history
- Read normal and featured art news
- Browse artwork auctions and place bids
- Configure contact information from build settings

### 🧑‍💼 Administration Panel

- Sign in through the browser-based administration panel
- Review dashboard totals for products, orders, categories, and news
- Create, edit, filter, paginate, and remove products
- Manage product categories and product-image relationships
- Create and process customer orders
- Create and manage normal or featured news
- Configure currency, tax, shipping methods, and email preferences
- Manage the administrator profile and password

### 🗄️ Backend and Data

- PHP REST endpoints shared by the Android client and admin panel
- PDO-based PostgreSQL access with parameterized queries
- Neon pooled connections for application traffic
- Direct PostgreSQL connections for schema migration
- Idempotent schema creation and initial administrator setup
- PostgreSQL constraints, foreign keys, and useful indexes
- Local development routing without requiring Apache
- Optional SMTP order notifications

### 🔧 Modernization

- AndroidX and Material Components
- Java 17, Android SDK 37, and target SDK 36
- Gradle 9.4.1 and Android Gradle Plugin 9.2
- AngularJS 1.8.3 and AngularJS Material 1.2.5
- PostgreSQL/Neon migration from the original MySQL implementation
- Password hashing for new administrator and customer accounts
- Debug-only local HTTP support while release builds require HTTPS
- GitHub Actions checks for PHP, PostgreSQL migration, Android lint, and APK builds

---

## 🛠️ Technology Stack

### Android Client

- Java 17
- Android SDK 37 with target SDK 36
- AndroidX and Material Components
- Retrofit and OkHttp
- Gson
- Glide
- SQLite and SharedPreferences for local client data

### Admin Panel

- AngularJS 1.8.3
- AngularJS Material 1.2.5
- Angular Route, Cookies, Messages, and Sanitize
- HTML, CSS, and JavaScript

### Backend

- PHP 8.2 or later
- PDO PostgreSQL (`pdo_pgsql`)
- Neon/PostgreSQL
- PHPMailer-compatible SMTP integration
- Apache `mod_rewrite` in production or the included development router locally

---

## 🚀 Getting Started

### Prerequisites

- PHP `8.2` or later
- PHP extensions: `PDO` and `pdo_pgsql`
- A Neon/PostgreSQL database
- Android Studio with JDK 17 and Android SDK 37
- An Android Emulator or development device

### Installation

Clone the repository:

```bash
git clone https://github.com/hashemi1997ali/honarnama.git
cd honarnama
```

Create the local backend environment file:

```bash
cp .env.example .env
```

Configure at least the database, Android compatibility value, and initial administrator in `.env`:

```env
# Pooled Neon URL for normal API requests
DATABASE_URL=postgresql://USER:PASSWORD@ENDPOINT-pooler.REGION.aws.neon.tech/DATABASE?sslmode=require&channel_binding=require

# Direct Neon URL for schema changes and migrations
DATABASE_URL_UNPOOLED=postgresql://USER:PASSWORD@ENDPOINT.REGION.aws.neon.tech/DATABASE?sslmode=require&channel_binding=require

SECURITY_CODE=replace-with-a-random-local-value

ADMIN_NAME=Administrator
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=replace-with-at-least-12-characters
```

`DATABASE_URL` falls back to `DATABASE_URL_UNPOOLED` when omitted, which is convenient during local development. A pooled URL is recommended for deployed API traffic. Never put either database URL inside the Android application.

### Prepare the Database

Create or update the PostgreSQL schema and insert the initial administrator when it does not already exist:

```bash
php panel/database/migrate.php
```

The migration is safe to run again. It preserves an existing administrator and does not overwrite that account's password. It creates essential settings and currencies but intentionally adds no sample products, news, auctions, customers, orders, or images.

### Run the Backend and Admin Panel

From the repository root, start PHP's local development server:

```bash
php -S 127.0.0.1:8000 dev-router.php
```

- Admin panel: `http://127.0.0.1:8000/panel/`
- REST API base: `http://127.0.0.1:8000/panel/services/`

Verify the database connection:

```bash
curl http://127.0.0.1:8000/panel/services/checkResponse
```

Expected response:

```text
Database Connection : Success
```

### Run the Android Application

Open the `Market/` directory in Android Studio and wait for Gradle synchronization. Android Studio normally creates `Market/local.properties`; add these project values to it:

```properties
BACKEND_URL=http://10.0.2.2:8000/panel/
SECURITY_CODE=use-the-same-value-as-the-root-env-file
CONTACT_EMAIL=contact@example.com
```

Start the PHP server, select an Android Emulator, and run the `app` configuration. `10.0.2.2` is Android Emulator's alias for the host computer.

The Debug build permits the local HTTP address above. Release builds block cleartext traffic and require an HTTPS backend URL ending with `/`.

---

## 📜 Available Commands

| Command | Description |
| --- | --- |
| `php panel/database/migrate.php` | Apply the PostgreSQL schema and create the initial administrator |
| `php -S 127.0.0.1:8000 dev-router.php` | Start the local backend and admin panel |
| `curl http://127.0.0.1:8000/panel/services/checkResponse` | Verify the active database connection |
| `find panel -type f -name '*.php' -print0 \| xargs -0 -n1 php -l` | Check PHP syntax |
| `cd Market && ./gradlew assembleDebug` | Build the debug APK |
| `cd Market && ./gradlew lintDebug` | Run Android lint |
| `cd Market && ./gradlew lintDebug assembleDebug` | Run Android lint and build together |

The generated debug APK is written to `Market/app/build/outputs/apk/debug/` and remains ignored by Git.

---

## 🛣️ Main Routes

### Administration Panel

AngularJS uses hash-based routes below `http://127.0.0.1:8000/panel/`:

| Route | Access | Description |
| --- | --- | --- |
| `#/login` | Public | Administrator sign-in |
| `#/dashboard` | Administrator | Store, order, category, and news overview |
| `#/product` | Administrator | Product catalogue management |
| `#/create_product` | Administrator | Create or edit a product |
| `#/category` | Administrator | Category management |
| `#/create_category` | Administrator | Create or edit a category |
| `#/order` | Administrator | Customer order management |
| `#/create_order` | Administrator | Create or edit an order |
| `#/news` | Administrator | News management |
| `#/create_news` | Administrator | Create or edit news |
| `#/setting` | Administrator | Application, email, and account settings |

### Android REST API

| Endpoint | Method | Purpose |
| --- | --- | --- |
| `/info` | `GET` | Load currency, tax, and shipping configuration |
| `/listCategory` | `GET` | List published categories |
| `/listProduct` | `GET` | Search and paginate published products |
| `/getProductDetails` | `GET` | Load a product with categories and images |
| `/listFeaturedNews` | `GET` | List featured news |
| `/listNews` | `GET` | Search and paginate published news |
| `/getNewsDetails` | `GET` | Load a news article |
| `/registerUser` | `POST` | Register an Android customer account |
| `/loginUser` | `POST` | Sign in an Android customer |
| `/listProductAuction` | `GET` | Search and paginate auctions |
| `/getProductAuctionDetails` | `GET` | Load auction details |
| `/addBid` | `POST` | Submit an auction bid |
| `/submitProductOrder` | `POST` | Submit an order and its items |

All paths above are relative to `/panel/services/`.

---

## 👤 Initial Administrator

The first migration reads these variables from `.env`:

```env
ADMIN_NAME=Administrator
ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=replace-with-at-least-12-characters
```

If `ADMIN_USERNAME` already exists, migration leaves the existing record and password unchanged. Do not expect changing `ADMIN_PASSWORD` followed by another migration to reset an established account.

---

## 🎓 Project History

Honarnama began as a university project and was presented in **Dey 1399** (December 2020–January 2021). The project was created collaboratively by [Ali Hashemi](https://github.com/hashemi1997ali) and his friend [Ali Ferasatpour](https://www.linkedin.com/in/ali-ferasatpour-a15b08108/).

The original work has now been cleaned, updated, migrated to PostgreSQL, and prepared for preservation as a portfolio project while retaining its academic history.

### 🕊️ In Memory of Ali Ferasatpour

Ali Ferasatpour, a friend and collaborator on this project, has passed away. This repository is preserved in appreciation of his contribution, friendship, and the work created together.

> His memory is honored. یادش گرامی باد.

---

## 📌 Production Notes

- This is a modernized university and portfolio project, not a production-ready commerce platform.
- Review authentication, authorization, upload handling, and the legacy `SECURITY_CODE` mechanism before public deployment.
- Any value embedded in an APK can be extracted; `SECURITY_CODE` is not a real client secret.
- Configure an HTTPS PHP deployment before distributing a release APK.
- Use the pooled Neon URL for ordinary API traffic and the direct URL for migration work.
- Rotate credentials exposed through old source files, chats, logs, screenshots, or database exports.
- Never commit `.env`, database URLs, customer data, runtime uploads, signing keys, or SMTP credentials.
- Checkout submits an order to the backend; no online payment SDK is currently bundled.
- GitHub Pages cannot run the PHP API or PostgreSQL database.
- Review [SECURITY.md](SECURITY.md) before exposing the project publicly.

---

## ✅ Continuous Integration

GitHub Actions runs the following checks for pushes and pull requests:

- PHP syntax validation
- PostgreSQL schema migration against an isolated PostgreSQL service
- Required-table verification
- Android lint
- Android debug APK build

---

## 📄 License and Usage

No project-wide license has been granted yet. The repository is currently available for portfolio and educational review only. Bundled third-party libraries, fonts, icons, and artwork retain their respective rights.

Choose and add an explicit license before accepting external contributions or redistributing the project.
