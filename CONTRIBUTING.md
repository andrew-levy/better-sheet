# Contributing

Use Android Studio with JDK 17 and SDK 35. Preserve named-detent semantics and caller-owned lazy
scrolling. Add tests around user-observable behavior, particularly resizing and gesture handoff,
when changing those paths. Do not add a Material dependency to the core library.

Before a pull request:

```bash
./gradlew :better-sheet:testDebugUnitTest :better-sheet:lintRelease :sample:assembleDebug
./gradlew :sample:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.better.sheet.sample.SheetUiTest
```

Include the device/API/IME used and explain the reproduction scenario. When changing public API,
update README examples and architecture notes. Keep release artifacts and videos reproducible;
do not check build caches or credentials into the repository.

The README displays the recordings committed in `docs/videos/`. To recreate them, start
an emulator with `adb` and `ffmpeg` on `PATH`, then run:

```bash
python3 scripts/record-demos.py
python3 scripts/record-demos.py chat
```

The script builds and installs the sample and its test APK, runs one recording test per scenario,
and assembles per-frame screenshots into a 30 fps MP4 and a GIF. Every push to `main` records all scenarios. Once the build, lint, unit tests, API 24/35 device
tests, and recording job pass, CI commits the generated GIFs and MP4s to `docs/videos/`.
The README updates automatically. Video-only commits do not start another recording run.
Stale runs cannot overwrite newer source commits.

On feature branches, push a commit containing `[record-demos]` to generate a downloadable
`demo-videos` artifact. To publish manually, run the Android workflow on `main` with
`record_demos` enabled; validation still runs before publication.

## Releasing

Project convention: use plain semantic versions without a `v` prefix everywhere: Git tags,
GitHub release titles, `VERSION_NAME`, and dependency examples (for example, `0.2.1`).
Keep existing published tags intact; do not move or delete them to fix publication failures.

1. Update `VERSION_NAME` in `gradle.properties` and add a matching `## [x.y.z]` section to
   `CHANGELOG.md`.
2. Merge to `main`, then tag and push: `git tag X.Y.Z && git push origin X.Y.Z`.
   Alternatively, run the **Release** workflow from `main` with `version = X.Y.Z`.
   The manual workflow validates and packages the commit before creating the unprefixed tag.
   To refresh recordings before releasing, include `[release]` in a commit pushed to `main`.
   After Android validation and automatic video publication succeed, CI starts the Release
   workflow with the version from `gradle.properties`. No release starts if validation,
   recording, or video publication fails.
3. The Release workflow checks that the tag matches `VERSION_NAME`, runs unit tests and lint, and
   creates a GitHub release with the changelog section and artifacts. JitPack builds the tag on
   first request. The workflow requests and verifies the JitPack POM and AAR; a failed or
   unavailable publication fails the workflow instead of reporting a successful release.
4. Update the README dependency version to the new tag. Confirm the Release workflow succeeds
   before announcing that the dependency is available. If JitPack fails, inspect its build log;
   do not claim a GitHub release alone means JitPack published successfully.
5. When the Maven Central secrets are set, the workflow also uploads a signed deployment to the
   Central Portal. Open <https://central.sonatype.com/publishing/deployments>, review it, and
   click **Publish**. It usually appears on Maven Central within about 30 minutes.

One-time Maven Central setup:

- Sign in to <https://central.sonatype.com> with the `andrew-levy` GitHub account; the
  `io.github.andrew-levy` namespace is verified automatically. Generate a user token.
- Create a signing key (`gpg --quick-gen-key "Andrew Levy <email>" rsa4096 sign 0`), publish it
  (`gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`), and export it with
  `gpg --armor --export-secret-keys <KEY_ID>`.
- Add repository secrets: `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (the token),
  `SIGNING_KEY` (the exported key) and `SIGNING_KEY_PASSWORD`.

Maven Central releases are permanent; a published version cannot be replaced or deleted.
