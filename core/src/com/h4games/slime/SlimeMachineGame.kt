package com.h4games.slime

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.ScreenUtils
import com.google.gson.Gson
import com.h4games.slime.field.FieldScreen
import com.h4games.slime.level.GameProgress
import com.h4games.slime.level.LevelChooseScreen
import ktx.async.KtxAsync

class SlimeMachineGame() : Game() {

    lateinit var context: GameContext

    lateinit var progress: GameProgress

    override fun create() {
        KtxAsync.initiate()
        context = GameContext(TextureAtlas(Gdx.files.internal("textures.atlas")))

        val gson = Gson()

        val prefs = Gdx.app.getPreferences("progress")
        progress = if (prefs.contains("progress")) {
            gson.fromJson(prefs.getString("progress"), GameProgress::class.java)
        } else {
            GameProgress(-1)
        }

        setScreen(LevelChooseScreen(context, this))
    }

    fun markLevelComplete(index: Int) {
        if (progress.maxLevelComplete < index) {
            progress = progress.copy(maxLevelComplete = index)
            Gdx.app.getPreferences("progress").putString("progress", Gson().toJson(progress))
            Gdx.app.getPreferences("progress").flush()
        }
    }

    override fun render() {
        ScreenUtils.clear(0f, 0f, 0f, 0f)
        super.render()
    }
}