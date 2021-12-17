package com.h4games.slime

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.ScreenUtils
import com.h4games.slime.field.FieldScreen
import ktx.async.KtxAsync

class SlimeMachineGame() : Game() {

    lateinit var context: GameContext

    override fun create() {
        KtxAsync.initiate()
        context = GameContext(TextureAtlas(Gdx.files.internal("textures.atlas")))

        setScreen(FieldScreen(context))
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 0f)
        super.render()
    }
}