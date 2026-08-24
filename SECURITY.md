# Security policy

## Project status

Honarnama is an archived university project and has no supported production release. Please do not deploy it on a public server without a full security review.

Known areas that require modernization include:

- replacing legacy MD5/plain-text password handling with `password_hash`/`password_verify` and server-side sessions or a reviewed token design;
- converting dynamic SQL to prepared statements and validating every API input;
- adding authorization, CSRF protection, rate limiting, and hardened cookie settings to the admin panel;
- restricting uploads by authenticated user, destination, MIME type, extension, size, and server execution policy;
- replacing the end-of-life AngularJS panel and upgrading the legacy PHP/PHPMailer code;
- keeping all database, mail, signing, and hosting credentials outside the repository;
- using HTTPS only and configuring production security headers.

The Android `SECURITY_CODE` option exists for compatibility only. A value embedded in a client application is recoverable and cannot serve as a trusted secret.

## Reporting a vulnerability

If this repository is published on GitHub, report vulnerabilities privately through GitHub Security Advisories. Do not include credentials, personal data, or exploitable details in a public issue.
