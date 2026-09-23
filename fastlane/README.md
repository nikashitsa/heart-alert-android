fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android validate_metadata

```sh
[bundle exec] fastlane android validate_metadata
```

Validate Google Play metadata

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Capture store screenshots in every app language and frame them into metadata/android

Needs a running emulator or connected device, and Google Chrome for the captions

### android frame

```sh
[bundle exec] fastlane android frame
```

Frame the captures in fastlane/screenshots and add their captions, into metadata/android

frameit draws the Google Pixel 4 frame (ScreenshotTest captures at its resolution); the

captions and layout are screenshots/template.html, rendered by headless Chrome

### android feature_graphic

```sh
[bundle exec] fastlane android feature_graphic
```

Render the feature graphic of each locale into metadata/android, from its raw tracking capture

The layout is screenshots/feature_graphic.html, the subtitle <locale>/feature_graphic.strings

### android upload

```sh
[bundle exec] fastlane android upload
```

Upload to Google Play and submit changes

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
