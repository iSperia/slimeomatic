package com.h4games.slime.field.panel

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.h4games.slime.GameContext
import com.h4games.slime.field.blocks.*
import ktx.actors.onClick
import ktx.actors.repeatForever

enum class BlockType {
    LIQUID, CORNER, MIXER, INVERTOR, ADDER, REMOVER
}

/**
 * Something that allows some focus
 */
class PanelActor(
    private val context: GameContext
): Group() {

    val image = Image(context.texture("test_panel"))

    val blockLiquidSource = LiquidSourceActor(context, 96f)
    val cornerSource = CornerActor(context, 96f, true)
    val mixer = MixerActor(context, 96f)
    val invertor = InvertorActor(context, 96f)
    val adder = ModificatorActor(context, 96f, true)
    val remover = ModificatorActor(context, 96f, false)

    var blockType: BlockType = BlockType.CORNER

    init {
        addActor(image)

        listOf(blockLiquidSource, cornerSource, mixer, invertor, adder, remover).forEachIndexed { index, block ->
            block.x = (133f - 96f) / 2f
            block.y = 1000f - 133f * (index + 1) - (133f - 96f) / 2f
            addActor(block)
        }

        blockLiquidSource.onClick { setFocusType(BlockType.LIQUID) }
        cornerSource.onClick { setFocusType(BlockType.CORNER) }
        mixer.onClick { setFocusType(BlockType.MIXER) }
        invertor.onClick { setFocusType(BlockType.INVERTOR) }
        adder.onClick { setFocusType(BlockType.ADDER) }
        remover.onClick { setFocusType(BlockType.REMOVER) }

        setFocusType(BlockType.LIQUID)
    }

    private fun setFocusType(blockType: BlockType) {
        if (this.blockType != blockType) {
            when (this.blockType) {
                BlockType.LIQUID -> blockLiquidSource.clearActions()
                BlockType.MIXER -> mixer.clearActions()
                BlockType.CORNER -> cornerSource.clearActions()
                BlockType.INVERTOR -> invertor.clearActions()
                BlockType.ADDER -> adder.clearActions()
                BlockType.REMOVER -> remover.clearActions()
            }
            this.blockType = blockType
            when (this.blockType) {
                BlockType.LIQUID -> blockLiquidSource
                BlockType.MIXER -> mixer
                BlockType.CORNER -> cornerSource
                BlockType.INVERTOR -> invertor
                BlockType.ADDER -> adder
                BlockType.REMOVER -> remover
            }.let { blockActor ->
                blockActor.addAction(SequenceAction(
                    ScaleToAction().apply {
                        setScale(1.2f, 1.2f)
                        duration = 0.1f
                    },
                    ScaleToAction().apply {
                        setScale(0.95f, 0.95f)
                        duration = 0.1f
                    }
                ).repeatForever())
            }
        }
    }
}