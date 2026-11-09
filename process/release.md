# Release parameters (release-seal)

The generic steps live in the global `release-seal` skill. This file only records the parameters and exceptions for this project.

## Version

- Semantic versioning `X.Y.Z`: bump Z for bug fixes only, bump Y for new APIs.
- **The version is bumped at release time.** Do not bump to the next version after sealing (skip the skill's "bump version" step).
- Update all of these in the release commit (`grep -rn "<old version>" bezier/build.gradle sample/build.gradle README.md README-CN.md` should hit every one):
  - `version` under `publishing` in `bezier/build.gradle`
  - `versionName` in `sample/build.gradle` (leave `versionCode` unchanged). The sample's version badge and dependency coordinate read from it, so nothing else in the sample needs editing.
  - The dependency coordinates (Groovy and Kotlin) and the "download sample APK" link in `README.md` and `README-CN.md`

## Pre-release checks

```bash
./gradlew :bezier:testDebugUnitTest :bezier:assembleRelease :sample:assembleDebug :sample:testDebugUnitTest
```

- CI (`.github/workflows/ci.yml`) does not run the sample tests, so run them locally.
- Library changes must also be re-tested on a real device for the affected scenarios.

## Tag

- The tag name is the bare version, without a `v` prefix: `2.0.2`
- Annotated tag with the message `BezierMaker X.Y.Z`, created on the release commit and pushed to `origin`
- JitPack builds from the tag; the coordinate is `com.github.venshine:BezierMaker:X.Y.Z`. A new tag is not built until something requests it, so request `https://jitpack.io/com/github/venshine/BezierMaker/X.Y.Z/BezierMaker-X.Y.Z.pom` once, then check that `https://jitpack.io/api/builds/com.github.venshine/BezierMaker/X.Y.Z` reports `"status" : "ok"`
- A pushed tag is pulled by users right away: never delete or move it. Fix problems in a new patch version.

## GitHub Release

- Title is the version number. Notes are written in English, with sections such as "Fixed / Sample app / Tests / Installation", kept or dropped as the release requires
- Attachment: the sample APK, named `BezierMaker-sample-X.Y.Z.apk`
  - Build: `./gradlew :sample:assembleDebug`; output at `sample/build/outputs/apk/debug/sample-debug.apk`
  - Signed with the local debug certificate (SHA-256 `bc4b28be…1433982`), the same as previous releases, so users can install it over an older version. Check with `apksigner verify --print-certs`; a different certificate prevents in-place upgrades

## Afterwards

- There is no product doc or ROADMAP; the GitHub Release is the source of truth for release notes, so no other docs need syncing
