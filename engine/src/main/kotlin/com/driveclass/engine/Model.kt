package com.driveclass.engine

enum class BlockKind { INTRO, CORE, REVIEW, CLOSING }

/** 1·3·5분 길이로 만든 오디오 학습 단위. */
data class Block(
    val id: String,
    val kind: BlockKind,
    val title: String,
    val durationSec: Int,
)

/** 예측한 주행 길이에 따라 정하는 수업 형식. */
enum class Format { SNACK, LESSON, DEEP_DIVE }

data class Catalog(
    val intro: Block?,
    val cores: List<Block>,
    val reviews: List<Block>,
    val closing: Block,
)

data class Plan(val format: Format, val blocks: List<Block>) {
    val totalSec: Int get() = blocks.sumOf { it.durationSec }
}
