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
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.h4games.slime.ColbaActor
import com.h4games.slime.GameContext
import com.h4games.slime.SlimeActor
import com.h4games.slime.SlimeMachineGame
import com.h4games.slime.field.blocks.*
import com.h4games.slime.field.panel.BlockType
import com.h4games.slime.field.panel.PanelActor
import com.h4games.slime.level.LevelChooseScreen
import com.h4games.slime.level.LevelConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.actors.repeat
import ktx.async.KtxAsync
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class AreaAvailabilityResult(
    val x: Int,
    val y: Int,
    val available: Boolean
)

private val LIQUID_STEP_LENGTH_MILLIS = 700L

enum class MachineState {
    NOT_WORKING,
    WORKING,
    SLIME_SHOWING
}

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

    var base_x = 0f

    var slime: SlimeActor? = null

    val warnings = Group()

    val blocksMap = mutableMapOf<Pair<Int, Int>, BlockActor>()
    val lockedCoords = mutableSetOf<Pair<Int, Int>>()

    val tutorials = Group()

    var machineState: MachineState = MachineState.NOT_WORKING

    init {
        addActor(animations)
        addActor(blocks)
        addActor(warnings)
        addActor(tutorials)

        val baseBlock = SlimeFactoryActor(context).apply {
            x = (Gdx.graphics.width - 133f - BASE_WIDTH) / 2f
            y = 0f
        }
        blocks.addActor(baseBlock)
        base_x = baseBlock.x + 430f / 2f - 5.5f * OBJECT_SIZE

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
            if (machineState == MachineState.NOT_WORKING) {
                KtxAsync.launch {
                    machineState = MachineState.WORKING
                    context.sound("lever").play()
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
            } else if (machineState == MachineState.SLIME_SHOWING) {
                KtxAsync.launch {
                    machineState = MachineState.WORKING
                    door.closeDoor()
                    delay(300L)
                    slime?.let { it.remove() }
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_empty")))
                    rechargeLiquidSources()
                    rotateStickBack()
                    animations.clearChildren()
                    machineState = MachineState.NOT_WORKING
                }
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

        level.tutorials?.forEach {
            val text = Label(it.text, context.tutorialStyle).apply {
                x = it.x.toFloat()
                y = it.y.toFloat()
            }
            tutorials.addActor(text)
        }
    }

    private fun attemptLaunchMachine(baseBlock: SlimeFactoryActor) {
        val warnings = validateBlocks()
        if (warnings.isNotEmpty()) {
            context.sound("error").play()
            shake(launcher)
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
            rotateStickBack()
            machineState = MachineState.NOT_WORKING
        } else {
            val meta = animateLiquid(5, 0, 0)
            launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_working")))
            KtxAsync.launch {
                delay(meta.delay)
                delay(250L)
                launcher.launcherImage.setDrawable(TextureRegionDrawable(context.texture("launcher_off")))

                door.openDoor()
                val colorMatch = abs(meta.color.r - this@FieldActor.targetColor.r) < 0.02f &&
                        abs(meta.color.g - this@FieldActor.targetColor.g) < 0.02f &&
                        abs(meta.color.b - this@FieldActor.targetColor.b) < 0.02f
                //Add slime
                slime?.let { it.remove() }
                slime = SlimeActor(context, colorMatch, meta.color).apply {
                    x = baseBlock.x + 140f
                    y = baseBlock.y + 10f
                }
                slimes.addActor(slime)

                delay(500L)
                if (!colorMatch) {
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_red")))
                    context.sound("sigh").play()
                    shake(colba)
                    machineState = MachineState.SLIME_SHOWING
                } else {
                    colba.fg.setDrawable(TextureRegionDrawable(context.texture("colba_green")))
                    context.sound("fanfare").play()
                    game.markLevelComplete(levelIndex)
                    delay(5000L)
                    game.screen = LevelChooseScreen(context, game)
                    rotateStickBack()
                }
            }
        }
    }

    private fun rotateStickBack() {
        launcher.launcherStick.addAction(RotateByAction().apply {
            amount = 90f
            duration = 0.25f
        })
    }

    private fun rechargeLiquidSources() {
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
        block.x = base_x + x * OBJECT_SIZE
        block.y = BASELINE + y * OBJECT_SIZE
    }

    override fun act(delta: Float) {
        super.act(delta)
        val x = Gdx.input.x
        val y = Gdx.input.y
        if (x < Gdx.graphics.width - PanelActor.PANEL_SIZE) {
            val inputCoords = screenToLocalCoordinates(Vector2(x.toFloat(), y.toFloat()))
            val xx = ((inputCoords.x - base_x) / OBJECT_SIZE).toInt()
            val yy = ((inputCoords.y - BASELINE) / OBJECT_SIZE).toInt()
            if (!blocksMap.containsKey(Pair(xx, yy)) && inputCoords.y >= BASELINE) {
                cursor?.let { it.remove() }
                cursor = Image(context.texture("cursor_ok")).apply {
                    this.x = base_x + xx * OBJECT_SIZE
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
            val xx = ((inputCoords.x - base_x) / OBJECT_SIZE).toInt()
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
            BlockType.HOR_PIPE -> HorizontalPipeActor(context, OBJECT_SIZE)
            BlockType.MIXER_MAX -> MixerCrossActor(context, OBJECT_SIZE)
        }.let {
            blocks.addActor(it)
            placeBlock(it, x, y)
            blocksMap[Pair(x, y)] = it
        }
    }

    fun processRemoveBlock(x: Int, y: Int) {

        if (x < Gdx.graphics.width - 144f) {
            animations.clearChildren()
            val inputCoords = screenToLocalCoordinates(Vector2(x.toFloat(), y.toFloat()))
            val xx = ((inputCoords.x - base_x) / OBJECT_SIZE).toInt()
            val yy = ((inputCoords.y - BASELINE) / OBJECT_SIZE).toInt()
            if (lockedCoords.contains(Pair(xx, yy))) return
            if (xx >= 0 && yy >= 0) {
                val coords = Pair(xx, yy)
                blocksMap[coords]?.let { actor ->
                    actor.remove()
                }
                blocksMap.remove(coords)
                context.sound("destroy").play()
            }
        }
    }

    fun findAverageCoord(x1: Int, y1: Int, x2: Int, y2: Int): Vector2 {
        return Vector2(base_x + OBJECT_SIZE * (x1.toFloat() + x2.toFloat()) / 2f + OBJECT_SIZE / 2f, BASELINE + OBJECT_SIZE * (y1.toFloat() + y2.toFloat()) / 2f + OBJECT_SIZE / 2f)
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
                    if (!checkBlockNonCross(xy.first - 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first - 1, xy.second))
                    if (!checkBlockNonCross(xy.first + 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first + 1, xy.second))
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
                is HorizontalPipeActor -> {
                    if (!checkBlockExists(xy.first - 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first - 1, xy.second))
                    if (!checkBlockExists(xy.first + 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first + 1, xy.second))
                }
                is MixerCrossActor -> {
                    if (!checkBlockNonCross(xy.first - 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first - 1, xy.second))
                    if (!checkBlockNonCross(xy.first + 1, xy.second)) result.add(findAverageCoord(xy.first, xy.second, xy.first + 1, xy.second))
                    if (!checkBlockExists(xy.first, xy.second - 1)) result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second - 1))
                    if (!checkBlockExists(xy.first, xy.second + 1)) result.add(findAverageCoord(xy.first, xy.second, xy.first, xy.second + 1))
                }
            }
        }
        return result
    }

    private fun showLiquidAnimation(x: Int, y: Int, color: Color) {
//        context.sound("water_flow").play()
//        val animation = AnimatedActor(context, "liquid_source_anim", 1/6f).apply {
//            this.x = base_x + x * OBJECT_SIZE
//            this.y = BASELINE + y * OBJECT_SIZE
//            this.width = OBJECT_SIZE
//            this.height = OBJECT_SIZE
//            this.color = color
//        }
//        animations.addActor(animation)
    }

    private fun showPipeAnimationBottom(x: Int, y: Int, color: Color) {
        context.sound("water_flow").play()
        val animation = AnimatedActor(context, "liquid_animation_vertical", LIQUID_STEP_LENGTH_MILLIS / 15000f).apply {
            this.x = base_x + x * OBJECT_SIZE
            this.y = BASELINE + (y - 0.25f) * OBJECT_SIZE
            this.width = OBJECT_SIZE
            this.height = OBJECT_SIZE * 0.5f
            this.color = color
        }
        animations.addActor(animation)
    }

    private fun showPipeAnimationHorizontal(x: Int, y: Int, color: Color, inverted: Boolean) {
        val animation = AnimatedActor(context, if (inverted) "liquid_animation_right" else "liquid_animation_left", LIQUID_STEP_LENGTH_MILLIS / 15000f).apply {
            this.x = base_x + x * OBJECT_SIZE + if (inverted) OBJECT_SIZE * 0.75f else - OBJECT_SIZE * 0.25f
            this.y = BASELINE + y * OBJECT_SIZE
            this.width = OBJECT_SIZE / 2f
            this.height = OBJECT_SIZE
            this.color = color
            this.inverted = false
        }
        animations.addActor(animation)
    }

    data class AnimationMetadata(
        val delay: Long,
        val color: Color
    )

    fun animateLiquid(x: Int, y: Int, dx: Int): AnimationMetadata {
        val block: BlockActor = blocksMap[Pair(x,y)]!!
        return when (block) {
            is LiquidSourceActor -> {
//                block.hideLiquid()
                showLiquidAnimation(x, y, block.colors[block.colorActiveIndex])
                showPipeAnimationBottom(x, y, block.colors[block.colorActiveIndex])
                AnimationMetadata(LIQUID_STEP_LENGTH_MILLIS, block.colors[block.colorActiveIndex])
            }
            is MixerActor -> {
                val meta1 = animateLiquid(x - 1, y, -1)
                val meta2 = animateLiquid(x + 1, y, 1)
                val c1 = meta1.color
                val c2 = meta2.color
                val c = Color((c1.r + c2.r) / 2f, (c1.g + c2.g) /2f, (c1.b + c2.b) / 2f, 1f)
                KtxAsync.launch {
                    delay(max(meta1.delay, meta2.delay))
                    showPipeAnimationBottom(x, y, c)
                }
                AnimationMetadata(max(meta1.delay, meta2.delay) + LIQUID_STEP_LENGTH_MILLIS, c)
            }
            is MixerCrossActor -> {
                val meta1 = animateLiquid(x - 1, y, -1)
                val meta2 = animateLiquid(x + 1, y, 1)
                val meta3 = animateLiquid(x, y + 1, 0)
                val c1 = meta1.color
                val c2 = meta2.color
                val c3 = meta3.color
                val c = Color((c1.r + c2.r + c3.r) / 3f, (c1.g + c2.g + c3.g) /3f, (c1.b + c2.b + c3.b) / 3f, 1f)
                KtxAsync.launch {
                    delay(max(meta1.delay, meta2.delay))
                    showPipeAnimationBottom(x, y, c)
                }
                AnimationMetadata(max(meta1.delay, meta2.delay) + LIQUID_STEP_LENGTH_MILLIS, c)
            }
            is CornerActor -> {
                val metadata = animateLiquid(x, y + 1, 0)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationHorizontal(x, y, metadata.color, block.bottomLeft)
                }
                AnimationMetadata(metadata.delay + LIQUID_STEP_LENGTH_MILLIS, metadata.color)
            }
            is InvertorActor -> {
                val metadata = animateLiquid(x, y + 1, 0)
                val c = metadata.color
                val invertedColor = Color(1f - c.r, 1f - c.g, 1f - c.b, 1f)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, invertedColor)
                }
                AnimationMetadata(metadata.delay + LIQUID_STEP_LENGTH_MILLIS, invertedColor)
            }
            is ModificatorActor -> {
                val metadata = animateLiquid(x, y + 1, 0)
                val c = metadata.color
                val c2 = operateModificator(block, c)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationBottom(x, y, c2)
                }
                AnimationMetadata(metadata.delay + LIQUID_STEP_LENGTH_MILLIS, c2)
            }
            is HorizontalPipeActor -> {
                val metadata = animateLiquid(x + dx, y, dx)
                KtxAsync.launch {
                    delay(metadata.delay)
                    showPipeAnimationHorizontal(x, y, metadata.color, dx < 0)
                }
                AnimationMetadata(metadata.delay + LIQUID_STEP_LENGTH_MILLIS, metadata.color)
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

    private fun checkBlockNonCross(x: Int, y: Int): Boolean {
        return (blocksMap.containsKey(Pair(x,y)) && !(blocksMap[Pair(x,y)] is MixerCrossActor) && !(blocksMap[Pair(x,y)] is MixerActor)) || (x == 5 && y == -1)
    }

    companion object Markup {
        val OBJECT_SIZE = 180f
        const val BASE_WIDTH = 438f
        const val BASE_HEIGHT = 251f
        const val BASELINE = 242f
    }
}