# Security

## Reporting a vulnerability

If you find a vulnerability, **do not open a public issue**. Message me on Telegram: @coondev.

Please include:

- what is wrong and where (screen, file, request);
- steps to reproduce;
- the app version;
- the impact as you see it.

I'll reply as soon as I can, fix it and publish an update. If you want, I'll credit you.

## Supported versions

Fixes ship only for the **latest version** on GitHub Releases. Older versions are not supported, please update.

## In scope

- The Android app and widget.
- Our Worker (sponsor status check).
- APK build and distribution (repository, releases, bot).
- Third-party integrations connected in the app (e.g. wallet activation).

## Out of scope

- Vulnerabilities in Telegram, TON, wallets and third-party APIs (tonapi, toncenter, etc.). Report those to their owners.
- Attacks requiring physical access to an unlocked phone.
- Social engineering and phishing not done in our name.

## How security works here

- The app stores no seed phrases, passwords or keys and never requests transaction signing.
- The address and settings are stored on the phone. Details: `PRIVACY.md`.
- Download the APK only from our repository or bot and verify the source.

## Rules for researchers

Don't touch other users' data, don't deliberately overload services, and don't disclose a vulnerability before it is fixed.
