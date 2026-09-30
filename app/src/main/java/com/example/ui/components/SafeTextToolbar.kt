package com.example.ui.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * A safe TextToolbar implementation that prevents Android's DecorView from logging:
 * "Destroying unexpected ActionMode instance of TYPE_FLOATING... was not the current floating action mode! Expected null"
 *
 * This occurs when Compose text fields or bottom sheets trigger a floating action mode (cut/copy/paste popup).
 * When user taps outside or switches focus, DecorView dismisses and destroys the floating action mode.
 * Standard Compose FloatingTextToolbar retains its reference and calls actionMode.finish() again inside hide(),
 * causing DecorView to complain that an already-destroyed ActionMode was finished twice.
 *
 * SafeFloatingTextToolbar tracks the active ActionMode and nullifies its reference immediately inside
 * onDestroyActionMode, ensuring finish() is strictly invoked at most once.
 */
class SafeFloatingTextToolbar(private val view: View) : TextToolbar {
    private var currentActionMode: ActionMode? = null
    private var currentRect: Rect = Rect.Zero
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        currentRect = rect
        val existingMode = currentActionMode
        if (existingMode != null) {
            try {
                existingMode.invalidateContentRect()
            } catch (_: Throwable) {}
            return
        }

        if (!view.isAttachedToWindow) return

        val callback = object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (menu == null || mode == null) return false
                if (onCutRequested != null) {
                    menu.add(Menu.NONE, android.R.id.cut, Menu.NONE, android.R.string.cut)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                if (onCopyRequested != null) {
                    menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, android.R.string.copy)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                if (onPasteRequested != null) {
                    menu.add(Menu.NONE, android.R.id.paste, Menu.NONE, android.R.string.paste)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                if (onSelectAllRequested != null) {
                    menu.add(Menu.NONE, android.R.id.selectAll, Menu.NONE, android.R.string.selectAll)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                }
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    android.R.id.cut -> onCutRequested?.invoke()
                    android.R.id.copy -> onCopyRequested?.invoke()
                    android.R.id.paste -> onPasteRequested?.invoke()
                    android.R.id.selectAll -> onSelectAllRequested?.invoke()
                    else -> return false
                }
                hide()
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                // DecorView destroyed the action mode. Clear our reference so hide() will never call finish() again!
                currentActionMode = null
                status = TextToolbarStatus.Hidden
            }

            override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
                outRect?.set(
                    currentRect.left.toInt(),
                    currentRect.top.toInt(),
                    currentRect.right.toInt(),
                    currentRect.bottom.toInt()
                )
            }
        }

        status = TextToolbarStatus.Shown
        try {
            currentActionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        } catch (_: Throwable) {
            currentActionMode = null
            status = TextToolbarStatus.Hidden
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        val mode = currentActionMode
        currentActionMode = null
        if (mode != null) {
            try {
                mode.finish()
            } catch (_: Throwable) {}
        }
    }
}

@Composable
fun ProvideSafeTextToolbar(content: @Composable () -> Unit) {
    val view = LocalView.current
    val safeToolbar = remember(view) { SafeFloatingTextToolbar(view) }
    CompositionLocalProvider(LocalTextToolbar provides safeToolbar) {
        content()
    }
}
