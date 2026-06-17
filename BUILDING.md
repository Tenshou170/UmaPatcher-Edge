# Building UmaPatcher-Edge from Source

A complete guide to building **UmaPatcher-Edge** on your local machine. This document covers environment setup, dependency configuration, and compilation instructions.

---

## 📋 Prerequisites

Before building, ensure your system meets these requirements:

### 1. Java Development Kit (JDK)
- **JDK 17** (required for both Gradle daemon and compilation)
- Download from: [Eclipse Temurin JDK 17](https://adoptium.net/temurin/releases/?version=17) or [Amazon Corretto 17](https://aws.amazon.com/corretto/)

### 2. Android SDK
The following Android SDK components are required:
- **Android API 35** (`platforms;android-35`)
- **Build-Tools 35.0.0** or newer
- **Platform-Tools**
- **CMake** (optional, for native compilation)

### 3. Git
- Required to clone the repository and manage source control

---

## 🛠️ Environment Setup

### Step 1: Clone the Repository

```bash
git clone https://github.com/Tenshou170/UmaPatcher-Edge.git
cd UmaPatcher-Edge
```

### Step 2: Install JDK 17

Choose your platform:

**Linux/macOS:**
```bash
# Download using curl
mkdir -p ~/jvm
curl -L -o ~/jvm/jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
tar -xzf ~/jvm/jdk17.tar.gz -C ~/jvm && rm ~/jvm/jdk17.tar.gz
```

**Windows:**
1. Download the JDK 17 installer from [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17)
2. Run the installer (default location: `C:\Program Files\Eclipse Adoptium\`)

### Step 3: Configure JDK Path (Optional but Recommended)

Create or edit your global Gradle configuration to automatically detect JDK 17:

**Linux/macOS** (`~/.gradle/gradle.properties`):
```properties
org.gradle.java.installations.paths=/path/to/jdk-17
```

**Windows** (`%USERPROFILE%\.gradle\gradle.properties`):
```properties
org.gradle.java.installations.paths=C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.x.x-hotspot
```

### Step 4: Configure Android SDK

Create `local.properties` in the project root directory:

**Linux/macOS:**
```properties
sdk.dir=/home/username/Android/Sdk
```

**Windows:**
```properties
sdk.dir=C:\\Users\\username\\AppData\\Local\\Android\\Sdk
```

If you don't have the Android SDK installed, download the [Android Command Line Tools](https://developer.android.com/studio#command-line-tools-only) and run:

**Linux/macOS:**
```bash
sdkmanager --sdk_root ~/Android/Sdk "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

**Windows (PowerShell):**
```powershell
$env:ANDROID_HOME = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
.\cmdline-tools\bin\sdkmanager.bat "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

---

## 🔑 Release Signing Configuration

By default, debug builds use the included `debug.keystore` file. For release builds with custom signing:

Set these environment variables before building:
```bash
export SIGNING_KEY_FILE="/path/to/keystore.jks"
export SIGNING_STORE_PASSWORD="your_keystore_password"
export SIGNING_KEY_ALIAS="your_key_alias"
export SIGNING_KEY_PASSWORD="your_key_password"
```

**Windows (PowerShell):**
```powershell
$env:SIGNING_KEY_FILE = "C:\path\to\keystore.jks"
$env:SIGNING_STORE_PASSWORD = "password"
$env:SIGNING_KEY_ALIAS = "alias"
$env:SIGNING_KEY_PASSWORD = "key_password"
```

---

## 🚀 Building the Application

### Debug Build

**Linux/macOS:**
```bash
./gradlew assembleDebug
```

**Windows (Command Prompt):**
```cmd
gradlew.bat assembleDebug
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat assembleDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

**Linux/macOS:**
```bash
./gradlew assembleRelease
```

**Windows (Command Prompt):**
```cmd
gradlew.bat assembleRelease
```

**Windows (PowerShell):**
```powershell
.\gradlew.bat assembleRelease
```

Output APK: `app/build/outputs/apk/release/app-release.apk`

### Build with Android Studio

1. Open Android Studio
2. Select **Open an Existing Project** and navigate to the cloned directory
3. Android Studio will automatically detect `local.properties`
4. Configure Gradle JDK:
   - Go to `Settings` → `Build, Execution, Deployment` → `Build Tools` → `Gradle`
   - Set **Gradle JDK** to JDK 17
5. Build via **Build** → **Make Project** or **Build** → **Build Bundle(s) / APK(s)**

---

## 📊 Build Configuration

Current build settings (from `app/build.gradle`):

| Setting | Value |
|---------|-------|
| **Compile SDK** | 35 |
| **Min SDK** | 24 (Android 7.0) |
| **Target SDK** | 35 |
| **Kotlin** | 2.0.21 |
| **Compose** | 1.5.4 |
| **JDK Target** | 17 |
| **Gradle** | 8.x (managed by wrapper) |

---

## ❓ Troubleshooting

### Build Fails with "Unsupported class file major version"
**Cause:** Gradle is running on Java 21+ instead of JDK 17.

**Solution:**
```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug

# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot"
.\gradlew.bat assembleDebug
```

### Android SDK not found
**Cause:** `local.properties` is not configured correctly or SDK is not installed.

**Solution:**
1. Verify `local.properties` exists and has correct path
2. Run `sdkmanager` to install Android 35 platform and build-tools
3. Restart Android Studio or IDE

### Gradle Wrapper Permission Denied (Linux/macOS)
**Solution:**
```bash
chmod +x gradlew
./gradlew assembleDebug
```

### Out of Memory (OOM) during build
**Solution:** Increase Gradle heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

---

## 📝 Notes

- The Gradle wrapper (`gradlew`/`gradlew.bat`) automatically downloads the correct Gradle version
- The first build will take longer as dependencies are downloaded
- Builds are cached; subsequent builds are much faster
- For CI/CD pipelines, set environment variables for signing keys securely

