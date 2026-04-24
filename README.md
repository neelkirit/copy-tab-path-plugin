# Copy Tab Path Plugindd

Option+Click (Mac) or Alt+Click (Windows/Linux) on an editor tab to copy the file's project-relative path to the clipboard.

## Requirements

- JDK 17+
- IntelliJ IDEA 2023.1+ (Community or Ultimate)

## Build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 19)   # if not already set
./gradlew buildPlugin
```

The plugin zip will be at `build/distributions/copy-tab-path-plugin-1.0.0.zip`.

## Install

1. Open IntelliJ IDEA
2. Go to **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
3. Select the `.zip` file from `build/distributions/`
4. Restart the IDE

## Usage

Hold **Option** (Mac) or **Alt** (Windows/Linux) and click any open editor tab. The file's path relative to the project root is copied to your clipboard, and a toast confirms what was copied.
