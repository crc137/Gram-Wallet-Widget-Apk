# Accessibility

Gram Wallet should be usable by everyone, including people with low vision, limited motor control or hearing.

## What we aim for

- Readable text and sufficient contrast in the app screens.
- Large, easy-to-hit buttons.
- Support for the system font size and Android dark theme.
- Labels (contentDescription) for buttons and icons so TalkBack can announce them.
- Information not conveyed by color alone: statuses are also given as text.

## Known limitations

- **The widget draws the wallet card as an image** with the gift's backdrop, pattern and stickers. Screen readers may not read everything drawn on the card. We try to duplicate the balance and address as a text description.
- The backdrop and stickers come from the gift itself, so text contrast on the card depends on the chosen gift.
- The widget refreshes every 15 minutes and does not announce changes on its own.

## Reporting a problem

If something is hard to use or read, message me on Telegram: @coondev. Please include:

- phone model and Android version;
- which screen or widget;
- which assistive feature you use (TalkBack, large font, etc.);
- what exactly fails.

Accessibility fixes ship in regular updates on GitHub Releases.
