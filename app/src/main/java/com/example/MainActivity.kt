package com.example

import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var currentActionMode: ActionMode? = null
    private var shortcutAction by mutableStateOf<String?>(null)

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        currentActionMode = mode
    }

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        if (currentActionMode == mode) {
            currentActionMode = null
        }
    }

    override fun onPause() {
        try {
            currentActionMode?.finish()
            currentActionMode = null
        } catch (_: Throwable) {}
        super.onPause()
    }

    override fun onDestroy() {
        try {
            currentActionMode?.finish()
            currentActionMode = null
        } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        shortcutAction = intent?.getStringExtra("shortcut_action")

        setContent {
            val defaultToolbar = LocalTextToolbar.current
            val safeToolbar = remember(defaultToolbar) {
                object : TextToolbar {
                    override val status: TextToolbarStatus
                        get() = defaultToolbar.status

                    override fun showMenu(
                        rect: Rect,
                        onCopyRequested: (() -> Unit)?,
                        onPasteRequested: (() -> Unit)?,
                        onCutRequested: (() -> Unit)?,
                        onSelectAllRequested: (() -> Unit)?
                    ) {
                        try {
                            defaultToolbar.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
                        } catch (_: Throwable) {}
                    }

                    override fun hide() {
                        try {
                            if (defaultToolbar.status == TextToolbarStatus.Shown) {
                                defaultToolbar.hide()
                            }
                        } catch (_: Throwable) {}
                    }
                }
            }

            CompositionLocalProvider(LocalTextToolbar provides safeToolbar) {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = BackgroundDark
                    ) {
                        AppNavigation(initialShortcutAction = shortcutAction)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutAction = intent.getStringExtra("shortcut_action")
    }
}

