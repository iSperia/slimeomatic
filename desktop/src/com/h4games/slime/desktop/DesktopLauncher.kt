package com.h4games.slime.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.h4games.slime.SlimeMachineGame

class DesktopLauncher {
    companion object {
        @JvmStatic fun main(args : Array<String>) {
            val config = LwjglApplicationConfiguration().apply {
                width = 1600
                height = 1000
                title = "SLIME-O-MATIC"
                x = 0
                y = 0
            }
//            val config = Lwjgl3ApplicationConfiguration().apply {
//                setWindowedMode(1600, 1000)
//                setWindowSizeLimits(1600, 1000, 1600, 1000)
//                setTitle("SLIME-O_MATIC")
//            }

            LwjglApplication(SlimeMachineGame(), config)
        }
    }
}
