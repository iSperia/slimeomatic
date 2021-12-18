package com.h4games.slime

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport

abstract class AbstractScreen(
    protected val context: GameContext
) : Screen {

    protected val stage = Stage(ScreenViewport())

    protected val inputMultiplexer = InputMultiplexer()

    override fun show() {
        inputMultiplexer.addProcessor(stage)
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun render(delta: Float) {
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        stage.dispose()
    }
}