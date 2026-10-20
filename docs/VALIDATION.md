# Validation

## Device suite — October 13, 2026

`SheetUiTest`, `SheetMotionTest`, `FragmentSheetTest` and `HeavyContentMotionTest` (11 tests)
passed on both API 24 and API 35 x86_64 Pixel 4 emulators with system animations enabled:
[run 38399272177](https://github.com/andrew-levy/better-sheet/actions/runs/38399272177).
They cover intermediate entry/exit/resize frames, interrupted entry, mounted-body retention across
hide/show, keyboard expansion and restoration, a real Fragment ComposeView being disposed after an
animated exit, and opening timing under heavy background load.

Earlier API 24 jobs never booted: the emulator aborted with "Not enough space to create userdata
partition", so the workflow now frees hosted-runner disk first. Later API 24 runs sometimes lost the
system server during the first test (`DeadSystemException`, "System has crashed"); the emulators now
get 3 GB of RAM and a settle period after boot. The chat keyboard test was flaky on API 35 because
the IME ignores show requests while the sheet's dialog window lacks focus; the test now waits for
that window to be focused, CI hides system error dialogs that can steal focus, and failures report
the focused window.

## Find My recordings

The recording pipeline now targets the real Find My app in light mode. Each scenario
waits for MapLibre to render its tiles before capturing:

- `snapPoints`: people list at peek, expanded, medium, and the permanent collapsed tab bar
- `tabs`: animated People/Chat changes, including reopening from the tab bar
- `tray`: frame-by-frame drag reversal, tray morphing, and progress-driven fade/blur
- `chat`: keyboard expansion, composing, sending, keyboard dismissal, and collapse
- `contentFit`: person details resizing as nearby places are added and removed
- `stacked`: person details, contact information, and notification sheets
- `scrollHandoff`: list scrolling followed by a captured handle drag and collapse

`FindMyNavigationTest` checks the persistent minimum height, tab selection, retained drafts,
unchanged footer position, and Back collapsing rather than hiding on API 24 and 35.
The sample gallery remains the focused library regression suite.

The software-rendered CI emulator misses motion with `screenrecord`, so clips capture
one screenshot per 32 ms of paused Compose time and encode native-resolution 30 fps
H.264 MP4s plus 540 px GIF previews directly from the captured frames. The map and IME
use the device clock; these videos demonstrate behavior, not frame-time performance.
Recordings publish only after build, device tests, and every recording pass.

## Heavy content while opening — October 13, 2026

The Heavy demo decodes 3,000 messages with SHA-256 rounds on every core while a simulated
900 ms database read is pending. `HeavyContentMotionTest` opens it on the real device frame
clock (no Compose test clock), five times each, and compares it with the plain chat. Medians from
[run 38394688976](https://github.com/andrew-levy/better-sheet/actions/runs/38394688976) and
[run 38394694275](https://github.com/andrew-levy/better-sheet/actions/runs/38394694275):

| Emulator | Scenario | Time until the sheet starts moving | Time spent moving | Longest frame |
| --- | --- | --- | --- | --- |
| API 24 | Chat | 243 ms | 759 ms | 85 ms |
| API 24 | Heavy, normal priority | 418 ms | 837 ms | 143 ms |
| API 24 | Heavy, background priority | 426 ms | 874 ms | 148 ms |
| API 35 | Chat | 669 ms | 745 ms | 208 ms |
| API 35 | Heavy, normal priority | 1241 ms | 710 ms | 200 ms |
| API 35 | Heavy, background priority | 999 ms | 705 ms | 206 ms |

On these 2-core software-rendered emulators, saturating every core delays the start of the
opening by roughly 0.2–0.6 s. Once moving, the sheet's motion time is about the same; API 24's
longest frame grows from about 85 ms to about 145 ms. Background thread priority helps on API 35
but does not remove the delay. The cause of the remaining delay (for example garbage collection
from the decoding allocations) has not been isolated. The test's 150 ms start-time tolerance
fails on some runs. These emulators are much slower than real devices, so the absolute numbers
are not device performance figures.

The heavy clip is frame-captured like the others, so it shows the loading sequence (skeleton
rows during the opening, then the thread) but cannot show timing; use the table above for that.

## Local checks

The library, sample APK and instrumentation APK build. All 27 library unit tests pass.
Release lint passes (dependency-version notices only).

## Remaining verification

Accessibility, configuration changes, performance, OEM keyboards and production rollout
still need broader device coverage. No production traffic evidence or representative
frame-time benchmarks are provided by these demonstration videos.
