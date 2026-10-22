package dev.better.sheet.sample.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavHostController
import androidx.navigation.fragment.NavHostFragment
import dev.better.sheet.sample.R

class SheetNavHostFragment : NavHostFragment() {
    internal lateinit var sheets: SheetFragmentNavigator
        private set

    override fun onCreateNavHostController(navHostController: NavHostController) {
        super.onCreateNavHostController(navHostController)
        sheets = SheetFragmentNavigator(requireContext(), childFragmentManager, R.id.navigation_sheet_container)
        navHostController.navigatorProvider.addNavigator(sheets)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).apply {
            // Preserve the normal FragmentNavigator's container and its stable ID.
            addView(super.onCreateView(inflater, container, savedInstanceState), FrameLayout.LayoutParams(-1, -1))
            addView(FragmentContainerView(context).apply {
                id = R.id.navigation_sheet_container
            }, FrameLayout.LayoutParams(-1, -1))
        }
}
