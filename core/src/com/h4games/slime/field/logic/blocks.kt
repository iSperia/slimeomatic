package com.h4games.slime.field.logic

val CONNECTION_TOP = 0b0001
val CONNECTION_RIGHT = 0b0010
val CONNECTION_BOTTOM = 0b0100
val CONNECTION_LEFT = 0b1000

typealias Connections = Int

fun Connections.isDirectionNeeded(dx: Int, dy: Int) = if (dx == -1) this and CONNECTION_LEFT > 0
    else if (dx == 1) this and CONNECTION_RIGHT > 0
    else if (dy == -1) this and CONNECTION_BOTTOM > 0
    else this and CONNECTION_TOP > 0

sealed class FieldBlock(
    val x: Int,
    val y: Int) {

    abstract fun getTexture(): String
    abstract fun getConnections(): Connections

}
