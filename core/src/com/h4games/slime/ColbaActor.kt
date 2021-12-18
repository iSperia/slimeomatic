package com.h4games.slime

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image

class ColbaActor(
    private val context: GameContext,
    private val liquidColor: Color
) : Group() {

    val fg = Image(context.texture("colba_empty"))
    val bg = Image(context.texture("colba_liquid"))

    init {
        bg.color = liquidColor
        addActor(bg)
        addActor(fg)
    }
}