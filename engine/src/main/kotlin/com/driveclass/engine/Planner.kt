package com.driveclass.engine

import kotlin.math.abs

/**
 * 남은 주행 시간을 받아 블록 큐를 만드는 순수 함수 모음.
 * 차량 API에 의존하지 않는다. 불변식: 큐 총 길이 <= 남은 시간 - [SAFETY_SEC].
 */
object Planner {
    /** 도착 전에 비워 두는 여유. */
    const val SAFETY_SEC = 60

    /** ETA가 이만큼 이상 바뀌어야 재편성한다. */
    const val REPLAN_THRESHOLD_SEC = 180

    /** 남은 시간이 이 이하이면 지금 블록 뒤에 바로 마무리 블록을 붙인다. */
    const val CLOSING_WINDOW_SEC = 120

    fun formatFor(remainingSec: Int): Format = when {
        remainingSec <= 15 * 60 -> Format.SNACK
        remainingSec <= 45 * 60 -> Format.LESSON
        else -> Format.DEEP_DIVE
    }

    fun shouldReplan(expectedRemainingSec: Int, actualRemainingSec: Int): Boolean =
        abs(actualRemainingSec - expectedRemainingSec) >= REPLAN_THRESHOLD_SEC

    /**
     * 남은 큐(지금 블록의 남은 부분 + 뒤쪽 블록)가 도착 전에 끝나지 않으면 true.
     * 일시정지해 둔 사이 ETA만 줄어든 경우처럼 ETA 변화가 작아도 재편성이 필요할 때를 잡는다.
     */
    fun overBudget(queueLeftSec: Int, remainingSec: Int): Boolean =
        queueLeftSec > remainingSec - SAFETY_SEC

    /** 출발 시점의 편성. [done]은 이미 들은 블록 id. */
    fun plan(remainingSec: Int, catalog: Catalog, done: Set<String> = emptySet()): Plan {
        val format = formatFor(remainingSec)
        val closing = catalog.closing
        var budget = remainingSec - SAFETY_SEC - closing.durationSec
        if (budget < 0) {
            val onlyClosing = closing.durationSec <= remainingSec - SAFETY_SEC
            return Plan(format, if (onlyClosing) listOf(closing) else emptyList())
        }
        val candidates = buildList {
            if (format != Format.SNACK) catalog.intro?.let(::add)
            addAll(catalog.cores)
            addAll(catalog.reviews)
        }.filter { it.id !in done }
        val picked = mutableListOf<Block>()
        for (block in candidates) {
            if (block.durationSec <= budget) {
                picked += block
                budget -= block.durationSec
            }
        }
        return Plan(format, picked + closing)
    }

    /**
     * 주행 중 재편성. 지금 듣는 블록([currentIndex])과 그 앞은 건드리지 않고
     * 뒤쪽 큐만 다시 짠다. 블록을 중간에 자르지 않는다.
     */
    fun replan(
        plan: Plan,
        currentIndex: Int,
        currentBlockRemainingSec: Int,
        newRemainingSec: Int,
        catalog: Catalog,
        done: Set<String> = emptySet(),
    ): Plan {
        val format = formatFor(newRemainingSec)
        val head = plan.blocks.take(currentIndex + 1)
        val closing = catalog.closing
        if (head.lastOrNull()?.kind == BlockKind.CLOSING) return Plan(format, head)

        val afterCurrent = newRemainingSec - SAFETY_SEC - currentBlockRemainingSec
        if (afterCurrent < closing.durationSec) return Plan(format, head)
        if (newRemainingSec <= CLOSING_WINDOW_SEC) return Plan(format, head + closing)

        var budget = afterCurrent - closing.durationSec
        val used = head.map { it.id }.toSet() + done
        val oldTail = plan.blocks.drop(currentIndex + 1)
            .filter { it.kind != BlockKind.CLOSING && it.id !in used }
            .distinctBy { it.id }
        val oldTailIds = oldTail.map { it.id }.toSet()
        // 기존 큐를 먼저 살려서 ETA가 흔들려도 순서가 요동치지 않게 한다.
        val candidates = oldTail +
            (catalog.cores + catalog.reviews).filter { it.id !in used && it.id !in oldTailIds }
        val picked = mutableListOf<Block>()
        for (block in candidates) {
            if (block.durationSec <= budget) {
                picked += block
                budget -= block.durationSec
            }
        }
        return Plan(format, head + picked + closing)
    }
}
