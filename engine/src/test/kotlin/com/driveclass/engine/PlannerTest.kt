package com.driveclass.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlannerTest {
    private val catalog = Catalog(
        intro = Block("intro", BlockKind.INTRO, "도입", 60),
        cores = (1..30).map { Block("core$it", BlockKind.CORE, "본문 $it", if (it % 2 == 0) 300 else 180) },
        reviews = (1..6).map { Block("review$it", BlockKind.REVIEW, "복습 $it", 180) },
        closing = Block("closing", BlockKind.CLOSING, "오늘의 한 문장", 60),
    )

    @Test
    fun `총 길이는 남은 시간에서 1분을 뺀 값을 넘지 않는다`() {
        for (minutes in listOf(3, 15, 30, 60, 120)) {
            val remaining = minutes * 60
            val plan = Planner.plan(remaining, catalog)
            assertTrue(plan.totalSec <= remaining - Planner.SAFETY_SEC, "$minutes 분: ${plan.totalSec}")
            assertEquals(BlockKind.CLOSING, plan.blocks.last().kind)
            assertEquals(plan.blocks.size, plan.blocks.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `길이에 따라 형식이 바뀐다`() {
        assertEquals(Format.SNACK, Planner.plan(12 * 60, catalog).format)
        assertEquals(Format.LESSON, Planner.plan(32 * 60, catalog).format)
        assertEquals(Format.DEEP_DIVE, Planner.plan(70 * 60, catalog).format)
    }

    @Test
    fun `남은 시간이 너무 짧으면 아무것도 틀지 않는다`() {
        assertTrue(Planner.plan(90, catalog).blocks.isEmpty())
        assertEquals(listOf("closing"), Planner.plan(125, catalog).blocks.map { it.id })
    }

    @Test
    fun `이미 들은 블록은 다시 넣지 않는다`() {
        val plan = Planner.plan(30 * 60, catalog, done = setOf("core1", "core2"))
        assertTrue(plan.blocks.none { it.id == "core1" || it.id == "core2" })
    }

    @Test
    fun `ETA가 15분 늘면 뒤에 블록이 붙고 듣던 블록은 그대로다`() {
        val plan = Planner.plan(32 * 60, catalog)
        val replanned = Planner.replan(plan, 2, 100, 40 * 60, catalog)
        assertEquals(plan.blocks.take(3), replanned.blocks.take(3))
        assertTrue(replanned.blocks.size > plan.blocks.size)
        val left = replanned.blocks.drop(3).sumOf { it.durationSec } + 100
        assertTrue(left <= 40 * 60 - Planner.SAFETY_SEC)
        assertEquals(BlockKind.CLOSING, replanned.blocks.last().kind)
    }

    @Test
    fun `ETA가 10분 줄면 뒤쪽 블록이 빠지고 마무리로 끝난다`() {
        val plan = Planner.plan(32 * 60, catalog)
        val replanned = Planner.replan(plan, 1, 120, 12 * 60, catalog)
        assertEquals(plan.blocks.take(2), replanned.blocks.take(2))
        assertTrue(replanned.blocks.size < plan.blocks.size)
        val left = replanned.blocks.drop(2).sumOf { it.durationSec } + 120
        assertTrue(left <= 12 * 60 - Planner.SAFETY_SEC)
        assertEquals(BlockKind.CLOSING, replanned.blocks.last().kind)
    }

    @Test
    fun `도착 2분 전에는 지금 블록 뒤에 마무리만 남는다`() {
        val plan = Planner.plan(32 * 60, catalog)
        val replanned = Planner.replan(plan, 3, 0, 120, catalog)
        assertEquals(plan.blocks.take(4) + catalog.closing, replanned.blocks)
    }

    @Test
    fun `3분 미만의 흔들림에는 재편성하지 않는다`() {
        assertTrue(!Planner.shouldReplan(1000, 1100))
        assertTrue(Planner.shouldReplan(1000, 1180))
        assertTrue(Planner.shouldReplan(1000, 800))
    }

    @Test
    fun `재편성할 때 기존 큐의 들은 블록도 빼고 중복을 넣지 않는다`() {
        val core1 = catalog.cores[0]
        val core2 = catalog.cores[1]
        val plan = Plan(Format.SNACK, listOf(core1, core2, core2, catalog.closing))
        val replanned = Planner.replan(plan, 0, 60, 30 * 60, catalog, done = setOf("core2"))
        val ids = replanned.blocks.map { it.id }
        assertTrue("core2" !in ids, ids.toString())
        assertEquals(ids.size, ids.toSet().size)
        assertEquals("core1", ids.first())
    }

    @Test
    fun `남은 큐가 도착 1분 전을 넘으면 예산 초과다`() {
        assertTrue(!Planner.overBudget(540, 600))
        assertTrue(Planner.overBudget(541, 600))
        assertTrue(Planner.overBudget(720, 540))
    }
}
