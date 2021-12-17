package com.h4games.slime.field.logic

import com.badlogic.gdx.graphics.Color

data class FieldLaunchResult(
    val result: Color
)

class FieldConfiguration {

    /**
     * All the blocks user placed on the field (and predefined by level)
     */
    val blocks = mutableListOf<FieldBlock>()

    /**
     * Validate field if all blocks and connections are filled
     */
    fun validate(): Boolean = TODO("Not implemented yet")

    fun launch(): FieldLaunchResult = TODO("Not implemented yet")
}