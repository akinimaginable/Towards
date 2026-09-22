# Build Towards on iOS

This builds a Release archive and installs it on a paired iPhone with the free Apple ID already configured for team `T9MBAJS7Z9`. Signing is automatic and uses [app/iosApp/ExportOptions.plist](../app/iosApp/ExportOptions.plist) (`method` = `debugging`).

## Version

The version users see and the build number are in [app/iosApp/Configuration/Config.xcconfig](../app/iosApp/Configuration/Config.xcconfig):

- `MARKETING_VERSION` is the release version (`2026.9.0`). Keep it aligned with `towards.version` in `gradle.properties`.
- `CURRENT_PROJECT_VERSION` is the build number. Raise it for every new install of the same marketing version.

The bundle id signed by the Xcode target is `org.etrange.Towards`.

## Archive

From `app/iosApp`:

```sh
xcodebuild \
  -project iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -archivePath "$PWD/build/Towards.xcarchive" \
  archive
```

The **Compile Kotlin Framework** build phase runs `./gradlew :app:shared:embedAndSignAppleFrameworkForXcode` with `CONFIGURATION=Release`, so there is no separate Gradle step. The first Release archive compiles the Kotlin/Native binary and takes a long time.

## Export the .ipa

```sh
xcodebuild -exportArchive \
  -archivePath "$PWD/build/Towards.xcarchive" \
  -exportPath "$PWD/build/export" \
  -exportOptionsPlist ExportOptions.plist
```

`ExportOptions.plist` must sit next to the project. Its `teamID` is `T9MBAJS7Z9`. Change that value, and **Signing & Capabilities → Team** in Xcode, when signing with a different Apple ID.

## Install on a device

```sh
xcrun devicectl list devices
xcrun devicectl device install app --device <device-uuid> "$PWD/build/export/Towards.ipa"
```

Use the identifier printed by `list devices`.

On first launch, iOS blocks the app until you open **Settings → General → VPN & Device Management**, select the Apple ID that signed the build, and tap Trust.
