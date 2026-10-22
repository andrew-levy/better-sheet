package dev.better.sheet.sample.navigation

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState

/** Sample adapter for Navigation 2.8.9. Navigation owns the stack; FragmentManager owns the views. */
@Navigator.Name("sheet")
class SheetFragmentNavigator(
    private val context: Context,
    private val fragments: FragmentManager,
    private val containerId: Int,
) : Navigator<SheetFragmentNavigator.Destination>() {
    // Includes transactions committed but not attached yet (navigate + pop in the same frame).
    private val pending = mutableMapOf<String, ModalSheetFragment>()
    private val removing = mutableSetOf<String>()

    private val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            if (f is ModalSheetFragment) {
                if (state.backStack.value.none { it.id == f.tag }) remove(f)
                else if (state.backStack.value.lastOrNull()?.id != f.tag) {
                    fragments.beginTransaction().setMaxLifecycle(f, Lifecycle.State.STARTED).commit()
                }
            }
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            if (f !is ModalSheetFragment) return
            f.tag?.let(::completeRemoval)
        }
    }

    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        fragments.registerFragmentLifecycleCallbacks(callbacks, false)
    }

    override fun createDestination() = Destination(this)

    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        if (fragments.isStateSaved) return
        check(navOptions?.shouldRestoreState() != true) {
            "This sample does not implement saved sheet back stacks (restoreState)."
        }
        for (entry in entries) {
            val destination = entry.destination as Destination
            val name = destination.className.let {
                if (it.startsWith('.')) context.packageName + it else it
            }
            val fragment = fragments.fragmentFactory.instantiate(context.classLoader, name)
            require(fragment is ModalSheetFragment) { "<sheet> must reference a ModalSheetFragment" }
            fragment.arguments = entry.arguments
            pending[entry.id] = fragment
            val transaction = fragments.beginTransaction().setReorderingAllowed(true)
            state.backStack.value.forEach { old ->
                find(old.id)?.takeIf { it.isAdded }?.let {
                    transaction.setMaxLifecycle(it, Lifecycle.State.STARTED)
                }
            }
            // Do not addToBackStack: that would introduce a second owner of the logical stack.
            transaction.add(containerId, fragment, entry.id).commit()
            state.push(entry)
        }
    }

    override fun onLaunchSingleTop(backStackEntry: NavBackStackEntry) {
        // Reuse the destination/VM; refresh arguments, matching this sample's singleTop contract.
        if (fragments.isStateSaved) return
        find(backStackEntry.id)?.arguments = backStackEntry.arguments
        state.onLaunchSingleTop(backStackEntry)
    }

    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        if (fragments.isStateSaved) return
        check(!savedState) { "This sample does not implement saveState for sheet destinations." }
        popEntries(popUpTo, animate = true)
    }

    /** Called after a user/VM hide has finished. Never blindly pop the controller a second time. */
    fun onSheetHidden(fragment: ModalSheetFragment) {
        if (fragments.isStateSaved) return // Retry on resume after state has been restored.
        val entry = state.backStack.value.firstOrNull { it.id == fragment.tag }
        if (entry != null) popEntries(entry, animate = false) else remove(fragment)
    }

    private fun popEntries(entry: NavBackStackEntry, animate: Boolean) {
        val before = state.backStack.value
        val index = before.indexOfFirst { it.id == entry.id }
        if (index < 0) return
        val popped = before.drop(index)
        val incoming = before.getOrNull(index - 1)
        val incomingWasTransitioning = incoming in state.transitionsInProgress.value
        state.popWithTransition(entry, false)
        if (incoming != null && !incomingWasTransitioning) state.markTransitionComplete(incoming)
        popped.asReversed().forEach { outgoing ->
            val fragment = find(outgoing.id)
            when {
                fragment == null -> state.markTransitionComplete(outgoing)
                animate && popped.size == 1 && fragment.isResumed && fragment.view != null ->
                    fragment.requestClose()
                else -> remove(fragment)
            }
        }
        incoming?.let { find(it.id) }?.takeIf { it.isAdded }?.let {
            fragments.beginTransaction().setMaxLifecycle(it, Lifecycle.State.RESUMED).commit()
        }
    }

    private fun find(id: String) =
        fragments.findFragmentByTag(id) as? ModalSheetFragment ?: pending[id]

    private fun remove(fragment: ModalSheetFragment) {
        val tag = fragment.tag ?: pending.entries.firstOrNull { it.value === fragment }?.key ?: return
        if (!removing.add(tag)) return
        fragments.beginTransaction().remove(fragment).runOnCommit {
            // Reordering can eliminate an add+remove before attachment: no onDestroyed then runs.
            completeRemoval(tag)
        }.commit()
    }

    private fun completeRemoval(tag: String) {
        pending.remove(tag)
        removing.remove(tag)
        state.transitionsInProgress.value.filter { it.id == tag }.forEach(state::markTransitionComplete)
    }

    @NavDestination.ClassType(ModalSheetFragment::class)
    class Destination(navigator: SheetFragmentNavigator) : NavDestination(navigator), FloatingWindow {
        var className: String = ""
            private set

        override fun onInflate(context: Context, attrs: AttributeSet) {
            super.onInflate(context, attrs)
            val values = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.name))
            try {
                className = requireNotNull(values.getString(0)) { "<sheet> requires android:name" }
            } finally {
                values.recycle()
            }
        }
    }
}
