package com.driveclass.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.driveclass.app.HeardStore
import com.driveclass.app.RecallStore
import com.driveclass.app.SampleCatalog
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 재생과 미디어 세션을 맡는 서비스. 화면이 꺼지거나 다른 앱이 앞에 와도 재생이 이어지고,
 * 스티어링 휠 키와 시스템 미디어 위젯이 이 세션으로 들어온다.
 * 자동 재생은 하지 않는다(사용자가 재생을 눌러야 시작).
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        // 파일이 없거나 깨진 블록은 건너뛰고 다음 블록으로 간다.
        player.addListener(object : Player.Listener {
            /**
             * 지금 블록에서 끊김 없이 들어 온 구간의 시작(초). 끌거나 건너뛰면 그 자리로 옮긴다.
             * 표현은 이 지점 다음부터 들은 것만 모은다(화면 쪽 기록 규칙과 같다). -1이면 블록 처음부터.
             */
            private var heardFromSec = -1

            private fun startSec(positionMs: Long) = if (positionMs <= 0) -1 else (positionMs / 1000).toInt()

            // 끝까지 들었거나 30초 넘게 듣고 넘긴 블록을 '들음'으로 기록한다.
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                val oldId = oldPosition.mediaItem?.mediaId
                if (oldId == null) {
                    heardFromSec = startSec(newPosition.positionMs)
                    return
                }
                val oldSec = (oldPosition.positionMs / 1000).toInt()
                if (oldId == newPosition.mediaItem?.mediaId) {
                    // 같은 블록 안에서 끌었다: 끌기 전까지 들은 표현만 모으고, 새 자리부터 다시 센다.
                    if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                        RecallStore.reach(this@PlaybackService, oldId, oldSec, heardFromSec)
                        heardFromSec = startSec(newPosition.positionMs)
                    }
                    return
                }
                // 화면이 꺼져 있어도 지나간 표현은 오늘의 정리에 모은다. 끝까지 들었으면 블록 끝까지.
                val auto = reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION
                RecallStore.reach(this@PlaybackService, oldId, if (auto) Int.MAX_VALUE else oldSec, heardFromSec)
                if (auto || oldPosition.positionMs >= 30_000) {
                    HeardStore.add(this@PlaybackService, oldId)
                }
                heardFromSec = startSec(newPosition.positionMs)
            }

            // 새 큐를 넣으면(새 코스·이어 듣기·복습) 시작 자리부터 센다.
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    heardFromSec = startSec(player.currentPosition)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.currentMediaItem?.mediaId?.let {
                        HeardStore.add(this@PlaybackService, it)
                        RecallStore.reach(this@PlaybackService, it, Int.MAX_VALUE, heardFromSec)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                }
            }
        })
        session = MediaLibrarySession.Builder(this, player, object : MediaLibrarySession.Callback {
            // 컨트롤러가 보낸 항목은 URI가 빠져서 올 수 있다. 블록 id로 에셋 URI를 다시 붙인다.
            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>,
            ): ListenableFuture<MutableList<MediaItem>> = Futures.immediateFuture(
                mediaItems.map { item ->
                    item.buildUpon().setUri(SampleCatalog.assetUri(item.mediaId)).build()
                }.toMutableList(),
            )
        }).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
