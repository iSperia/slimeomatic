package com.h4games.slime.field

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.AbstractScreen
import com.h4games.slime.GameContext
import com.h4games.slime.SlimeMachineGame
import com.h4games.slime.field.panel.PanelActor
import com.h4games.slime.level.LevelConfig
import kotlin.math.abs

class FieldScreen(
    context: GameContext,
    private val level: LevelConfig,
    private val game: SlimeMachineGame,
    private val levelIndex: Int
) : AbstractScreen(context) {

    lateinit var field: FieldActor

    lateinit var panel: PanelActor

    val clickProcessor = object : InputProcessor {

        private var downTimestamp = 0L
        private var clickX: Int = 0
        private var clickY: Int = 0

        override fun keyDown(keycode: Int): Boolean = false

        override fun keyUp(keycode: Int): Boolean = false

        override fun keyTyped(character: Char): Boolean = false

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == 0 || button == 1) {
                clickX = screenX
                clickY = screenY
                downTimestamp = System.currentTimeMillis()
                return true
            }
            return false
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (button == 0) {
                val stamp = System.currentTimeMillis() - downTimestamp
                val x = screenX
                val y = screenY
                if (stamp - downTimestamp < 0.5f && abs(x - clickX) < 10f && abs(y - clickY) < 10f) {
                    val availabilityResult = field.isAreaAvailable(x, y)
                    if (availabilityResult.available) {
                        field.createBlock(availabilityResult.x, availabilityResult.y, panel.blockType)
                    }
                }
                return true
            } else if (button == 1) {
                val stamp = System.currentTimeMillis() - downTimestamp
                val x = screenX
                val y = screenY
                if (stamp - downTimestamp < 0.5f && abs(x - clickX) < 10f && abs(y - clickY) < 10f) {
                    field.processRemoveBlock(x, y)
                }
            }
            return false
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean = false

        override fun scrolled(amountX: Float, amountY: Float): Boolean = false
    }

    override fun show() {
        super.show()

        inputMultiplexer.addProcessor(clickProcessor)

        val bg = Image(context.texture("bg_screen")).apply {
            width = Gdx.graphics.width.toFloat()
            height = Gdx.graphics.height.toFloat()
        }
        stage.addActor(bg)

        field = FieldActor(context, level.targetColor.let { Color(it.r, it.g, it.b, 1f) }, game, levelIndex, level)
        stage.addActor(field)

        panel = PanelActor(context, level.blocks).apply {
            x = Gdx.graphics.width - PanelActor.PANEL_SIZE
        }
        stage.addActor(panel)

    }
}