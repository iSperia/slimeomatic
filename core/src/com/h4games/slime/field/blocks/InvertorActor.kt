package com.h4games.slime.field.blocks

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

class InvertorActor(
    context: GameContext,
    size: Float
) : BlockActor(context, size, "block_invertor") {

    lateinit var icon: Image

    override fun initBackground() {
        super.initBackground()

        val sc = size / 400f
        image.width = 200f * sc
        image.height = 400f * sc
        image.x = 100f * sc
    }

    override fun initForeground() {
        super.initForeground()
        val sc = size / 400f

        icon = Image(context.texture("block_invertor_icon")).apply {
            width = 200f * sc
            height = 400f * sc
            x = 100f * sc
        }

        foreground.addActor(icon)
    }
}