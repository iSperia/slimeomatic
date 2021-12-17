package com.h4games.slime.field

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.GameContext
import com.h4games.slime.field.blocks.*
import com.h4games.slime.field.panel.BlockType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync
import kotlin.math.max
import kotlin.math.min

data class AreaAvailabilityResult(
    val x: Int,
    val y: Int,
    val available: Boolean
)

/**
 * Something that draws the field blocks and slime bases, and controls
 * It scrolls and scales
 */
class FieldActor(
    private val context: GameContext
) : Group() {

    val animations = Group()
    val blocks = Group()
    var cursor: Image? = null

    var slime: Image? = null

    val warnings = Group()

    val blocksMap = mutableMapOf<Pair<Int, Int>, BlockActor>()

    init {
        addActor(animations)
        addActor(blocks)
        addActor(warnings)

        val baseBlock = SlimeFactoryActor(context).apply {
            x = OBJECT_SIZE * 4f
            y = 0f
        }
        blocks.addActor(baseBlock)

        val launcherBlock = Button(
            context.texture("launcher_off").let { TextureRegionDrawable(it) },
            context.texture("launcher_on").let { TextureRegionDrawable(it) }
        ).apply {
            x = baseBlock.x - OBJECT_SIZE * 2f
            y = 0f
        }
        launcherBlock.onClick {
            val warnings = validateBlocks()
            if (warnings.isNotEmpty()) {
                warnings.forEach { coords ->
                    val warningImage = Image(context.texture("warning")).apply {
                        width = OBJECT_SIZE / 2f
                        height = OBJECT_SIZE / 2f
                        x = coords.x - width / 2f
                        y = coords.y - height  / 2f
                    }
                    this@FieldActor.warnings.addActor(warningImage)
                    warningImage.addAction(DelayAction(1f).apply {
                        action = RunnableAction().apply {
                            setRunnable {
                                warningImage.remove()
                            }
                        }
                    })
                }
            } else {
                val targetColor = calculateColor(5, -1)
                slime?.let { it.remove() }
                slime = Image(context.texture("slime")).apply {
                    width = OBJECT_SIZE * 1.2f
                    height = OBJECT_SIZE * 1.2f
                    x = baseBlock.x + OBJECT_SIZE
                    y = baseBlock.y
                    color = targetColor
                }
                addActor(slime)
                animateLiquid(5, 0)
            }
        }
        blocks.addActor(launcherBlock)
    }

    fun placeBlock(block: BlockActor, x: Int, y: Int) {
        block.x = x * OBJECT_SIZE
        block.y = BASELINE + y * OBJECT_SIZE
    }

    override fun act(delta: Float) {
        super.act(delta)
        val x = Gdx.input.x
        val y = Gdx.input.y
        if (x < Gdx.graphics.width - 144f) {
            val inputCoords = screenToLocalCoordinates(Vector2(x.toFloat(), y.toFloat()))
            val xx = (inputCoords.x / OBJECT_SIZE).toInt()
            val yy = ((inputCoords.y - BASELINE) / OBJECT_SIZE).toInt()
            if (!blocksMap.containsKey(Pair(xx, yy))) {
                cursor?.let { it.remove() }
                cursor = Image(context.texture("cursor_ok")).apply {
                    this.x = xx * OBJECT_SIZE
                    this.y = BASELINE + yy * OBJECT_SIZE
                    this.width = OBJECT_SIZE
                    this.height = OBJECT_SIZE
                    this.touchable = Touchable.disabled
                }
                addActor(cursor)
            } else {
                cursor?.let { it.remove() }
            }
        } else {
            cursor?.let { it.remove() }
        }
    }

    fun isAreaAvailable(x: Int, y: Int): AreaAvailabilityResult {
        if (x < Gdx.graphics.width - 144f) {
            val inputCoords = screenToLocalCoordinates(Vector2(x.toFloat(), y.toFloat()))
            val xx = (inputCoords.x / OBJECT_SIZE).toInt()
            val yy = ((inputCoords.y - BASELINE) / OBJECT_SIZE).toInt()
            if (xx >= 0 && yy >= 0 && !blocksMap.containsKey(Pair(xx, yy))) {
                return AreaAvailabilityResult(xx, yy, true)
            }
        }
        return AreaAvailabilityResult(0, 0, false)
    }

    fun createBlock(x: Int, y: Int, blockType: BlockType) {
        when (blockType) {
            BlockType.MIXER -> MixerActor(context, OBJECT_SIZE)
            BlockType.CORNER -> CornerActor(context, OBJECT_SIZE, true)
            BlockType.LIQUID -> LiquidSourceActor(context, OBJECT_SIZE)
            BlockType.INVERTOR -> InvertorActor(context, OBJECT_SIZE)
            BlockType.ADDER -> ModificatorActor(context, OBJECT_SIZE, true)
            BlockType.REMOVER -> ModificatorActor(context, OBJECT_SIZE, false)
        }.let {
            blocks.addActor(it)
            placeBlock(it, x, y)
            blocksMap[Pair(x, y)] = it
        }
    }

    fun processRemoveBlock(x: Int, y: Int) {
        if (x < Gdx.graphics.width - 144f) {
            val inputCoords = screenToLocalCoordinates(Vector2(x.toFloat(), y.toFloat()))
            val xx = (inputCoords.x / OBJECT_SIZE).toInt()
            val yy = ((inputCoords.y - BASELINE) / OBJECT_SIZE).toInt()
            if (xx >= 0 && yy >= 0) {
                val coords = Pair(xx, yy)
                blocksMap[coords]?.let { actor ->
                    actor.remove()
                }
                blocksMap.remove(coords)
            }
        }
    }

    fun findAverageCoord(x1: Int, y1: Int, x2: Int, y2: Int): Vector2 {
        return Vector2(OBJECT_SIZE * (x1.toFloat() + x2.toFloat()) / 2f + OBJECT_SIZE / 2f, BASELINE + OBJECT_SIZE * (y1.toFloat() + y2.toFloat()) / 2f + OBJECT_SIZE / 2f)
    }

    fun validateBlocks(): List<Vector2> {
        var result = mutableListOf<Vector2>()
        if (blocksMap.isEmpty()) {
            result.add(findAverageCoord(5, -1, 5, 0))
        }
        blocksMap.forEach { xy, block ->
            when (block) {
                is LiquidSourceActor -> {
                    if (!checkBlockExists(xy.first, xy.second - 1)) result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second - 1))
                }
                is CornerActor -> {
                    if (!checkBlockExists(xy.first, xy.second + 1)) result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second + 1))
                    if (block.bottomLeft && !checkBlockExists(xy.first + 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first + 1, xy.second))
                    if (!block.bottomLeft && !checkBlockExists(xy.first - 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first - 1, xy.second))
                }
                is MixerActor -> {
                    if (!checkBlockExists(xy.first - 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first - 1, xy.second))
                    if (!checkBlockExists(xy.first + 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first + 1, xy.second))
                    if (!checkBlockExists(xy.first, xy.second - 1)) result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second - 1))
                }
                is InvertorActor -> {
                    if (!checkBlockExists(xy.first, xy.second + 1))  result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second + 1))
                    if (!checkBlockExists(xy.first, xy.second - 1))  result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second - 1))
                }
                is ModificatorActor -> {
                    if (!checkBlockExists(xy.first, xy.second + 1))  result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second + 1))
                    if (!checkBlockExists(xy.first, xy.second - 1))  result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second - 1))
                }
            }
        }
        return result
    }

    private fun showLiquidAnimation(x: Int, y: Int, color: Color) {
        val animation = AnimatedActor(context, "liquid_source_anim", 1/3f).apply {
            this.x = x * OBJECT_SIZE
            this.y = BASELINE + y * OBJECT_SIZE
            this.width = OBJECT_SIZE
            this.height = OBJECT_SIZE
            this.color = color
        }
        animations.addActor(animation)
    }

    private fun showPipeAnimationBottom(x: Int, y: Int, color: Color) {
        val animation = AnimatedActor(context, "vpipe", 1/3f).apply {
            this.x = x * OBJECT_SIZE + OBJECT_SIZE * 25f / 72f
            this.y = BASELINE + y * OBJECT_SIZE - OBJECT_SIZE * 21f / 72f
            this.width = OBJECT_SIZE * 23f / 72f
            this.height = OBJECT_SIZE * 44f / 72f
            this.color = color
        }
        animations.addActor(animation)
    }

    private fun showPipeAnimationHorizontal(x: Int, y: Int, color: Color, inverted: Boolean) {
        val animation = AnimatedActor(context, "hpipe", 1/3f).apply {
            this.x = x * OBJECT_SIZE + if (inverted) -20f / 72f * OBJECT_SIZE else OBJECT_SIZE - 20f / 72f * OBJECT_SIZE
            this.y = BASELINE + y * OBJECT_SIZE + OBJECT_SIZE * 24f / 72f
            this.width = OBJECT_SIZE * 44f / 72f
            this.height = OBJECT_SIZE * 23f / 72f
            this.color = color
            this.inverted = inverted
        }
        animations.addActor(animation)
    }

    data class AnimationMetadata(
        val delay: Long,
        val color: Color
    )

    fun animateLiquid(x: Int, y: Int): AnimationMetadata {
        val block = blocksMap[Pair(x,y)]!!
        when (block) {
            is LiquidSourceActor -> {
                block.hideLiquid()
                showLiquidAnimation(x, y, LiquidSourceActor.COLORS[block.colorActiveIndex])
                showPipeAnimationBottom(x, y, LiquidSourceActor.COLORS[block.colorActiveIndex])
                return AnimationMetadata(1400L, LiquidSourceActor.COLORS[block.colorActiveIndex])
            }
            is MixerActor -> {
                val meta1 = animateLiquid(x - 1, y)
                val meta2 = animateLiquid(x + 1, y)
                val c1 = meta1.color
                val c2 = meta2.color
                val c = Color((c1.r + c2.r) / 2f, (c1.g + c2.g) /2f, (c1.b + c2.b) / 2f, 1f)
                KtxAsync.launch {
                    delay(max(meta1.delay, meta2.delay))
                    showPipeAnimationBottom(x, y, c)
                }
                return AnimationMetadata(max(meta1.delay, meta2.delay) + 1400, c)
            }
            is CornerActor -> {
                val metadata = animateLiquid(x, y + 1)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationHorizontal(x, y, metadata.color, block.bottomLeft == false)
                }
                return AnimationMetadata(metadata.delay + 1400, metadata.color)
            }
            is InvertorActor -> {
                val metadata = animateLiquid(x, y + 1)
                val c = metadata.color
                val invertedColor = Color(1f - c.r, 1f - c.g, 1f - c.b, 1f)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, invertedColor)
                }
                return AnimationMetadata(metadata.delay + 1400, invertedColor)
            }
            is ModificatorActor -> {
                val metadata = animateLiquid(x, y + 1)
                val c = metadata.color
                val c2 = operateModificator(block, c)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, c2)
                }
                return AnimationMetadata(metadata.delay + 1400, c2)
            }
        }
        return AnimationMetadata(0L, Color.CYAN)
    }

    //We assume everything is validated and working
    fun calculateColor(x: Int, y: Int): Color {
        if (x == 5 && y == -1) return calculateColor(x, y + 1)
        val block = blocksMap[Pair(x,y)]!!
        when (block) {
            is LiquidSourceActor -> return LiquidSourceActor.COLORS[block.colorActiveIndex]
            is MixerActor -> {
                val c1 = calculateColor(x - 1, y)
                val c2 = calculateColor(x + 1, y)
                val c = Color((c1.r + c2.r) / 2f, (c1.g + c2.g) /2f, (c1.b + c2.b) / 2f, 1f)
                return c
            }
            is CornerActor -> return calculateColor(x, y + 1)
            is InvertorActor -> {
                val c = calculateColor(x, y + 1)
                return Color(1f - c.r, 1f - c.g, 1f - c.b, 1f)
            }
            is ModificatorActor -> {
                val c = calculateColor(x, y + 1)
                return operateModificator(block, c)
            }
        }
    }

    private fun operateModificator(
        block: ModificatorActor,
        c: Color
    ) = if (block.isAdder) {
        Color(
            min(1f, ModificatorActor.COLORS[block.colorActiveIndex].r + c.r),
            min(1f, ModificatorActor.COLORS[block.colorActiveIndex].g + c.g),
            min(1f, ModificatorActor.COLORS[block.colorActiveIndex].b + c.b),
            1f
        )
    } else {
        Color(
            max(0f, -ModificatorActor.COLORS[block.colorActiveIndex].r + c.r),
            max(0f, -ModificatorActor.COLORS[block.colorActiveIndex].g + c.g),
            max(0f, -ModificatorActor.COLORS[block.colorActiveIndex].b + c.b),
            1f
        )
    }

    private fun checkBlockExists(x: Int, y: Int): Boolean {
        return blocksMap.containsKey(Pair(x,y)) || (x == 5 && y == -1)
    }

    companion object Markup {
        const val OBJECT_SIZE = 144f
        const val BASE_WIDTH = OBJECT_SIZE * 3f
        const val BASE_HEIGHT = OBJECT_SIZE * 3f
        const val BASELINE = BASE_HEIGHT
    }
}