package com.h4games.slime.field

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

class LauncherActor(
    private val context: GameContext
) : Group() {

    val launcherImage = Image(context.texture("launcher_off"))
    val launcherStick = Image(context.texture("launcher_stick")).apply {
        x = -58f
        y = 39f
        originX = 121f
        originY = 20f
    }

    init {
        addActor(launcherImage)
        addActor(launcherStick)
    }
}