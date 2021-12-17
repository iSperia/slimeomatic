package com.h4games.slime.field

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext

class SlimeFactoryActor(
    val context: GameContext
) : Group() {

    val image = Image(context.texture("base")).apply {
        width = FieldActor.BASE_WIDTH
        height = FieldActor.BASE_HEIGHT
    }

    init {
        addActor(image)
    }

}