# Dev Container Configuration

This directory contains the development container configuration for TRMNL Android.

## Features

- **Base Image**: Java 17 (Bookworm)
- **Android SDK**: Manually installed via post-create script
- **Android Command Line Tools**: Version 11076708
- **Platform Tools**: Latest version
- **VS Code Extensions**: Kotlin, Gradle, Java, IntelliJ keybindings, and GitHub Copilot support

## Environment Variables

- `ANDROID_HOME`: `/usr/local/lib/android/sdk`
- `ANDROID_SDK_ROOT`: `/usr/local/lib/android/sdk`

## Post-Create Setup

The `post-create.sh` script automatically:
1. Downloads and installs Android SDK Command Line Tools if not present
2. Accepts Android SDK licenses
3. Installs Android SDK Platform 36 and Build Tools 36.0.0
4. Updates SDK components
5. Sets Gradle wrapper permissions
6. Pre-downloads Gradle dependencies

## Usage

### First Time Setup

1. Open the project in VS Code
2. When prompted, click "Reopen in Container"
3. Wait for the container to build and post-create script to complete

Alternatively, when using GitHub Codespaces:
1. Click "Code" button on the repository
2. Select "Codespaces" tab
3. Click "Create codespace on main"

### Building the Project

```bash
./gradlew assembleDebug
```

### Running Tests

```bash
./gradlew lintKotlin testDebugUnitTest --parallel --daemon
```

### Formatting Code

```bash
./gradlew formatKotlin
```

### Linting Code

```bash
./gradlew lintKotlin
```

## Connecting Android Devices

The container is configured with `--privileged` mode to allow ADB access to connected USB devices for on-device debugging. Your local `.android` directory is mounted to preserve ADB keys and settings.

## Troubleshooting

### SDK License Issues

If you encounter SDK license errors, run:
```bash
yes | sdkmanager --licenses
```

### Gradle Issues

If Gradle fails to sync, try:
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### Android SDK Not Found

Verify the environment variables:
```bash
echo $ANDROID_HOME
echo $ANDROID_SDK_ROOT
```

Both should point to `/usr/local/lib/android/sdk`.
