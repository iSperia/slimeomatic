package com.h4games.slime.field.blocks

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.GameContext
import ktx.actors.onClick

class ModificatorActor(
    context: GameContext,
    size: Float,
    val isAdder: Boolean,
    val colors: List<Color>
) : BlockActor(context, size, if (isAdder) "block_adder" else "block_remover") {
    lateinit var liquidImage: Image
    lateinit var liquidImage2: Image

    lateinit var refreshButton: Button

    var colorActiveIndex = 0

    override fun initBackground() {
        super.initBackground()

        val sc = size / 400f
        image.width = 284f * sc
        image.height = 400f * sc
        image.x = 63f * sc

        liquidImage = Image(context.texture(if (isAdder) "plus_liquid" else "minus_liquid")).apply {
            width = 284f * sc
            height = 400f * sc
            color = colors[colorActiveIndex]
            x = 63f * sc
        }
        background.addActor(liquidImage)

    }

    override fun initForeground() {
        super.initForeground()
        val sc = size / 400f
        liquidImage2 = Image(context.texture(if (isAdder) "plus_liquid_alt" else "minus_liquid_alt")).apply {
            width = 284f * sc
            height = 400f * sc
            color = colors[colorActiveIndex]
            x = 63f * sc
        }
        foreground.addActor(liquidImage2)

        refreshButton = Button(TextureRegionDrawable(context.texture(if (isAdder) "plus_btn" else "minus_btn")), TextureRegionDrawable(context.texture(if (isAdder) "plus_btn_pressed" else "minus_btn_pressed"))).apply {
            width = 284f * sc
            height = 400f * sc
            x = 63f * sc
        }
        foreground.addActor(refreshButton)
        refreshButton.onClick {
            context.sound("switch").play()
            colorActiveIndex = (colorActiveIndex + 1) % colors.size
            liquidImage.color = colors[colorActiveIndex]
            liquidImage2.color = colors[colorActiveIndex]
        }
    }
}