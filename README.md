<div align="center">
  <img src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" width="128" height="128" alt="UmaPatcher Edge Logo">
  <h1>UmaPatcher Edge</h1>
  <p><b>Hachimi mod patching utility for Android</b></p>

  <p>
    <a href="https://github.com/Tenshou170/UmaPatcher-Edge/releases"><img src="https://img.shields.io/github/v/release/Tenshou170/UmaPatcher-Edge?color=brightgreen&logo=github&style=for-the-badge" alt="Latest Release"></a>
    <a href="https://developer.android.com/"><img src="https://img.shields.io/badge/Android-7.0+-3DDC84?logo=android&logoColor=white&style=for-the-badge" alt="Android Support"></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white&style=for-the-badge" alt="Kotlin Version"></a>
  </p>
  <p>
    <a href="https://discord.gg/YjBgmuqqYr"><img src="https://dcbadge.limes.pink/api/server/https://discord.gg/YjBgmuqqYr" alt="Discord Server"></a>
    <a href="https://github.com/Tenshou170/UmaPatcher-Edge/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="License"></a>
  </p>
</div>

---

**UmaPatcher Edge** is an advanced, high-performance asset-patching tool for Android designed to seamlessly inject translation packs and Hachimi-based mod libraries into the **UM:PD** client. It provides translators and players with a streamlined mobile patching experience, featuring rootless Shizuku installations and fully desugared Java 17 compilation target.

---

## ✨ Features

- ⚡ **Desugared Java 17 Engine**: Fully modernized codebase targeting Java 17 for robust, modern library desugaring and optimal performance.
- 📱 **Flexible Installation Modes**:
  - **Normal & Direct**: Easy patching setups.
  - **Shizuku Integration**: Run rootless elevated installations natively through Android's `DocumentsUI`.
- 📁 **Type-Safe File Selection**: Utilizes Jetpack Activity Contracts (`OpenDocument` / `OpenMultipleDocuments`) to easily navigate and select `.so` mod libraries and files without format restrictions.
- 🔄 **Automated Update Checker**: Built-in intelligence to fetch and apply releases automatically from the remote repositories.

---

## 📋 Requirements

| Component | Requirement |
| :--- | :--- |
| **Android OS** | Version 7.0 (API Level 24) or newer |
| **Client** | JP client / custom APK splits |
| **Optional tool** | **Shizuku** (highly recommended for seamless rootless installation) |

---

## 📦 Download

Grab the latest compiled `.apk` release directly from our **[Releases page](https://github.com/Tenshou170/UmaPatcher/releases)**.

---

## 📖 How to Use

For complete step-by-step instructions on setting up, picking directories, and using the application, see the **[Installation guide (Android) | Hachimi Edge](https://hachimi.noccu.art/docs/hachimi/installing-android)**.

---

## 🛠️ Building from Source

To compile and package the application on your local machine, please follow our detailed **[Building Guide](BUILDING.md)**.

---

## ⚖️ License

Distributed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.
