package com.driveclass.app.playback

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.driveclass.app.SampleCatalog
import com.driveclass.engine.Block
import com.driveclass.engine.Catalog
import com.driveclass.engine.Plan
import com.driveclass.engine.Planner

/**
 * PlaybackService의 MediaController 래퍼. 화면이 읽는 상태(큐, 재생 여부)를 들고 있고,
 * 큐의 사실은 서비스 쪽 플레이어에 둔다(화면이 다시 만들어져도 큐에서 복원).
 * 연결만으로는 재생하지 않는다.
 */
class PlaybackClient(private val context: Context) {
    private var controller: MediaController? = null
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null

    /** 지금 서비스 큐에 든 블록 id(재생 전이거나 끝났으면 빈 목록). */
    var queueIds: List<String> by mutableStateOf(emptyList())
        private set
    var currentIndex: Int by mutableStateOf(0)
        private set
    var playing: Boolean by mutableStateOf(false)
        private set

    /** 코스를 끝까지 들었을 때 그 코스의 블록 id. 새 코스를 시작하거나 [clearFinished]하면 빈다. */
    var finishedIds: List<String> by mutableStateOf(emptyList())
        private set

    /** 마지막으로 큐를 짠 시점의 남은 시간(초)과 그때의 시각. */
    private var baseline: Pair<Int, Long>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()
    }

    fun connect() {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val f = MediaController.Builder(context, token).buildAsync()
        future = f
        f.addListener({
            // sync()가 controller를 읽으므로 먼저 대입한다(이미 재생 중인 큐를 연결 직후에 화면에 반영).
            val c = runCatching { f.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            sync()
        }, ContextCompat.getMainExecutor(context))
    }

    fun release() {
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        future = null
    }

    private fun sync() {
        val c = controller ?: return
        val active = c.mediaItemCount > 0 && c.playbackState != Player.STATE_ENDED
        queueIds = if (active) (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId } else emptyList()
        currentIndex = c.currentMediaItemIndex.coerceAtLeast(0)
        playing = active && c.playWhenReady
        if (c.playbackState == Player.STATE_ENDED && c.mediaItemCount > 0 && finishedIds.isEmpty()) {
            finishedIds = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).mediaId }
        }
    }

    private fun item(b: Block) = MediaItem.Builder()
        .setMediaId(b.id)
        .setUri(SampleCatalog.assetUri(b.id))
        .setMediaMetadata(MediaMetadata.Builder().setTitle(b.title).build())
        .build()

    /** 오디오 파일이 있는 블록만 남긴다. */
    private fun playable(blocks: List<Block>, available: Set<String>) = blocks.filter { it.id in available }

    /** 재생 중이면 일시정지, 아니면 재생(큐가 없거나 끝났으면 plan으로 새로 만든다). */
    fun togglePlay(plan: Plan, remainingSec: Int, available: Set<String>) {
        val c = controller ?: return
        when {
            playing -> c.pause()
            queueIds.isNotEmpty() -> c.play()
            else -> {
                val items = playable(plan.blocks, available).map(::item)
                if (items.isEmpty()) return
                finishedIds = emptyList()
                c.setMediaItems(items, /* resetPosition = */ true)
                c.prepare()
                c.play()
                baseline = remainingSec to SystemClock.elapsedRealtime()
            }
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun play() {
        controller?.play()
    }

    /** 큐를 비운다. 들은 자리는 부르는 쪽이 따로 저장한다. */
    fun stop() {
        val c = controller ?: return
        c.stop()
        c.clearMediaItems()
        finishedIds = emptyList()
    }

    fun clearFinished() {
        finishedIds = emptyList()
    }

    /**
     * [blocks]로 새 큐를 만들어 첫 블록의 [startSec]초부터 재생한다(이어 듣기·복습 듣기).
     * 오디오 파일이 없는 블록은 건너뛴다.
     */
    fun start(blocks: List<Block>, startSec: Int, remainingSec: Int, available: Set<String>) {
        val c = controller ?: return
        val items = playable(blocks, available).map(::item)
        if (items.isEmpty()) return
        finishedIds = emptyList()
        c.setMediaItems(items, 0, startSec.coerceAtLeast(0) * 1000L)
        c.prepare()
        c.play()
        baseline = remainingSec to SystemClock.elapsedRealtime()
    }

    /** 지금 블록에서 들은 시간(초). 화면의 코스 진행 막대가 1초마다 읽는다. */
    fun positionSec(): Int = ((controller?.currentPosition ?: 0L) / 1000).toInt().coerceAtLeast(0)

    fun next() {
        val c = controller ?: return
        if (queueIds.isNotEmpty() && c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    /** 이전 블록으로. 첫 블록이면 그 블록 처음으로. */
    fun previous() {
        val c = controller ?: return
        if (queueIds.isEmpty()) return
        if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem() else c.seekTo(0)
    }

    /** [index]번째 블록의 [sec]초로 옮긴다. */
    fun seekTo(index: Int, sec: Int) {
        val c = controller ?: return
        if (index !in queueIds.indices) return
        c.seekTo(index, sec.coerceAtLeast(0) * 1000L)
    }

    /**
     * 지금 위치에서 [deltaSec]초 앞(+)이나 뒤(−)로. 블록 경계를 넘으면 앞뒤 블록으로 이어 간다.
     * 코스 처음보다 앞이면 처음, 끝보다 뒤면 마지막 블록의 끝 직전으로.
     */
    fun seekBy(deltaSec: Int) {
        val c = controller ?: return
        val blocks = queueIds.map { SampleCatalog.block(it) ?: return }
        if (blocks.isEmpty()) return
        val idx = c.currentMediaItemIndex.coerceIn(0, blocks.lastIndex)
        val total = blocks.sumOf { it.durationSec }
        val now = blocks.take(idx).sumOf { it.durationSec } + positionSec()
        var target = (now + deltaSec).coerceIn(0, (total - 1).coerceAtLeast(0))
        for ((i, b) in blocks.withIndex()) {
            if (target < b.durationSec || i == blocks.lastIndex) {
                c.seekTo(i, target * 1000L)
                return
            }
            target -= b.durationSec
        }
    }

    /**
     * 남은 시간이 바뀌었을 때 호출. 큐가 있고, ETA가 3분 이상 달라졌거나(Planner.shouldReplan)
     * 남은 큐가 도착 전에 끝나지 않을 때(Planner.overBudget, 예: 일시정지한 사이 ETA만 줄었을 때)
     * 지금 항목 뒤의 큐를 교체한다. 지금 항목은 건드리지 않는다.
     */
    fun onRemainingChanged(remainingSec: Int, catalog: Catalog, available: Set<String>) {
        val c = controller ?: return
        if (queueIds.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        val base = baseline ?: run { baseline = remainingSec to now; return }
        val expected = base.first - ((now - base.second) / 1000).toInt()

        val idx = c.currentMediaItemIndex
        val blocks = queueIds.map { SampleCatalog.block(it) ?: return }
        if (idx !in blocks.indices) return
        val durSec = if (c.duration != C.TIME_UNSET) (c.duration / 1000).toInt() else blocks[idx].durationSec
        val currentLeft = (durSec - (c.currentPosition / 1000).toInt()).coerceAtLeast(0)
        val queueLeft = currentLeft + blocks.drop(idx + 1).sumOf { it.durationSec }
        if (!Planner.shouldReplan(expected, remainingSec) && !Planner.overBudget(queueLeft, remainingSec)) return
        val replanned = Planner.replan(
            Plan(Planner.formatFor(expected), blocks), idx, currentLeft, remainingSec, catalog,
        )
        val tail = playable(replanned.blocks.drop(idx + 1), available).map(::item)
        c.replaceMediaItems(idx + 1, c.mediaItemCount, tail)
        baseline = remainingSec to now
    }
}
