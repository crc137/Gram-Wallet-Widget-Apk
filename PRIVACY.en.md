# Gram Wallet — money, data, and install (plain talk)

**Updated:** 8 November 2026  
**Contact:** [@coonlink](https://t.coonlink.com/coonlink)

Short version: **we do not steal money, we do not ask for seeds/keys, we do not ship malware.** This is a widget and client for viewing a public GRAM/TON balance.  
The project is **not affiliated** with Telegram or the TON Foundation — independent work.

## Money

- The app **does not request transaction signing** (via TON Connect or otherwise). Without your signature in another wallet, funds will not move.
- We **do not store** seed phrases, passwords, or keys, and we do not ask you to enter them.
- You enter the address yourself or connect via TON Connect **only to show the balance**. Funds stay in your wallet.
- This is not a custodial service or a bank: we **do not control** your balance.

## Data

- Address, balance cache, and notification settings are **stored on the phone**.
- We **do not collect** contacts, photos, SMS, call history, “ad” location, or build a hidden user profile.
- The app only talks to the network when the screen would be useless without it:
  - **public** balance and history for an address you already provided (open APIs such as tonapi / toncenter / gram.org);
  - APK update check (GitHub);
  - supporter badge check / unlock: a request to our **Cloudflare Worker**. The Worker sees the **wallet address** and **IP** (as any HTTPS request does). We **do not keep** advertising or analytics logs of these requests; for supporters, KV may store a “this address is a donor” flag (and a bot code), with no keys and no seed.
- TON addresses are public on-chain. Querying a balance **does not** grant access to your funds.

## Transaction notifications

- If enabled: **the phone itself** periodically polls a public history API for the address stored on the device and shows a local notification.
- That address is **not sent** to our server for pushes and is **not stored** by us for notifications.

## Install and “viruses”

- The APK comes from our repository / bot. We **do not embed** miners, trojans, or hidden password stealers.
- Installing outside Play Store needs “unknown sources” — normal for a sideloaded APK, not proof of malware.
- Source and releases are public — you can review them before installing.

## Donations (optional)

In the bot you can support the project (Stars or any tiny TON amount) — **voluntary**.

- Prefer not to — tap “Skip” and download the file. Paying is not required.
- The supporter badge is cosmetic “thanks”, not access to funds.

## What we do / do not promise

**We currently provide:** home-screen widget and balance screen, history, receive QR, optional local notifications, supporter badge.

**We do not do or promise:** key custody, price guarantees, fund insurance, official Telegram / TON Foundation status.

**About the future:** this version has **no** faucet / activation that tops up and returns GRAM. If that ships, the Money and Data sections will be rewritten (third party + fund movement). Until then, the statements above apply to the current app only.

If you are unsure — do not install. If you install — keep keys only on your side.
