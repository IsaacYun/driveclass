package com.driveclass.app

import android.content.Context

/** 들은 블록 id를 기기 안에만 저장한다. 정차 화면의 정리 카드가 읽는다. */
object HeardStore {
    private const val PREFS = "heard"
    private const val KEY = "ids"

    fun add(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY, emptySet()).orEmpty() + id
        prefs.edit().putStringSet(KEY, ids).apply()
    }

    fun all(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY, emptySet()).orEmpty()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
