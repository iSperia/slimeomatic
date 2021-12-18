package com.h4games.slime.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.h4games.slime.SlimeMachineGame

class DesktopLauncher {
    companion object {
        @JvmStatic fun main(args : Array<String>) {
            val config = Lwjgl3ApplicationConfiguration().apply {
                setWindowSizeLimits(1600, 1000, 1600, 1000)
            }

            Lwjgl3Application(SlimeMachineGame(), config)
        }
    }
}
