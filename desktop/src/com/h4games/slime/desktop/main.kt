package com.h4games.slime.desktop

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.h4games.slime.SlimeMachineGame

fun main() {
    val config = LwjglApplicationConfiguration().apply {
        width = 1600
        height = 1000
        resizable = false
        fullscreen = false
    }

    LwjglApplication(SlimeMachineGame(), config)
}