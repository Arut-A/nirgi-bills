# Security

## Does building this app from source give anyone access to someone's bills?

**No.** The app is only a client. All data lives behind a server you run, and the
server — not the app — enforces who can read it.

A stranger who clones this repo, builds the APK and installs it gets an empty shell:

- **It points nowhere by default.** The committed server URL is a placeholder
  (`https://your-nas.example.com`); a real address is never in this repo.
- **Even pointed at a real server, they're rejected.** Sign-in goes through Google;
  the server verifies the Google ID token **and** checks the email against a
  single-entry allow-list. Any other account gets `403`.
- **Sessions can't be forged.** The server signs session JWTs with a secret that
  lives only in the server's environment (`SESSION_SECRET`), never in this repo.
- **Every data endpoint is closed without a valid session** (`401` otherwise).

The app being open source does not weaken this: access is gated server-side, so the
source can be fully public while the data stays private.

## What is never in this repository

- No `.env`, secrets, API keys, session secret, or Google client ID
- No signing keystore or its password (git-ignored — the key is the app's identity)
- No real server address, account, bills, or PDFs

## Hardening in the app

- HTTPS only (cleartext disabled).
- Session token stored in `EncryptedSharedPreferences` (Android Keystore); excluded
  from Android auto-backup; never logged.
- Sign-in tied to the app's package + signing certificate via the Google OAuth
  Android client.

## Reporting

Found something? Open a private security advisory on this repository.
