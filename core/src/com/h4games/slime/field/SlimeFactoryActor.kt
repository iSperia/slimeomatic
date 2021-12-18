package com.h4games.slime.field

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import ktx.actors.repeatForever
import kotlin.random.Random

class SlimeFactoryActor(
    val context: GameContext
) : Group() {

    val image = Image(context.texture("garage")).apply {
        width = FieldActor.BASE_WIDTH
        height = FieldActor.BASE_HEIGHT
    }

    val bgLeft = Image(context.texture("bg_left")).apply {
        x = -416f
        originX = 457f
        originY = 67f

    }
    val bgRight = Image(context.texture("bg_right")).apply {
        x = 391f
    }

    init {
        addActor(bgLeft)
        addActor(bgRight)
        addActor(image)

        bgLeft.addAction(
            SequenceAction(
                ScaleToAction().apply {
                    setScale(1.03f, 0.95f)
                    duration = 0.6f
                },
                ScaleToAction().apply {
                    setScale(1f, 1f)
                    duration = 0.6f
                }
            ).repeatForever()
        )

        val ox = bgRight.x
        val oy = bgRight.y
        bgRight.addAction(
            SequenceAction(
                DelayAction(0.08f),
                RunnableAction().apply {
                    setRunnable {
                        bgRight.x = ox - 3f + 6f * Random.nextFloat()
                        bgRight.y = oy - 3f + 6f * Random.nextFloat()
                    }
                }
            ).repeatForever()
        )
    }

}