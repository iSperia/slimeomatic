package com.h4games.slime.level

import com.h4games.slime.field.panel.BlockType

data class LevelConfig(
    val r: Float,
    val g: Float,
    val b: Float,
    val blocks: List<BlockType>
)

data class LevelsConfig(
    val levels: List<LevelConfig>
)

data class GameProgress(
    val maxLevelComplete: Int
)