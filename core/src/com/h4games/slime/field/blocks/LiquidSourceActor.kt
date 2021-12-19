package com.h4games.slime.field.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import ktx.actors.onClick

class LiquidSourceActor(
    context: GameContext,
    size: Float,
    val colors: List<Color>
) : BlockActor(context, size, "liquid_source") {

    lateinit var liquidImage: Image

    lateinit var refreshButton: Image

    var colorActiveIndex = 0

    override fun initBackground() {
        super.initBackground()

        val sc = size / 400f
        image.width = 200f * sc
        image.height = 300f * sc
        image.x = 100f * sc

        liquidImage = Image(context.texture("block_liquid_bg")).apply {
            width = 200f * sc
            height = 300f * sc
            x = 100f * sc
            color = colors[colorActiveIndex]
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
        val sc = size / 400f
        refreshButton = Image(context.texture("liquid_btn")).apply {
            width = 91f * sc
            height = 77f * sc
            x = 162f * sc
            y = 73f * sc
        }
        foreground.addActor(refreshButton)
        refreshButton.onClick {
            context.sound("switch").play()
            colorActiveIndex = (colorActiveIndex + 1) % colors.size
            liquidImage.color = colors[colorActiveIndex]
        }
    }
}