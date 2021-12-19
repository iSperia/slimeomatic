package com.h4games.slime

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import ktx.actors.repeatForever

class SlimeActor(
    val context: GameContext,
    val isHappy: Boolean,
    val targetColor: Color
) : Group() {

    val fg = Image(context.texture(if (isHappy) "slime_happy_fg" else "slime_sad_fg"))
    val bg = Image(context.texture(if (isHappy) "slime_happy_bg" else "slime_sad_bg"))

    init {
        bg.color = targetColor
        addActor(bg)
        addActor(fg)

        if (isHappy) {
            originX = 75f
            addAction(SequenceAction(
                MoveByAction().apply {
                   setAmount(0f, 40f)
                   duration = 0.2f
                },
                ScaleToAction().apply {
                    setScale(1.1f)
                    duration = 0.1f
                },
                ScaleToAction().apply {
                    setScale(1f)
                    duration = 0.1f
                },
                MoveByAction().apply {
                    setAmount(0f, -40f)
                    duration = 0.1f
                }
            ).repeatForever())
        } else {
            originX = 75f
            addAction(SequenceAction(
                    ScaleToAction().apply {
                        setScale(1.1f, 0.8f)
                        duration = 1.5f
                    },
                    ScaleToAction().apply {
                        setScale(1f, 1f)
                        duration = 0.5f
                    }
            ).repeatForever())
        }
    }
}