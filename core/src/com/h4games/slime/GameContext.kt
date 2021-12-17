package com.h4games.slime

import com.badlogic.gdx.graphics.g2d.TextureAtlas

data class GameContext(
    val atlas: TextureAtlas
) {
    fun texture(name: String) = atlas.findRegion(name)

    fun textures(id: String) = atlas.findRegions(id)
}