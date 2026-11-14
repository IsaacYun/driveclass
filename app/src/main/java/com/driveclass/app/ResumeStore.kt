package com.driveclass.app

import android.content.Context

/**
 * 코스를 다 듣지 못하고 주행이 끝났을 때(목적지 전 정차, 일찍 도착) 들은 자리를 기기 안에 저장한다.
 * 다음에 출발하면 이 자리부터 이어 듣기를 묻는다.
 */
object ResumeStore {
    private const val PREFS = "resume"

    fun save(context: Context, p: CourseProgress) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("ids", p.ids.joinToString(","))
            .putInt("done", p.doneCount)
            .putInt("sec", p.partialSec)
            .apply()
    }

    fun load(context: Context): CourseProgress? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getString("ids", null)?.split(",")?.filter { it.isNotBlank() }.orEmpty()
        if (ids.isEmpty()) return null
        val p = CourseProgress(ids, prefs.getInt("done", 0), prefs.getInt("sec", 0))
        return if (p.doneCount < ids.size) p else null
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/**
 * 한 코스를 어디까지 들었는지. 앞에서 [doneCount]개 블록을 다 들었고,
 * 그다음 블록은 [partialSec]초까지 들었다.
 */
data class CourseProgress(val ids: List<String>, val doneCount: Int, val partialSec: Int) {
    val blocks get() = ids.mapNotNull(SampleCatalog::block)
    val finished get() = doneCount >= ids.size
    val heardSec get() = blocks.take(doneCount).sumOf { it.durationSec } +
        (blocks.getOrNull(doneCount)?.let { partialSec.coerceAtMost(it.durationSec) } ?: 0)
    val leftBlocks get() = blocks.drop(doneCount)
    val leftSec get() = leftBlocks.sumOf { it.durationSec } - partialSec
}
