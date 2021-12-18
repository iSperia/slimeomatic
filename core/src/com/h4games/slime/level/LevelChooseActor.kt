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

    val image = Image(context.texture("level")).apply {
        x = 14f
        y = 14f
        color = targetColor
    }

    val lock = Image(context.texture("lock")).apply {
        x = 14f
        y = 14f
        isVisible = false
    }

    init {
        originX = 50f
        originY = 50f

        addActor(image)
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