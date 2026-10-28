# Fragment Navigation comparison

Run the sample gallery and select **Fragment navigation comparison**, then browse Customers →
View customer. Both buttons open the same VM-backed customer editor over the details screen.
Both use the same `NavController.navigateSafe(...)` helper and the same `Content()` override pattern.
The screen counter and returned note make preservation of the underlying screen easy to check.

| | Old: DialogFragment | New: modal sheet destination |
|---|---|---|
| Graph tag | `<dialog>` | `<sheet>` |
| Navigator | AndroidX `DialogFragmentNavigator` | Sample `SheetFragmentNavigator` |
| Fragment dialog | Full-screen transparent dialog | Disabled with `setShowsDialog(false)` |
| Sheet composable | `BetterSheet` | `BetterModalSheet` |
| Window owner | DialogFragment | BetterModalSheet's Compose Dialog |
| VM owner | Fragment | Fragment |
| Presentation call | `navigateSafe(actionId)` | `navigateSafe(actionId)` |

This is an application integration example, not a new public library API. The legacy version is a
valid hosting arrangement; the new version lets the library manage its own modal window and system
bar configuration. Neither version should have two dialogs for one presented sheet.

## Integration steps

1. Add `navigation-fragment-ktx` to the app. The example pins Navigation **2.8.9**, alongside the
   sample's Fragment **1.8.8**. Keep these versions aligned with your application's dependency policy.
2. Copy the sample's `SheetNavHostFragment` and `SheetFragmentNavigator` into your app's navigation
   layer. Use the custom NavHost in your layout. It preserves the standard FragmentNavigator container
   and adds a transparent sibling container with a stable resource ID for sheet Fragment views.
3. Register the navigator in `onCreateNavHostController`, before graph inflation and restoration.
   `app:navGraph` in `navigation_comparison.xml` also ensures the graph is assigned during recreation.
4. Create a host base class following `ComposeContentFragment` / `ModalSheetFragment` and subclass it as in
   `ModalCustomerSheetFragment`: retain your
   VM factory, `by viewModels()`, and `Content()` override. Disable the Fragment dialog in `onCreate`.
   Give the ComposeView a stable ID and use `DisposeOnViewTreeLifecycleDestroyed`.
5. Change participating graph destinations from `<dialog>` to `<sheet>`, retaining IDs, arguments,
   actions, and deep links. Existing regular Fragment and dialog destinations keep their navigators.
6. Keep your own `navigateSafe` helper. The sample's helper only demonstrates a RESUMED-entry guard;
   it deliberately does not catch and hide graph or argument errors. If your helper inspects dialog
   destination types, update that check to recognize the new `FloatingWindow` destination.
7. Render `BetterModalSheet` in `Content()`. Keep it mounted while hiding. Wire `onDismissed` to the
   navigator's entry-specific completion callback. A VM or button can request `SheetState.hide()`.
8. Keep logical stack mutations in Navigation. Never directly `dismiss()` a `<sheet>` Fragment or
   add a second Fragment back-stack entry for it. Use `popBackStack()` for navigation-driven closure.
9. Return results through `previousBackStackEntry.savedStateHandle` before closing, as in `saveResult()`.
   With stacked sheets, the previous entry is the previous sheet, not necessarily the underlying screen.

The new host remains a DialogFragment subclass to demonstrate migration of an existing base class.
It is added to an actual container as a regular Fragment, rather than presented with `.show()`.
Do not disable the dialog of a Fragment still managed by the stock `<dialog>` navigator: that
navigator observes `requireDialog()` and expects a real dialog.

## Stack and dismissal ownership

Each Fragment's tag is its **NavBackStackEntry ID**, not the destination ID. Two instances of the same
sheet destination therefore have independent arguments and VMs. Instantiation uses `FragmentFactory`.

