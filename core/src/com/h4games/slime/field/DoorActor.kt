package com.h4games.slime.field

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

class DoorActor(
    val context: GameContext
) : Group() {

    val fg = Image(context.texture("door_fg")).apply {
        originY = 142f
    }

    init {
        addActor(fg)
    }

    fun openDoor() {
        context.sound("garage_open").play()
        fg.addAction(ScaleToAction().apply {
            setScale(1f, 0f)
            duration = 0.3f
        })
    }

    fun closeDoor() {
        context.sound("garage_close").play()
        fg.addAction(ScaleToAction().apply {
            setScale(1f, 1f)
            duration = 0.3f
        })
    }
}