package com.h4games.slime.field.blocks

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

class MixerActor(
    context: GameContext,
    size: Float
) : BlockActor(context, size, "block_mixer") {

    lateinit var icon: Image

    override fun initBackground() {
        super.initBackground()

        val sc = size / 400f
        image.width = 400f * sc
        image.height = 315f * sc
    }

    override fun initForeground() {
        super.initForeground()
        val sc = size / 400f

        val ww = 82f * sc
        val hh = 85f * sc
        icon = Image(context.texture("block_mixer_icon")).apply {
            width = ww
            height = hh
            originX = ww / 2
            originY = hh / 2
            x = 160f * sc
            y = 160f * sc
        }

        foreground.addActor(icon)
    }
}