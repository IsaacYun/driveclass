package com.driveclass.app.trip

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pleos NaviHelper에서 남은 주행 시간을 받아 주행 상태 소스에 넣는다.
 * 내비 서비스가 없는 환경(일반 에뮬레이터)에서는 연결에 실패하고, 그때는 데모 설정 값이 그대로 쓰인다.
 * 위치 좌표는 읽지 않는다. remainTime만 쓴다.
 */
class NaviBridge(context: Context, private val target: InjectedTripStateSource) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var helper: NaviHelper? = null
    private var routeActive = false
    private var newRoutePending = false

    private val _status = MutableStateFlow("내비 연동: 연결 중")
    val status: StateFlow<String> = _status

    /** 경로 안내 중이라 남은 시간을 내비에서 받고 있으면 true. 화면의 "내비 연동" 표시에 쓴다. */
    private val _receiving = MutableStateFlow(false)
    val receiving: StateFlow<Boolean> = _receiving

    private val poll = object : Runnable {
        override fun run() {
            if (routeActive) {
                runCatching { helper?.getDestinationInfo() }
                main.postDelayed(this, POLL_MS)
            }
        }
    }

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            // 목적지가 바뀌어 새 경로가 시작됐다. 새 남은 시간이 오면 그때 '목적지 변경'으로 알린다.
            main.post { newRoutePending = true }
            refresh("onRouteStarted")
        }
        override fun onRoutingCompleted() = refresh("onRoutingCompleted")
        override fun onReRouteEnded() = refresh("onReRouteEnded")

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            // remainTime 단위는 초. Pleos 에뮬레이터에서 내비 ETA 1시간 1분 → 3660으로 확인했다.
            Log.i(TAG, "onDestinationInfo remainTime=${destinationInfo.remainTime} remainDistance=${destinationInfo.remainDistance}")
            main.post {
                if (destinationInfo.remainTime <= 0) {
                    // 경로가 없으면 0이 온다. 이 값으로 편성을 비우지 않고, 경로가 시작될 때까지 폴링을 멈춘다.
                    routeActive = false
                    main.removeCallbacks(poll)
                    _receiving.value = false
                    _status.value = "내비 연동: 연결됨(목적지 대기)"
                    return@post
                }
                target.set(destinationInfo.remainTime, target.state.value.driving)
                if (newRoutePending) {
                    // 이전 경로에 붙인 시연용 정체 보정은 버린다.
                    newRoutePending = false
                    target.startNewRoute()
                }
                _receiving.value = true
                _status.value = "내비 연동: 남은 시간 수신 중"
            }
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) = stopRoute("onDestinationArrived", 0)
        override fun onRouteCancelled() = stopRoute("onRouteCancelled", null)

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "onError $errorCode")
        }
    }

    fun start() {
        runCatching {
            val h = NaviHelper(appContext)
            helper = h
            h.initialize(
                {
                    Log.i(TAG, "connected")
                    main.post { _status.value = "내비 연동: 연결됨(목적지 대기)" }
                    h.addListener(listener)
                    refresh("connected")
                },
                { t ->
                    Log.w(TAG, "connect failed: $t")
                    main.post { _status.value = "내비 연동: 없음(데모 설정 값 사용)" }
                },
            )
        }.onFailure {
            Log.w(TAG, "init failed: $it")
            _status.value = "내비 연동: 없음(데모 설정 값 사용)"
        }
    }

    fun stop() {
        main.removeCallbacks(poll)
        runCatching {
            helper?.removeListener(listener)
            helper?.release()
        }
        helper = null
    }

    private fun refresh(reason: String) {
        Log.i(TAG, reason)
        main.post {
            routeActive = true
            main.removeCallbacks(poll)
            poll.run()
        }
    }

    private fun stopRoute(reason: String, remainingSec: Int?) {
        Log.i(TAG, reason)
        main.post {
            routeActive = false
            main.removeCallbacks(poll)
            _receiving.value = false
            target.set(remainingSec, target.state.value.driving)
            _status.value = "내비 연동: 연결됨(목적지 대기)"
        }
    }

    private companion object {
        const val TAG = "DriveClassNavi"
        const val POLL_MS = 30_000L
    }
}
