# Contributing to Gram Wallet

Thanks for wanting to help. The rules are short.

## Stack

- Android, **Java** (not Kotlin). Code lives in `android/app/src/main/java/...`.
- Write new files and classes in Java, don't add Kotlin.

## How to submit changes

1. Fork the repo and create a separate branch for one task.
2. Keep changes **minimal**: no reformatting of existing code, no needless renames.
3. Open a Pull Request and briefly describe what changed and why.
4. I review the code and test how it works. If everything is fine, I approve and trigger the build. The APK is built automatically.

## Code style

- Write compactly: logic in fewer lines, no splitting into many short ones.
- Don't expand simple expressions across multiple lines for looks.
- Follow the style of the surrounding code, don't introduce your own.

## Security

- **Never commit** keys, tokens, seed phrases or private API keys. Use local settings or environment variables.
- The app must not request transaction signing and must not store wallet credentials.
- If your code sends data to an external server, state it in the PR and update `PRIVACY.md`: what is sent, where and why.

## Third-party integrations

Connecting external APIs (e.g. wallet activation) goes through a separate PR. It must include:

- what the service does and what data is sent to it;
- how the app behaves when the service is unavailable;
- credit to the service's author or partner in the app.

## Credit

Contributors are credited in the app and in the repository. The project carries the CoonLink brand (@coonlink).

## Questions

Message on Telegram: @coondev.
