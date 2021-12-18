package com.h4games.slime

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.ui.Label

data class GameContext(
    val atlas: TextureAtlas
) {
    private val sounds = mutableMapOf<String, Sound>()
    private val music = mutableMapOf<String, Music>()

    val font = BitmapFont(Gdx.files.internal("cuprum.fnt"))

    val tutorialStyle = Label.LabelStyle().apply {
        font = this@GameContext.font
        fontColor = Color.WHITE
    }

    fun texture(name: String) = atlas.findRegion(name)

    fun textures(id: String) = atlas.findRegions(id)
    
    fun sound(name: String) = sounds[name] ?: Gdx.audio.newSound(Gdx.files.internal("sound/$name.ogg")).also { sounds[name] = it }

    fun music(name: String) = music[name] ?: Gdx.audio.newMusic(Gdx.files.internal("sound/$name.ogg")).also { music[name] = it }
}