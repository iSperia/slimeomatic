package com.h4games.slime.level

import com.h4games.slime.field.panel.BlockType

data class ColorConfig(
    val r: Float,
    val g: Float,
    val b: Float
)

data class LockedBlockConfig(
    val type: BlockType,
    val x: Int,
    val y: Int
)

data class LevelConfig(
    val targetColor: ColorConfig,
    val blocks: List<BlockType>,
    val liquids: List<ColorConfig>,
    val adders: List<ColorConfig>,
    val removers: List<ColorConfig>,
    val locked: List<LockedBlockConfig>?
)

data class LevelsConfig(
    val levels: List<LevelConfig>
)

data class GameProgress(
    val maxLevelComplete: Int
)