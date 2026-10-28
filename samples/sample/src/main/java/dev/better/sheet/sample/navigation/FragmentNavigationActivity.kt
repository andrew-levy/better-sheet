package dev.better.sheet.sample.navigation

import android.os.Bundle
import android.graphics.Color
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import dev.better.sheet.sample.R

class FragmentNavigationActivity : FragmentActivity() {
    val host: NavHostFragment
        get() = supportFragmentManager.findFragmentByTag("navigation-host") as NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContentView(R.layout.fragment_navigation)
    }
}

/** Illustrative app helper. Keep your own navigateSafe implementation when integrating. */
internal fun NavController.navigateSafe(action: Int, args: Bundle? = null, options: NavOptions? = null) {
    // Avoid repeating an action from a screen which has already been covered/navigated away from.
    if (currentBackStackEntry?.lifecycle?.currentState != androidx.lifecycle.Lifecycle.State.RESUMED) return
    navigate(action, args, options)
}