For user dismissal, the modal animates out first. `onSheetHidden` then pops the matching navigator
entry (and any sheet entries above it, following the stock dialog navigator's policy).

For a programmatic single-entry pop, `popWithTransition` updates Navigation immediately while the
outgoing Fragment stays composed for the exit. Completion removes that Fragment. Fragment destruction
marks the outgoing entry's transition complete, allowing Navigation to release its state. A transaction
completion callback also handles optimized-away hosts that never reached a Fragment lifecycle. A repeated
`onDismissed` callback cannot pop the underlying screen because the outgoing entry is already absent.

Multi-entry pops remove their hosts immediately. A navigate-and-pop issued before a Fragment attaches
also removes the pending host without waiting for a composition or animation that never started.
Navigating to a regular full-screen destination uses Navigation's usual floating-window removal.

The navigator reconnects restored Fragments by their entry tags. The modal host saves its pending
close flag and reconciles a sheet state restored already hidden. Dismissal while FragmentManager has
saved its state is retried when the host resumes. Fragment view composition and VM ownership remain
with the Fragment; navigation-entry lifecycle changes alone do not pause every underlying Fragment.

## Scope and limitations

- This sample supports a normal Navigation 2 Fragment stack, arguments, repeated sheet destinations,
  user/VM dismissal, single-entry animated pops, multi-entry pops, configuration recreation, and
  SavedStateHandle results. It does not require migrating the app to Navigation Compose or Navigation 3.
- It explicitly rejects sheet `saveState` / `restoreState` options for multiple saved back stacks.
  Supporting those requires saving/restoring the associated Fragment state as well; do not remove the
  checks and claim support. Ordinary Activity/Fragment recreation uses a different restoration path.
- The sample uses Bundle arguments. Safe Args generation and typed-route integration are not exercised.
- `launchSingleTop` reuses the existing host and VM; editor inputs are initialized when the VM is created.
  Use a new entry when you need a new editor, rather than expecting singleTop to reset the VM.
- Multiple new sheets stack separate dialogs. Window ownership is one dialog **per presented sheet**.
  The sample does not coordinate cross-sheet background scaling via `SheetStackState`.
- Runtime behavior must be revalidated when adapting this navigator to another AndroidX version.
  Process-death restoration should be checked on the target app/device in addition to Activity recreation.

## Validation

`NavigationComparisonTest` covers both hosting paths, results, physical Back, scrim and drag dismissal,
programmatic pops, composition disposal and VM cleanup, stacked sheets, full-screen navigation,
Activity recreation with draft/VM preservation, immediate navigate-and-pop, and multi-entry popUpTo.
On API 29+, it also counts global window views to assert that opening either version adds one window.

```bash
./gradlew :sample:assembleDebug :sample:assembleDebugAndroidTest \
  :better-sheet:testDebugUnitTest :better-sheet:lintRelease
./gradlew :sample:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.better.sheet.sample.NavigationComparisonTest
```


## Focused option 3 prototype

Open **Navigation comparison** in the sample gallery, then Customers → account. The three
numbered buttons open the same customer editor at the same 80% snap point:

1. Existing `LegacyCustomerSheetFragment`: unchanged window setup.
2. Existing `ModalCustomerSheetFragment`: custom `<sheet>` navigator + `BetterModalSheet`.
3. `AdapterCustomerSheetFragment`: stock `<dialog>` navigator + `SheetDialogAdapter` + `BetterSheet`.

The adapter additionally clears system-bar colors and contrast enforcement, inherits the host's
system-bar icon appearance, configures cutout and IME policy, and disables outer window animations.
It keeps one DialogFragment window and the same Fragment-scoped ViewModel/result flow. No core API
changes are required. Copying the original four window flags alone was not the proposed fix.

Run `python3 scripts/record-navigation.py` with an API 35 emulator and ffmpeg available. The
prototype workflow runs NavigationComparisonTest (including adapter results, single-window
ownership, recreation, and full-screen navigation), SheetUiTest, and records all three scenarios.
Videos are artifacts of **Navigation prototype recordings**, under `docs/videos/navigation/`.
`comparison.mp4` labels synchronized opening/closing clips side by side; separate native-resolution
clips preserve all device pixels, including the system bars. These are real Android screenshots
at 32 ms steps of Compose animation time. Window-manager animation timing is not controlled by
that clock, so the legacy outer-window animation is illustrative rather than a timing benchmark.

This prototype tests presentation and basic stock-navigator integration. It does not add animated
interception of arbitrary `popBackStack()` calls to option 3; those remain stock dialog navigation.
Validate system-bar appearance, IME and dismissal on the production app's supported devices before
replacing the custom navigator. The clip alone does not establish API 24 or OEM compatibility.
