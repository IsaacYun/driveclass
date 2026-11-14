package com.driveclass.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 오늘의 정리에 모으는 '되새길 표현'. 블록을 듣다가 그 표현이 나오는 지점을 지나면 하나씩 모인다.
 * 키는 "블록id#순번"이고, 모인 순서대로 기기 안에만 저장한다.
 *
 * 표현이 나오는 지점(cue)은 블록 길이를 표현 수+1로 나눈 근사값이다.
 * 실제 강의 데이터에서는 대본의 타임스탬프로 바꾼다.
 */
object RecallStore {
    private const val PREFS = "recall"
    private const val KEY = "keys"

    data class Item(val blockId: String, val index: Int, val en: String, val ko: String) {
        val key get() = "$blockId#$index"
    }

    private val _keys = MutableStateFlow<List<String>?>(null)

    /** 모인 순서대로의 키. 처음 읽을 때 저장소에서 불러온다. */
    fun keys(context: Context): StateFlow<List<String>?> {
        if (_keys.value == null) _keys.value = load(context)
        return _keys
    }

    fun items(keys: List<String>): List<Item> = keys.mapNotNull(::item)

    fun item(key: String): Item? {
        val id = key.substringBefore('#')
        val i = key.substringAfter('#').toIntOrNull() ?: return null
        val (en, ko) = SampleCatalog.keySentences[id]?.getOrNull(i) ?: return null
        return Item(id, i, en, ko)
    }

    /** 블록의 i번째 표현이 나오는 지점(초). */
    fun cueSec(durationSec: Int, i: Int, count: Int) = durationSec * (i + 1) / (count + 1)

    /**
     * [blockId] 블록을 [fromSec] 다음부터 [positionSec]까지 들었을 때 그 사이의 표현을 모은다.
     * 끌거나 건너뛴 구간의 표현은 모으지 않도록 화면은 들은 구간만 넘긴다.
     */
    @Synchronized
    fun reach(context: Context, blockId: String, positionSec: Int, fromSec: Int = -1) {
        val block = SampleCatalog.block(blockId) ?: return
        val sentences = SampleCatalog.keySentences[blockId] ?: return
        val now = _keys.value ?: load(context)
        val fresh = sentences.indices
            .filter { cueSec(block.durationSec, it, sentences.size).let { cue -> cue > fromSec && cue <= positionSec } }
            .map { "$blockId#$it" }
            .filter { it !in now }
        if (fresh.isEmpty()) {
            if (_keys.value == null) _keys.value = now
            return
        }
        val next = now + fresh
        _keys.value = next
        save(context, next)
    }

    @Synchronized
    fun clear(context: Context) {
        _keys.value = emptyList()
        save(context, emptyList())
    }

    private fun load(context: Context): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
            .split(',').filter { it.isNotBlank() }

    private fun save(context: Context, keys: List<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, keys.joinToString(",")).apply()
    }
}
