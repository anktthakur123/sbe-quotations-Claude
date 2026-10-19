# SBE Quotations — QuoteMaker Android application

**Android 11 (API 30) and newer** · `minSdk = 30` · `targetSdk = 34` · Kotlin · Android Studio

[![Android build](https://github.com/anktthakur123/sbe-quotations-Claude/actions/workflows/android-ci-release.yml/badge.svg?branch=main)](https://github.com/anktthakur123/sbe-quotations-Claude/actions/workflows/android-ci-release.yml) · **[Download latest installable APK](https://github.com/anktthakur123/sbe-quotations-Claude/releases/latest)**

QuoteMaker is an offline-first quotation app for **SIDH BABA ENTERPRISES**. Customer and quotation records stay on the device. GitHub is used only to publish the app's installation APK and check for updates.

## Features

- **Customer Master:** add, view, edit and delete customer records. Customer name is optional.
- **Paste multiple items:** accepts item code before or after the description, parses the pasted text into separate editable rows, and lets the operator correct each row.
- **Manual item entry:** use **Add** to add individual items. The quotation contains only the rows entered; no empty item rows are appended to the PDF.
- **Pricing:** quantity is fixed at `01nos`; the operator enters prices manually. PDF amounts use rupee formatting with two decimals, e.g. `₹ 2,600.00`.
- **Quotation numbering:** configure `Prefix` and `Next Serial Number`; the app advances numbering when a new quotation is created.
- **PDF export:** view, save and share a PDF. Files are named after the quotation number and saved in the phone's `Quotations Form` folder when storage access is granted.
- **Multi-page PDFs:** larger quotations flow onto additional pages. The quotation number is repeated on continuation pages, and the signatory/footer block is kept together at the end.
- **Editable History:** quotations are saved as compact local records, not PDF copies. Existing records can be edited and exported again with their quotation number retained.
- **JSON data backup:** export/import customer and quotation data so records can be restored on another device.
- **PDF design:** centered `QUOTATION` title, larger `CUSTOMER` heading, compact customer details, fixed owner details/terms/signatory and updated centered footer.
- **App icon:** red caliper/shield mark based on the supplied Sidh Baba Enterprises logo.
- **GitHub OTA updates:** checks the latest release when the app opens, and also provides a manual **Check for Update** action. Android still asks the user to approve installation; silent installation is not allowed.

## Latest APK and testing

1. Open the **[Releases page](https://github.com/anktthakur123/sbe-quotations-Claude/releases/latest)**.
2. Download **`QuoteMaker.apk`** from the latest release's **Assets** section.
3. Transfer/open it on an Android 11+ phone and approve the installation when prompted.
4. If Android requests it, enable **Allow from this source / Install unknown apps** for the browser or file manager that opens the APK.

A push to `main` builds a **debug APK artifact** under GitHub Actions (useful for build checks, not for OTA updates). Pushing a version tag such as `v1.0.2` builds a **signed release APK** and publishes it under GitHub Releases. For normal phone testing and future in-app OTA updates, install the signed APK from **Releases**.

> If you already installed a debug build from Android Studio, export a JSON backup first. Debug APKs have a different signing key from the release APK; uninstalling the debug build may be required before installing the first signed release and will erase that app installation's local data.

## Implemented app settings

- Quotation numbering: `Prefix` and `Next Serial Number`.
- Manual **Check for Update**, plus a quiet update check when the app opens.
- Export full JSON data backup and import a JSON backup.
- Owner details remain fixed in the app; the GitHub update repository is fixed in source code and is not editable from the UI.

The OTA updater is configured in `app/src/main/java/com/sidhbaba/quotemaker/util/Prefs.kt`:

- Owner: `anktthakur123`
- Repository: `sbe-quotations-Claude`

The updater calls the public GitHub Releases API and looks for a newer release containing an `.apk` asset. Keep this repository public for the unauthenticated update checker to work.

## Build locally in Android Studio

1. Install Android Studio and a Java 17 JDK (Android Studio's bundled JBR can be selected in **Settings → Build Tools → Gradle**).
2. Clone/download this repository and open its **repository root** in Android Studio (the folder containing `settings.gradle.kts`).
3. In **Tools → SDK Manager**, install **Android SDK Platform 34** and **Android SDK Build-Tools 34.0.0**.
4. Wait for Gradle Sync, select the `app` run configuration and a phone/emulator, then click **Run ▶**.
5. To build a local debug APK, use **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Expected path: `app/build/outputs/apk/debug/app-debug.apk`.

Build configuration: Gradle wrapper 8.7 · Android Gradle Plugin 8.4.2 · Kotlin 1.9.24 · Java target 17.

## Publish the first signed release (one-time setup)

The GitHub Actions workflow needs the signing key to produce release APKs that can be installed and updated consistently. Never upload the keystore or its passwords to the repository.

### 1. Add four repository secrets

Open [repository Actions secrets](https://github.com/anktthakur123/sbe-quotations-Claude/settings/secrets/actions) and add:

| Secret | Value |
|---|---|
| `SBE_KEYSTORE_BASE64` | Base64 of your existing `sbe-release.keystore` file |
| `SBE_KEYSTORE_PASSWORD` | The keystore password |
| `SBE_KEY_ALIAS` | `sbequotations` |
| `SBE_KEY_PASSWORD` | The key password (same as keystore password if you set them equal) |

On Windows PowerShell, copy the Base64 value from the keystore already saved in Documents:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Documents\sbe-release.keystore")) | Set-Clipboard
```

Paste the clipboard content into the `SBE_KEYSTORE_BASE64` secret. Add the other three values as separate secrets. **Do not paste secrets into chat, README, source code, or commits.** Keep multiple secure backups of the `.keystore` file and its passwords; later in-place updates require the same signing key.

### 2. Build and publish the first release

After the source has been pushed to `main` and the four secrets have been added, run these commands from the repository folder in Git Bash:

```bash
git tag v1.0.2
git push origin v1.0.2
```

Then open the **[Actions page](https://github.com/anktthakur123/sbe-quotations-Claude/actions)** and wait for the tagged build to turn green. The workflow publishes `QuoteMaker.apk` on the **[Releases page](https://github.com/anktthakur123/sbe-quotations-Claude/releases)**.

### 3. Publish future OTA updates

Commit and push code changes to `main`, then use a new version tag that has never been used before:

```bash
git add .
git commit -m "Describe the QuoteMaker change"
git push origin main
git tag v1.0.3
git push origin v1.0.3
```

Use the next version for each subsequent release (for example `v1.0.4`, `v1.0.5`). The workflow derives the version name/code from the tag, signs the APK with the same key, and uploads it to GitHub Releases. On the installed app, the updater can then detect it at launch or through **Check for Update**.

## Permissions and data

- To save directly to the phone's storage root in `Quotations Form`, grant **Enable Quotation Folder Access** in the app's Settings. Without it, saving falls back to the app-specific external folder.
- Customer data, history and prices remain in local app storage. Use **Export Data Backup (JSON)** before uninstalling or changing phones.
- The app's updater downloads a release APK from this public repository. Android displays its normal installation confirmation.

## Source layout

- `app/src/main/java/com/sidhbaba/quotemaker/pdf/PdfGenerator.kt` — PDF rendering, pages, table, terms, signatory and footer.
- `app/src/main/java/com/sidhbaba/quotemaker/util/ItemPasteParser.kt` — pasted item parsing.
- `app/src/main/java/com/sidhbaba/quotemaker/util/Currency.kt` — rupee formatting.
- `app/src/main/java/com/sidhbaba/quotemaker/util/GithubUpdateChecker.kt` — GitHub release update checks and APK download.
- `app/src/main/java/com/sidhbaba/quotemaker/data/` — Room database entities and DAOs.
- `app/src/main/assets/signature.png` — authorised signatory signature.
- `app/src/main/res/drawable-nodpi/ic_launcher_fg.png` — app launcher logo.
- `.github/workflows/android-ci-release.yml` — build-artifact and signed-release workflow.

---

**Important:** the repository contains source code and an automated APK build workflow, not a prebuilt APK by default. The first signed APK appears in Releases after the tag workflow succeeds. The workflow configuration and source should be treated as unverified until the first GitHub Actions run finishes successfully.
