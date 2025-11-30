#!/bin/bash

set -e

echo "🚀 Setting up Android development environment..."

# Define Android SDK paths
export ANDROID_HOME="/usr/local/lib/android/sdk"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# Install Android SDK Command Line Tools if not present
if [ ! -d "${ANDROID_HOME}/cmdline-tools" ]; then
    echo "📥 Downloading Android SDK Command Line Tools..."
    # Command Line Tools version 11076708 (latest as of 2024)
    # Check https://developer.android.com/studio#command-line-tools-only for newer versions
    CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    sudo mkdir -p "${ANDROID_HOME}/cmdline-tools"
    cd /tmp
    wget -q "${CMDLINE_TOOLS_URL}" -O commandlinetools.zip
    unzip -q commandlinetools.zip
    sudo mv cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest"
    rm commandlinetools.zip
    cd -
    
    # Set proper ownership and permissions
    sudo chown -R vscode:vscode "${ANDROID_HOME}"
    sudo chmod -R 755 "${ANDROID_HOME}"
fi

# Accept Android SDK licenses
echo "📝 Accepting Android SDK licenses..."
yes | sdkmanager --licenses || true

# Install required Android SDK components
echo "📦 Installing Android SDK components..."
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" || true

# Update SDK components
echo "🔄 Updating SDK components..."
sdkmanager --update || true

# Set proper permissions for Gradle wrapper
echo "🔧 Setting Gradle wrapper permissions..."
chmod +x ./gradlew

# Install Gradle dependencies (helps with IDE indexing)
echo "📚 Downloading Gradle dependencies..."
./gradlew --version

echo "✅ Android development environment setup complete!"
echo "📱 You can now build the project with: ./gradlew assembleDebug"
echo "🧪 Run tests with: ./gradlew lintKotlin testDebugUnitTest"
echo "🎨 Format code with: ./gradlew formatKotlin"
