package com.h4games.slime.level

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.google.gson.Gson
import com.h4games.slime.AbstractScreen
import com.h4games.slime.GameContext
import com.h4games.slime.SlimeMachineGame
import com.h4games.slime.field.FieldScreen
import ktx.actors.onClick

class LevelChooseScreen(
    context: GameContext,
    private val game: SlimeMachineGame
) : AbstractScreen(context) {

    val levels = Group()

    lateinit var config: LevelsConfig

    override fun show() {
        super.show()

        val bg = Image(context.texture("bg_main")).apply {
            width = Gdx.graphics.width.toFloat()
            height = Gdx.graphics.height.toFloat()
        }
        stage.addActor(bg)


        stage.addActor(levels)

        val gson = Gson()



        config = gson.fromJson<LevelsConfig>(Gdx.files.internal("levels.json").readString(), LevelsConfig::class.java)
        config.levels.forEachIndexed { index, level ->
            val xx = index % 10
            val yy = index / 10
            val levelActor = LevelChooseActor(context, level.targetColor.let { Color(it.r, it.g, it.b, 1f)}, index > game.progress.maxLevelComplete + 1, index == game.progress.maxLevelComplete + 1).apply {
                x = 140f + xx * (100f + 300f / 9f)
                y = 280 - yy * (100f + 300f / 9f)
            }
            levels.addActor(levelActor)
            if (index <= game.progress.maxLevelComplete + 1) {
                levelActor.onClick {
                    context.sound("enter").play()
                    game.screen = FieldScreen(context, level, game, index)
                }
            }
        }
    }

}