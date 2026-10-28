# Changelog

All notable changes to Better Sheet are documented here. The project follows
[Semantic Versioning](https://semver.org); while the version is 0.x, minor releases may change
public API.

## [Unreleased]

### Added

- `Dialog.configureForBetterSheet(hostWindow)` and `BetterSheetDialogTheme` for hosting
  `BetterSheet` in an existing DialogFragment with standard XML Fragment Navigation.
- A Fragment Navigation sample with Fragment-scoped ViewModels, saved-state results,
  stacked dialogs, and edge-to-edge system-bar configuration.

## [0.6.2] - 2026-10-20

### Fixed

- Keep Find My grab bars visible when their sheets are covered by another sheet.
  Callers continue to control visibility through the custom `dragHandle` composable.

### Changed

- Make entrance-rendering tests independent of screenshot latency while retaining
  checks for real rendered motion, first opening, and reopening.
- Compile test APKs before emulator startup, reduce test-driver memory use, and wait
  for Android services before testing on API 24 and 35.
- Preserve continuous Android logs and system diagnostics for device-test failures.

## [0.6.1] - 2026-10-19

### Fixed

- Keep tray shadow blur, opacity, and offset consistent while dragging and changing height,
  avoiding the offset gray layer beneath floating trays.

### Changed

- Use Compose 1.9 (BOM 2025.08.00) for fixed drop shadows; Android minimum remains API 24.
- Use Find My for README demos, with a persistent People/Chat bar, smooth progress-driven
  content transitions, standalone snap points, and progressively scaled stacked sheets.
- Keep Find My titles pinned and dismiss its keyboard before collapsing.
- Add independent JitPack publication verification and document progress-driven content effects.

## [0.6.0] - 2026-10-19

### Breaking API changes

- Rename animated `SheetState.animateTo` to `snapTo`, and the previous immediate `snapTo` to `jumpTo`.
- Move animation configuration from `rememberSheetState` to the sheet composable. Use
  `enterTransition`, `exitTransition`, and `snapAnimationSpec` on all three sheet composables.
  Rubber-band recovery and live content resizing retain internal defaults.

### Added

- Standard Compose enter/exit transitions, including fade, slide, scale, and combined transitions.
- Recorded animation gallery comparing spring, fade/tween, and slide + fade.

### Fixed

- Measure the destination height before entry so sheets slide fully into view from the bottom,
  including their first entrance and reopening.
- Preserve mounted content during transitions and invoke dismissal only after the exit completes.
- Release faded scrim touches during exit and remove the dismissal ripple.
- Remove the hard shadow cutoff beside rounded tray corners.

### Changed

- Simplify the README, lead with demos, and improve GIF preview quality.
- Group sample apps under `samples/` and record all demos on every source push, committing
  recordings only after Android validation succeeds.

## [0.5.0] - 2026-10-18

### Added

- Add the optional `SheetPresentation.Tray` presentation to `BetterSheet`,
  `BetterSheetHost`, and `BetterModalSheet`. Tray margins and corner radii morph
  smoothly as the sheet expands, with configurable insets, radii, and morph timing.
- Use Android 12+ window corner metadata for collapsed tray corners when available.

### Fixed

- Keep sheets visible with shared draw-pass clipping for sheet and stack surfaces.
- Bound sheet and footer shadows so they do not tint the navigation bar area.

## [0.4.0] - 2026-10-18

### Breaking API changes

- Rename the library to Better Sheet, with `BetterSheet`, `BetterModalSheet`, and
  `BetterSheetHost` as its rendering APIs.
- Move all library imports to `dev.better.sheet` and the Gradle module to `:better-sheet`.
- Use `com.github.andrew-levy:better-sheet:0.4.0` for JitPack installations.
- Keep `SheetState`, `rememberSheetState()`, snap points, behavior, and stack APIs unchanged.
  No compatibility aliases are provided; update imports and rendering function names together.

### Changed

- Update the sample apps, recording tools, documentation, and publication metadata for Better Sheet.
- Preserve existing tags and releases; new release tags continue to use unprefixed versions.
- Record and commit README videos automatically on main after Android validation passes.
  Successful `[release]` runs refresh videos before starting the Release workflow.

## [0.3.0] - 2026-10-17

### Breaking API changes

- Rename `SheetDetent` / `SheetDetentScope` to `SheetSnapPoint` / `SheetSnapPointScope`,
  `detents` to `snapPoints`, and `content()` to `fitContent()` (the default ID remains `"content"`).
- Rename `initialDetent`, `currentDetent`, and `targetDetent` to `initialSnapPoint`,
  `currentSnapPoint`, and `targetSnapPoint`; method argument `detent` becomes `snapPoint`.
- Rename `SheetImePolicy` / `imePolicy` to `SheetKeyboardBehavior` / `keyboardBehavior`.
- Rename `BetterModalSheet.onDismissRequest` to `onDismissed`; it runs after dismissal completes.
- Stack APIs use `SheetStackEntry`, `SheetStackEffectScope`, `LocalSheetStackEntry`,
  `Modifier.sheetStackEffect`, and the `stackState` parameter.
- Remove the obsolete `expandBeforeScroll` parameter. Content owns gestures that begin
  by scrolling; fresh edge gestures use `bubbleAtTop` and `bubbleAtBottom` independently.
- Scrim color controls drawing only. Use `SheetBehavior(scrimEnabled = false)` to remove
  both scrim drawing and touch handling. Enabled transparent scrims still intercept touches.
  Modal windows continue to block background interaction when their scrim is disabled.

### Changed

- Use a short, critically damped settle spring that preserves flick velocity. Open, close,
  snap-point changes, content-fit resize, and rubber-band return share the state animation spec.
- Dragging past the top, or past the lowest detent when drag dismissal is off, rubber-bands and springs back. A dismissible pull downward still tracks the finger off screen.

### Added

- Coordinated modal stacks through `rememberSheetStackState()`, with live cumulative depth
  and customizable surface effects. Each sheet retains its own visibility state.
- Independent content-edge handoff switches and nested release velocity tuning.
- `SheetBehavior.scrimEnabled` (default `true`) and explicit blocking of outside touches
  when scrim dismissal is disabled.
- Usage documentation and GIFs for modal and inline sheets, stacks, keyboard handling,
  and common parameters; Fragment integration examples.
- `SheetBehavior.scrimFollowsDrag` (default `true`). The scrim fades with the drag until the lowest detent, then stays fully opaque. Set it to `false` to hold the scrim at full strength whenever the sheet is visible.

### Fixed

- Reaching a list edge during a drag or fling no longer starts moving the sheet.
- Release background touch interception promptly after the sheet is visually dismissed.
- Upward overscroll stretches from the top while keeping the bottom edge and footer seated.

## [0.2.0] - 2026-10-13

### Changed

- The scrim covers the whole host, including the status bar, navigation bar, and display
  cutout, so the dimming has no seam at the system bars.
- Modal windows use transparent system bars and turn off contrast overlays. The sheet
  continues its container color behind the navigation bar.
- The sample scrolls body content before moving the sheet (`expandBeforeScroll = false`).

### Fixed

- Chat lists in the sample are top-aligned. Dragging the sheet carries the messages with
  it, and further dragging scrolls them until they run out.

## [0.1.0] - 2026-10-13

Initial public release.

### Added

- `BetterModalSheet`: a modal sheet in its own Dialog window with focus and input isolation, a
  Compose-drawn scrim, Back, scrim and drag dismissal policies, and accessibility actions.
- `BetterSheetHost`: an inline overlay host for persistent panels over interactive content.
- `BetterSheet`: an always-mounted, in-place root sheet for Activities and ComposeView Fragments,
  with `show()`/`hide()` and an `onDismissed` callback after the exit animation.
- Named detents: fixed, fraction, content (natural size), expanded and custom.
- `SheetState` with `animateTo`, `snapTo`, `show`, `hide`, `expand` and `collapse`, saveable
  across recreation, plus `currentDetent`, `targetDetent`, `isVisible` and continuous `progress`.
- Animated entry from hidden after the first chrome and body measurement, animated exit, and live
  animated content-fit resizing, all using the state's `animationSpec`.
- Sticky header and footer chrome that keeps its space in small windows, and `SheetBodyMode.Scroll`
  or `Fill` (for caller-owned lazy lists) with nested-scroll handoff between body and sheet.
- Keyboard policies: `SheetImePolicy.Resize`, `Expand` (with restoration) and `Ignore`.

### Known limitations

- Under heavy CPU load on low-core devices, the start of the opening animation can be delayed;
  see `docs/VALIDATION.md` for measurements.
- Accessibility, configuration changes, OEM keyboards and real-device performance need broader
  coverage before production rollout at scale.

[0.3.0]: https://github.com/andrew-levy/better-sheet/releases/tag/0.3.0
[0.2.0]: https://github.com/andrew-levy/better-sheet/releases/tag/v0.2.0
[0.1.0]: https://github.com/andrew-levy/better-sheet/releases/tag/v0.1.0


[0.5.0]: https://github.com/andrew-levy/better-sheet/releases/tag/0.5.0
[0.4.0]: https://github.com/andrew-levy/better-sheet/releases/tag/0.4.0


[0.6.0]: https://github.com/andrew-levy/better-sheet/releases/tag/0.6.0

[0.6.1]: https://github.com/andrew-levy/better-sheet/releases/tag/0.6.1

[0.6.2]: https://github.com/andrew-levy/better-sheet/releases/tag/0.6.2
