# VidSave — TikTok Video Downloader with ChatGPT Interface

VidSave is a modern Android application built with Kotlin and Jetpack Compose for extracting and downloading TikTok videos via a conversational UI inspired by ChatGPT.

## 📱 Features

- **ChatGPT Conversational Interface:** Paste TikTok links directly in a chat interface with real-time extraction status and video preview cards.
- **Watermark-Free Rendition Support:** Downloads watermark-free renditions when verified by the extraction source.
- **Configurable Backend API:** Supports customizable extraction server endpoints via Settings screen.
- **Download Manager Integration:** Downloads videos directly to device media storage with progress indicators.
- **Download History:** Persistent local database using Room DB to view, share, and manage past downloads.
- **In-App Update System:** Automated background update checker retrieving release metadata from GitHub Releases with SHA-256 verification and FileProvider APK installer.
- **Arabic & English Localization:** RTL/LTR layout support for English and Arabic locales.
- **Dark & Light Themes:** Premium modern ChatGPT-inspired dark palette with light theme preference toggle.

---

## 🔐 Persistent APK Signing Configuration

To publish signed release APKs via GitHub Actions CI/CD, configure these **GitHub Repository Secrets**:

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` or `SIGNING_KEY` | Base64-encoded string of your persistent `.jks` or `.keystore` file (`base64 -w 0 upload-key.jks`) |
| `STORE_PASSWORD` | Password for your Java Keystore |
| `KEY_ALIAS` | Key alias name inside the keystore |
| `KEY_PASSWORD` | Password for the key alias |

---

## 🚀 Automatic Version Management Strategy

VidSave uses a patch versioning strategy (`1.0.0` -> `1.0.1` -> `1.0.2`):
- `versionName`: Calculated automatically during GitHub Actions CI/CD based on previous release tag or manually passed via `workflow_dispatch`.
- `versionCode`: Incrementing integer sequence corresponding to total release tags.

---

## 🛠 In-App Update Distribution Metadata

GitHub Releases publishes `update-metadata.json` alongside the signed release APK:

```json
{
  "version": "1.0.0",
  "versionCode": 1,
  "releaseName": "VidSave v1.0.0",
  "releaseNotes": "Initial release",
  "downloadUrl": "https://github.com/OWNER/VidSave/releases/download/v1.0.0/VidSave-v1.0.0-release.apk",
  "sha256": "APK_SHA256_HASH",
  "packageName": "com.vidsave.app",
  "mandatory": false
}
```

---

## 🏗 Architecture & Technologies

- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture Layering
- **UI:** Jetpack Compose + Material 3
- **Local Database:** Room Database with Kotlin Flow
- **Networking:** Retrofit2 + OkHttp3 + Moshi
- **Image Loading:** Coil Compose
- **Update System:** InAppUpdateManager + FileProvider (`content://com.vidsave.app.fileprovider`)
