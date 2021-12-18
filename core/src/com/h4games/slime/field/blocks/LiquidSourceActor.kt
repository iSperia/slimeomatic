package com.h4games.slime.field.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import ktx.actors.onClick

class LiquidSourceActor(
    context: GameContext,
    size: Float
) : BlockActor(context, size, "test_bottom") {

    lateinit var liquidImage: Image

    lateinit var refreshButton: Image

    var colorActiveIndex = 0

    override fun initBackground() {
        super.initBackground()

        liquidImage = Image(context.texture("test_liquid")).apply {
            width = size
            height = size
            color = COLORS[colorActiveIndex]
        }
        background.addActor(liquidImage)
    }

    fun hideLiquid() {
        background.isVisible = false
    }

    fun showLiquid() {
        background.isVisible = true
    }

    override fun initForeground() {
        super.initForeground()
        refreshButton = Image(context.texture("test_refresh")).apply {
            width = size * 12f / 72f
            height = size * 12f / 72f
            x = size * 12f / 72f
            y = size * 12f / 72f
        }
        foreground.addActor(refreshButton)
        refreshButton.onClick {
            colorActiveIndex = (colorActiveIndex + 1) % COLORS.size
            liquidImage.color = COLORS[colorActiveIndex]
        }
    }

    companion object {
        val COLORS = listOf<Color>(Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLACK, Color.WHITE)
    }
}