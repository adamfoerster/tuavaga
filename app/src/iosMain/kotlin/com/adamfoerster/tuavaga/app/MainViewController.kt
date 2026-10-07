package com.adamfoerster.tuavaga.app

import androidx.compose.ui.window.ComposeUIViewController
import com.adamfoerster.tuavaga.app.di.initKoin

@Suppress("FunctionName", "unused") // Called from Swift
fun MainViewController() = ComposeUIViewController(
    configure = { initKoin() },
) {
    App()
}
