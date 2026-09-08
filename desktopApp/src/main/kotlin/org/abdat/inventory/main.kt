package org.abdat.inventory

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.abdat.inventory.di.initDesktopKoin

fun main() {
    initDesktopKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "InventaireMed",
        ) {
            App()
        }
    }
}