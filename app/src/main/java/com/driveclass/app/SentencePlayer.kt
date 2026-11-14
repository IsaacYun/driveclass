package com.driveclass.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

/**
 * 화면에 붙는 문장 플레이어. 화면이 가려지면(ON_STOP: 홈 버튼·다른 앱) 멈추고, 화면을 닫으면 해제한다.
 */
@Composable
fun rememberSentencePlayer(): SentencePlayer {
    val context = LocalContext.current
    val player = remember { SentencePlayer(context) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) player.stop() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    return player
}

/**
 * 오늘의 정리에서 표현 한 문장을 원문 → 번역문 순서로 들려준다.
 * assets/sentences/<블록id>-<순번>-en.mp3, -ko.mp3 가 있으면 그 파일을, 없으면 기기 음성 합성(TTS)으로 읽는다.
 * 한 번에 한 문장만 재생하고, 같은 문장을 다시 누르면 멈춘다.
 */
class SentencePlayer(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())

    /** 지금 재생 중인 표현의 키("블록id#순번"). 없으면 null. */
    var playingKey: String? by mutableStateOf(null)
        private set

    private var media: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: (() -> Unit)? = null
    /** 재생 차례를 구분한다. 멈추거나 다른 문장을 누르면 올라가 지난 콜백을 무시한다. */
    private var seq = 0

    fun toggle(item: RecallStore.Item) {
        if (playingKey == item.key) stop() else play(item)
    }

    fun play(item: RecallStore.Item) {
        stop()
        val my = ++seq
        playingKey = item.key
        val files = listOf("en", "ko").map { "sentences/${item.blockId}-${item.index}-$it.mp3" }
        if (files.all(::hasAsset)) playFiles(files, my)
        else speak(listOf(item.en to Locale.US, item.ko to Locale.KOREAN), my)
    }

    fun stop() {
        seq++
        media?.release()
        media = null
        tts?.stop()
        pendingSpeak = null
        playingKey = null
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }

    private fun finish(my: Int) {
        if (my == seq) {
            media?.release()
            media = null
            playingKey = null
        }
    }

    private fun hasAsset(path: String) = runCatching { app.assets.openFd(path).close() }.isSuccess

    private fun playFiles(files: List<String>, my: Int) {
        if (my != seq) return
        val path = files.firstOrNull() ?: return finish(my)
        val fd = runCatching { app.assets.openFd(path) }.getOrNull() ?: return finish(my)
        media?.release()
        val player = MediaPlayer()
        media = player
        // 파일이 깨졌거나 재생할 수 없으면 앱을 죽이지 않고 이 문장 재생만 끝낸다.
        val ok = runCatching {
            fd.use {
                player.setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                )
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            player.setOnCompletionListener { main.postDelayed({ playFiles(files.drop(1), my) }, GAP_MS) }
            player.setOnErrorListener { _, _, _ -> finish(my); true }
            player.prepare()
            player.start()
        }.isSuccess
        if (!ok) finish(my)
    }

    /** 언어가 다른 두 문장을 이어 읽는다. TTS 언어는 엔진 전체 설정이라 앞 문장이 끝난 뒤 바꾼다. */
    private fun speak(parts: List<Pair<String, Locale>>, my: Int) {
        if (my != seq) return
        val (text, locale) = parts.firstOrNull() ?: return finish(my)
        val engine = tts ?: TextToSpeech(app) { status ->
            main.post {
                ttsReady = status == TextToSpeech.SUCCESS
                if (ttsReady) {
                    pendingSpeak?.invoke()
                } else {
                    // 초기화에 실패한 엔진은 버린다. 다음에 누르면 새로 만든다.
                    tts?.shutdown()
                    tts = null
                    finish(seq)
                }
                pendingSpeak = null
            }
        }.also { t ->
            tts = t
            t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String) {}
                override fun onDone(id: String) = main.post { onSpoken(id) }.let {}
                @Deprecated("Deprecated in Java")
                override fun onError(id: String) = main.post { finish(id.substringBefore(':').toIntOrNull() ?: -1) }.let {}
            })
        }
        val go = go@{
            // 그 언어의 음성이 없거나 발화를 받지 않으면 재생 표시를 끈다.
            val lang = engine.setLanguage(locale)
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                finish(my)
                return@go
            }
            engine.setSpeechRate(if (locale == Locale.US) 0.9f else 1f)
            nextParts = parts.drop(1)
            if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "$my:${parts.size}") == TextToSpeech.ERROR) finish(my)
        }
        if (ttsReady) go() else pendingSpeak = { if (my == seq) go() }
    }

    private var nextParts: List<Pair<String, Locale>> = emptyList()

    private fun onSpoken(id: String) {
        val my = id.substringBefore(':').toIntOrNull() ?: return
        if (my != seq) return
        if (nextParts.isEmpty()) finish(my)
        else main.postDelayed({ speak(nextParts, my) }, GAP_MS)
    }

    private companion object {
        const val GAP_MS = 500L
    }
}
