# TermNX

TermNX is an **unofficial, modified build of [Termux](https://github.com/termux/termux-app)**. It is not affiliated with or endorsed by the Termux project.

It keeps the upstream Termux engine (terminal, bootstrap installer, `apt`/`pkg`) untouched and only changes the visible branding. Because it deliberately keeps the `com.termux` application id and the `/data/data/com.termux/files/usr` prefix, the official Termux package repositories work as-is: you can `pkg install python`, run `pip install`, install `git`, `clang`, `nodejs`, and everything else from the Termux repos.

## Important consequences of keeping `com.termux`

- TermNX **cannot be installed alongside the official Termux app**. They share the same application id, so Android treats them as the same app. To install TermNX you must first uninstall Termux (and vice versa).
- TermNX is signed with its own key, so it cannot update over an existing Termux install and the system will treat it as a different signer.
- Distribution is via GitHub / sideload only. It is not suitable for Google Play (duplicate package id).

## What was changed from upstream

- App display name set to `TermNX` (`TERMUX_APP_NAME`).
- Launcher icon recolored to the TermNX green prompt.
- Replaced the CI with a single `build` workflow producing a universal debug APK.

Everything else, including the `com.termux` package id, prefix, bootstrap variant (`apt-android-7`) and the terminal engine, is unchanged from upstream Termux.

## Usage

```
pkg update
pkg upgrade
pkg install python
python --version
pip install requests
```

## Build

Builds run on GitHub Actions on every push to `main`. The workflow downloads the upstream Termux bootstrap at build time and bundles it into the APK, then uploads a universal debug APK artifact. On failure the build errors are surfaced as workflow annotations and an `error.txt` artifact.

## License

TermNX is released under **GPLv3 only**, the same license as upstream Termux. See [`LICENSE.md`](LICENSE.md). The `terminal-emulator` and `terminal-view` libraries are under Apache 2.0 as noted in the license file.

Upstream projects:
- Application: https://github.com/termux/termux-app
- Packages and build infrastructure: https://github.com/termux/termux-packages
