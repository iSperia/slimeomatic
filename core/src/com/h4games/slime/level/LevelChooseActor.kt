package com.h4games.slime.level

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import ktx.actors.repeatForever

class LevelChooseActor(
    private val context: GameContext,
    private val targetColor: Color,
    private val isLocked: Boolean,
    private val isPulsing: Boolean
) : Group() {

    val image = Image(context.texture("level_chooser_bg")).apply {
        width = 100f
        height = 100f
        color = targetColor
    }
    val fg = Image(context.texture("level_chooser_fg")).apply {
        width = 100f
        height = 100f
    }

    val lock = Image(context.texture("lock")).apply {
        x = 19f
        y = 12f
        isVisible = false
    }

    init {
        originX = 50f
        originY = 50f

        addActor(image)
        addActor(fg)
        addActor(lock)

        if (isLocked) {
            lock.isVisible = true
        }

        if (isPulsing) {
            addAction(SequenceAction(
                ScaleToAction().apply {
                    setScale(1.05f, 1.05f)
                    duration = 0.3f
                },
                ScaleToAction().apply {
                    setScale(0.98f, 0.98f)
                    duration = 0.3f
                }
            ).repeatForever())
        }
    }
}