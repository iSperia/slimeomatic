package com.h4games.slime.field.blocks

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import ktx.actors.onClick

class CornerActor(
    context: GameContext,
    size: Float,
    var bottomLeft: Boolean
) : BlockActor(context, size, "test_corner_bottom_left") {

    lateinit var flipButton: Image

    init {
        bind(size)
    }

    private fun bind(size: Float) {
        if (!bottomLeft) {
            image.scaleX = -1f
            image.x += size
        } else {
            image.scaleX = 1f
            image.x = 0f
        }
    }

    override fun initForeground() {
        super.initForeground()
        flipButton = Image(context.texture("test_flip")).apply {
            width = size * 12f / 72f
            height = size * 12f / 72f
            x = size * 12f / 72f
            y = size * 12f / 72f
        }
        addActor(flipButton)
        flipButton.onClick {
            bottomLeft = !bottomLeft
            bind(size)
        }
    }
}