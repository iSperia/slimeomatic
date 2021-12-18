package com.h4games.slime.field

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RotateByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.ColbaActor
import com.h4games.slime.GameContext
import com.h4games.slime.SlimeMachineGame
import com.h4games.slime.field.blocks.*
import com.h4games.slime.field.panel.BlockType
import com.h4games.slime.level.LevelChooseScreen
import com.h4games.slime.level.LevelConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.actors.repeat
import ktx.actors.repeatForever
import ktx.async.KtxAsync
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class AreaAvailabilityResult(
    val x: Int,
    val y: Int,
    val available: Boolean
)

private val LIQUID_STEP_LENGTH_MILLIS = 700L

/**
 * Something that draws the field blocks and slime bases, and controls
 * It scrolls and scales
 */
class FieldActor(
    private val context: GameContext,
    private val targetColor: Color,
    private val game: SlimeMachineGame,
    private val levelIndex: Int,
    private val level: LevelConfig
) : Group() {

    val animations = Group()
    val blocks = Group()
    val slimes = Group()
    var cursor: Image? = null
    lateinit var door: DoorActor
    lateinit var launcher: LauncherActor
    lateinit var colba: ColbaActor

    var slime: Image? = null

    val warnings = Group()

    val blocksMap = mutableMapOf<Pair<Int, Int>, BlockActor>()
    val lockedCoords = mutableSetOf<Pair<Int, Int>>()

    init {
        OBJECT_SIZE = (Gdx.graphics.width - 133f) / 11f
        addActor(animations)
        addActor(blocks)
        addActor(warnings)

        val baseBlock = SlimeFactoryActor(context).apply {
            x = (Gdx.graphics.width - 133f - BASE_WIDTH) / 2f
            y = 0f
        }
        blocks.addActor(baseBlock)

        blocks.addActor(slimes)

        door = DoorActor(context).apply {
            x = baseBlock.x + 125f
            width = 177f
            height = 142f
        }
        blocks.addActor(door)

        launcher = LauncherActor(context).apply {
            x = baseBlock.x - 200f
            y = 6f
        }
        blocks.addActor(launcher)
        launcher.onClick {
            KtxAsync.launch {
                launcher.launcherStick.addAction(
                    SequenceAction(RotateByAction().apply {
                        amount = -90f
                        duration = 0.2f
                    },
                    RunnableAction().apply {
                        setRunnable {
                            attemptLaunchMachine(baseBlock)
                        }
                    })
                )
            }
        }

        colba = ColbaActor(context, targetColor).apply {
            x = baseBlock.x + 520f
            y = baseBlock.y - 5f
        }
        blocks.addActor(colba)

        level.locked?.forEach { lockConfig ->
            createBlock(lockConfig.x, lockConfig.y, lockConfig.type)
            lockedCoords.add(Pair(lockConfig.x, lockConfig.y))
        }
    }

    private fun attemptLaunchMachine(baseBlock: SlimeFactoryActor) {
        val warnings = validateBlocks()
        if (warnings.isNotEmpty()) {
            shake(launcher)
            launcher.launcherStick.addAction(RotateByAction().apply {
                amount = 90f
                duration = 0.25f
            })
            KtxAsync.launch {
                delay(250L)
                launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_error")))
                delay(1000L)
                launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_off")))
            }
            warnings.forEach { coords ->
                val warningImage = Image(context.texture("warning")).apply {
                    width = OBJECT_SIZE / 2f
                    height = OBJECT_SIZE / 2f
                    x = coords.x - width / 2f
                    y = coords.y - height / 2f
                }
                warningImage.addAction(
                    SequenceAction(
                        DelayAction(0.05f),
                        RunnableAction().apply { setRunnable { warningImage.isVisible = false } },
                        DelayAction(0.05f),
                        RunnableAction().apply { setRunnable { warningImage.isVisible = true } }
                    ).repeat(10)
                )
                this@FieldActor.warnings.addActor(warningImage)
                warningImage.addAction(DelayAction(1f).apply {
                    action = RunnableAction().apply {
                        setRunnable {
                            warningImage.clearActions()
                            warningImage.remove()
                        }
                    }
                })
            }
        } else {
            val targetColor = calculateColor(5, -1)

            val meta = animateLiquid(5, 0)
            launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_working")))
            KtxAsync.launch {
                delay(meta.delay)
                launcher.launcherStick.addAction(RotateByAction().apply {
                    amount = 90f
                    duration = 0.25f
                })
                delay(250L)
                launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_off")))

                door.openDoor()
                //Add slime
                slime?.let { it.remove() }
                slime = Image(context.texture("slime")).apply {
                    width = 144f
                    height = 144f
                    x = baseBlock.x + 145f
                    y = baseBlock.y + 10f
                    color = targetColor
                }
                slimes.addActor(slime)

                if (meta.color != this@FieldActor.targetColor) {
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_red")))
                    shake(colba)
                    delay(2000L)
                    door.closeDoor()
                    delay(300L)
                    slime?.let { it.remove() }
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_empty")))
                    rechargeLiquidSources()
                } else {
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_green")))
                    //TODO: end the level
                    delay(2000L)
                    game.markLevelComplete(levelIndex)
                    game.screen = LevelChooseScreen(context, game)
                }
            }
        }
    }

    private fun rechargeLiquidSources() {
        blocksMap.forEach { xy, block ->
            if (block is LiquidSourceActor) {
                block.showLiquid()
            }
        }
    }

    private fun shake(target: Actor) {
        val ox = target.x
        val oy = target.y
        target.addAction(
            SequenceAction(
                DelayAction(0.05f),
                RunnableAction().apply {
                    setRunnable {
                        target.x = ox - 2f + 4f * Random.nextFloat()
                        target.y = oy - 2f + 4f * Random.nextFloat()
                    }
                }
            ).repeat(20)
        )
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
            BlockType.LIQUID -> LiquidSourceActor(context, OBJECT_SIZE, level.liquids.map { Color(it.r, it.g, it.b, 1f) })
            BlockType.INVERTOR -> InvertorActor(context, OBJECT_SIZE)
            BlockType.ADDER -> ModificatorActor(context, OBJECT_SIZE, true, level.adders.map { Color(it.r, it.g, it.b, 1f) })
            BlockType.REMOVER -> ModificatorActor(context, OBJECT_SIZE, false, level.removers.map { Color(it.r, it.g, it.b, 1f) })
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
            if (lockedCoords.contains(Pair(xx, yy))) return
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
        val animation = AnimatedActor(context, "liquid_source_anim", 1/6f).apply {
            this.x = x * OBJECT_SIZE
            this.y = BASELINE + y * OBJECT_SIZE
            this.width = OBJECT_SIZE
            this.height = OBJECT_SIZE
            this.color = color
        }
        animations.addActor(animation)
    }

    private fun showPipeAnimationBottom(x: Int, y: Int, color: Color) {
        val animation = AnimatedActor(context, "vpipe", 1/6f).apply {
            this.x = x * OBJECT_SIZE + OBJECT_SIZE * 25f / 72f
            this.y = BASELINE + y * OBJECT_SIZE - OBJECT_SIZE * 21f / 72f
            this.width = OBJECT_SIZE * 23f / 72f
            this.height = OBJECT_SIZE * 44f / 72f
            this.color = color
        }
        animations.addActor(animation)
    }

    private fun showPipeAnimationHorizontal(x: Int, y: Int, color: Color, inverted: Boolean) {
        val animation = AnimatedActor(context, "hpipe", 1/6f).apply {
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
        val block: BlockActor = blocksMap[Pair(x,y)]!!
        return when (block) {
            is LiquidSourceActor -> {
                block.hideLiquid()
                showLiquidAnimation(x, y, block.colors[block.colorActiveIndex])
                showPipeAnimationBottom(x, y, block.colors[block.colorActiveIndex])
                AnimationMetadata(LIQUID_STEP_LENGTH_MILLIS, block.colors[block.colorActiveIndex])
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
                AnimationMetadata(max(meta1.delay, meta2.delay) + 1400, c)
            }
            is CornerActor -> {
                val metadata = animateLiquid(x, y + 1)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationHorizontal(x, y, metadata.color, block.bottomLeft == false)
                }
                AnimationMetadata(metadata.delay + 1400, metadata.color)
            }
            is InvertorActor -> {
                val metadata = animateLiquid(x, y + 1)
                val c = metadata.color
                val invertedColor = Color(1f - c.r, 1f - c.g, 1f - c.b, 1f)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, invertedColor)
                }
                AnimationMetadata(metadata.delay + 1400, invertedColor)
            }
            is ModificatorActor -> {
                val metadata = animateLiquid(x, y + 1)
                val c = metadata.color
                val c2 = operateModificator(block, c)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, c2)
                }
                AnimationMetadata(metadata.delay + 1400, c2)
            }
            else -> throw IllegalStateException("Unknown block")
        }
    }

    //We assume everything is validated and working
    fun calculateColor(x: Int, y: Int): Color {
        if (x == 5 && y == -1) return calculateColor(x, y + 1)
        val block = blocksMap[Pair(x,y)]!!
        return when (block) {
            is LiquidSourceActor -> block.colors[block.colorActiveIndex]
            is MixerActor -> {
                val c1 = calculateColor(x - 1, y)
                val c2 = calculateColor(x + 1, y)
                Color((c1.r + c2.r) / 2f, (c1.g + c2.g) /2f, (c1.b + c2.b) / 2f, 1f)
            }
            is CornerActor -> return calculateColor(x, y + 1)
            is InvertorActor -> {
                val c = calculateColor(x, y + 1)
                Color(1f - c.r, 1f - c.g, 1f - c.b, 1f)
            }
            is ModificatorActor -> {
                val c = calculateColor(x, y + 1)
                operateModificator(block, c)
            }
            else -> throw IllegalStateException("Unknown block")
        }
    }

    private fun operateModificator(
        block: ModificatorActor,
        c: Color
    ) = if (block.isAdder) {
        Color(
            min(1f, block.colors[block.colorActiveIndex].r + c.r),
            min(1f, block.colors[block.colorActiveIndex].g + c.g),
            min(1f, block.colors[block.colorActiveIndex].b + c.b),
            1f
        )
    } else {
        Color(
            max(0f, -block.colors[block.colorActiveIndex].r + c.r),
            max(0f, -block.colors[block.colorActiveIndex].g + c.g),
            max(0f, -block.colors[block.colorActiveIndex].b + c.b),
            1f
        )
    }

    private fun checkBlockExists(x: Int, y: Int): Boolean {
        return blocksMap.containsKey(Pair(x,y)) || (x == 5 && y == -1)
    }

    companion object Markup {
        var OBJECT_SIZE = 144f
        const val BASE_WIDTH = 438f
        const val BASE_HEIGHT = 251f
        const val BASELINE = 262f
    }
}