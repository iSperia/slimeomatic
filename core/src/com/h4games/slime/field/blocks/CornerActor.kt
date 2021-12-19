package com.h4games.slime.field.blocks

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.GameContext
import ktx.actors.onClick

class CornerActor(
    context: GameContext,
    size: Float,
    var bottomLeft: Boolean
) : BlockActor(context, size, "block_corner") {

    lateinit var flipButton: Image

    init {
        bind(size)
    }

    private fun bind(size: Float) {
        val sc = size / 400f
        image.drawable = if (bottomLeft) TextureRegionDrawable(context.texture("block_corner_alt")) else
            TextureRegionDrawable(context.texture("block_corner"))
        image.width = 300f * sc
        image.height = 300f * sc
        if (bottomLeft) {
            image.x = 100f * sc
            image.y = 100f * sc
        } else {
            image.x = 0f
            image.y = 100f * sc
        }
    }

    override fun initForeground() {
        super.initForeground()
        val sc = size / 400f
        flipButton = Image(context.texture("corner_btn")).apply {
            width = sc * 83f
            height = sc * 82f
            x = 180f * sc
            y = 120f * sc
        }
        foreground.addActor(flipButton)
        flipButton.onClick {
            context.sound("switch").play()
            bottomLeft = !bottomLeft
            bind(size)
        }
    }
}