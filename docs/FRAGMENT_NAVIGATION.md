# Fragment Navigation with BetterSheet

Use a standard `DialogFragment`, `ComposeView`, and XML `<dialog>` destination. The sample gallery's
**Fragment navigation** entry opens Customers → account → Add a note. The editor retains its
Fragment-scoped ViewModel, Bundle arguments, and SavedStateHandle results.

## Integration

Keep your existing Fragment base class and navigation helper. Apply the supplied theme before
window creation and call the public dialog helper after `super.onStart()`:

```kotlin
class CustomerSheetFragment : YourComposeDialogFragment() {
    private val viewModel: CustomerSheetViewModel by viewModels()

    override fun getTheme(): Int = dev.better.sheet.R.style.BetterSheetDialogTheme

    override fun onStart() {
        super.onStart()
        requireDialog().configureForBetterSheet(requireActivity().window)
    }

    @Composable
    override fun Content() {
        val state = rememberSheetState(initialSnapPoint = "medium")
        val close by viewModel.closeRequested.collectAsState()
        LaunchedEffect(close) { if (close) state.hide() }
        BetterSheet(
            state = state,
            snapPoints = listOf(
                SheetSnapPoint.fraction("medium", .8f),
                SheetSnapPoint.expanded(),
            ),
            onDismissed = { dismiss() },
        ) {
            CustomerEditor(viewModel)
        }
    }
}
```

Import `dev.better.sheet.configureForBetterSheet`. Render the sheet as the full-size ComposeView
root, with `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` and a stable view ID.
The sample's `ComposeContentFragment` illustrates that setup; the helper does not require using it.

```xml
<dialog
    android:id="@+id/customerSheet"
    android:name="your.package.CustomerSheetFragment">
    <argument android:name="customer" app:argType="string" />
</dialog>
```

Keep your normal AndroidX `NavHostFragment`, graph IDs, arguments, Safe Args setup, and
`navigateSafe(...)` calls. The sample pins Navigation 2.8.9 and Fragment 1.8.8, uses Bundle arguments,
and includes an illustrative RESUMED-entry guard. Safe Args and process-death restoration need
validation in the consuming app.

## Window policy

`BetterSheetDialogTheme` is a transparent, non-floating, no-title theme. An app may instead use its
own theme with equivalent attributes. **`android:windowIsFloating=false` must be set before the
Dialog window is constructed**; an `onStart()` helper cannot change this policy retroactively.
The helper rejects a floating window with an actionable message rather than silently leaving
system bars outside the sheet's drawable area.

`configureForBetterSheet(hostWindow)` makes the window fill its parent, disables the platform dim
and outer window animation, clears system-bar colors and contrast overlays, configures cutouts,
and copies the host's system-bar icon appearance. It matches Compose Dialog's IME policy: resize
on API 30 and earlier, adjust-nothing on API 31+. BetterSheet consumes the resulting insets.
The Compose sheet draws the scrim and sheet surface across the system-bar areas.

Use `BetterSheet` here: the DialogFragment already owns the modal window. `BetterModalSheet`
remains available for Compose callers that want the library to create their dialog.

## Dismissal and results

For animated user/VM closure, call `state.hide()` and let `onDismissed` call `dismiss()`. Save results
through `previousBackStackEntry.savedStateHandle` before requesting closure. For a stacked dialog,
the previous entry is the preceding dialog. AndroidX owns stack removal, VM cleanup, recreation,
and transitions to regular full-screen destinations.

A direct `popBackStack()` or `popUpTo` retains standard dialog dismissal; it does not wait for a
Compose exit animation. The sample demonstrates both kinds of closure. Use sheet-state closure
when the exit animation matters. This integration does not add predictive Back progress or
coordinate a shared scrim/background transform across stacked dialogs.

## Validation

`FragmentNavigationTest` covers results and screen preservation, one full-window dialog with
transparent system bars, theme misuse, physical Back, scrim/drag/VM closure, navigation pops,
composition and VM disposal, stacked dialogs, full-screen navigation, Activity recreation,
immediate navigate-and-pop, and multi-entry removal. It is included in the API 24/35 device runner.

```bash
./gradlew :sample:assembleDebug :sample:assembleDebugAndroidTest \
  :sample:lintDebug :better-sheet:testDebugUnitTest :better-sheet:lintRelease
./gradlew :sample:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.better.sheet.sample.FragmentNavigationTest
```
