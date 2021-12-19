package com.h4games.slime.field.blocks

import com.h4games.slime.GameContext

class HorizontalPipeActor(
    context: GameContext,
    size: Float
) : BlockActor(context, size, "block_hor_pipe") {

    override fun initBackground() {
        super.initBackground()

        val sc = size / 400f
        image.width = 400f * sc
        image.height = 200f * sc
        image.y = 100f * sc
    }

}