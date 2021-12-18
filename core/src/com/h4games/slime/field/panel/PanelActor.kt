package com.h4games.slime.field.panel

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import com.h4games.slime.field.blocks.*
import ktx.actors.onClick
import ktx.actors.repeatForever

enum class BlockType {
    LIQUID, CORNER, MIXER, INVERTOR, ADDER, REMOVER, HOR_PIPE
}

/**
 * Something that allows some focus
 */
class PanelActor(
    private val context: GameContext,
    private val blocks: List<BlockType>
): Group() {

    val image = Image(context.texture("test_panel")).apply {
        width = PANEL_SIZE
        height = Gdx.graphics.height.toFloat()
    }

    val blockLiquidSource = LiquidSourceActor(context, 96f, listOf(Color.WHITE))
    val cornerSource = CornerActor(context, 96f, true)
    val mixer = MixerActor(context, 96f)
    val invertor = InvertorActor(context, 96f)
    val adder = ModificatorActor(context, 96f, true, listOf(Color.WHITE))
    val remover = ModificatorActor(context, 96f, false, listOf(Color.WHITE))
    val horPipe = HorizontalPipeActor(context, 96f)

    var blockType: BlockType = BlockType.CORNER

    init {
        addActor(image)

        BlockType.values().filter { blocks.contains(it) }.forEachIndexed { index, blockType ->
            val block = when (blockType) {
                BlockType.LIQUID -> blockLiquidSource
                BlockType.REMOVER -> remover
                BlockType.ADDER -> adder
                BlockType.INVERTOR -> invertor
                BlockType.CORNER -> cornerSource
                BlockType.MIXER -> mixer
                BlockType.HOR_PIPE -> horPipe
            }
            block.x = (PANEL_SIZE - 96f) / 2f
            block.y = Gdx.graphics.height - PANEL_SIZE * (index + 1) + (PANEL_SIZE - 96f) / 2f
            addActor(block)
        }

        blockLiquidSource.onClick { setFocusType(BlockType.LIQUID) }
        cornerSource.onClick { setFocusType(BlockType.CORNER) }
        mixer.onClick { setFocusType(BlockType.MIXER) }
        invertor.onClick { setFocusType(BlockType.INVERTOR) }
        adder.onClick { setFocusType(BlockType.ADDER) }
        remover.onClick { setFocusType(BlockType.REMOVER) }
        horPipe.onClick { setFocusType(BlockType.HOR_PIPE) }

        setFocusType(BlockType.LIQUID)
    }

    private fun setFocusType(blockType: BlockType) {
        context.sound("switch").play()
        if (this.blockType != blockType) {
            when (this.blockType) {
                BlockType.LIQUID -> blockLiquidSource.clearActions()
                BlockType.MIXER -> mixer.clearActions()
                BlockType.CORNER -> cornerSource.clearActions()
                BlockType.INVERTOR -> invertor.clearActions()
                BlockType.ADDER -> adder.clearActions()
                BlockType.REMOVER -> remover.clearActions()
                BlockType.HOR_PIPE -> horPipe.clearActions()
            }
            this.blockType = blockType
            when (this.blockType) {
                BlockType.LIQUID -> blockLiquidSource
                BlockType.MIXER -> mixer
                BlockType.CORNER -> cornerSource
                BlockType.INVERTOR -> invertor
                BlockType.ADDER -> adder
                BlockType.REMOVER -> remover
                BlockType.HOR_PIPE -> horPipe
            }.let { blockActor ->
                blockActor.addAction(SequenceAction(
                    ScaleToAction().apply {
                        setScale(1.03f, 1.03f)
                        duration = 0.2f
                    },
                    ScaleToAction().apply {
                        setScale(0.97f, 0.97f)
                        duration = 0.2f
                    }
                ).repeatForever())
            }
        }
    }

    companion object {
        const val PANEL_SIZE = 160f
    }
}