package com.h4games.slime.field.blocks

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

sealed class BlockActor(
    val context: GameContext,
    val size: Float,
    val texture: String
) : Group() {

    val background = Group()
    val foreground = Group()

    private var initDone = false

    protected val image = Image(context.texture(texture)).apply {
        width = size
        height = size
    }

    private fun initSelf() {
        addActor(background)
        initBackground()
        addActor(image)
        addActor(foreground)
        initForeground()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (!initDone) {
            initSelf()
        }
    }

    open fun initBackground() {}

    open fun initForeground() {}
}