package com.driveclass.app.trip

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 내비·차량 신호를 하나로 합친 상태. remainingSec가 null이면 목적지가 없다.
 * remainingSec에는 시연용 정체 보정([delaySec])이 이미 더해져 있다.
 */
data class TripState(
    val remainingSec: Int? = null,
    val driving: Boolean = false,
    val delaySec: Int = 0,
    /** 목적지가 바뀔 때마다 1씩 는다. 화면이 ETA 변화의 이유(목적지 변경)를 알 수 있게. */
    val routeSeq: Int = 0,
)

interface TripStateSource {
    val state: StateFlow<TripState>
}

/**
 * 주입 소스. 남은 시간의 기준값은 내비(NaviBridge)나 데모 설정이 넣고,
 * 시연용 정체·소통 보정은 그 위에 더한다. 그래서 내비 연동 중에도 정체 시연을 할 수 있다.
 */
class InjectedTripStateSource : TripStateSource {
    private val _state = MutableStateFlow(TripState())
    override val state: StateFlow<TripState> = _state

    private var baseSec: Int? = null
    private var driving = false
    private var delaySec = 0
    private var routeSeq = 0

    /** 기준 남은 시간과 주행 여부. 내비가 부른다. 정체 보정은 그대로 둔다. */
    fun set(remainingSec: Int?, driving: Boolean) {
        baseSec = remainingSec
        this.driving = driving
        publish()
    }

    fun setDriving(driving: Boolean) {
        this.driving = driving
        publish()
    }

    /** 데모 설정에서 남은 시간을 직접 정할 때. 정체 보정을 지운다. */
    fun setManual(remainingSec: Int) {
        baseSec = remainingSec
        delaySec = 0
        publish()
    }

    /** 시연용 정체(+) 또는 소통 원활(−). */
    fun addDelay(sec: Int) {
        delaySec += sec
        publish()
    }

    /** 새 경로가 시작됐다(내비 onRouteStarted). 이전 경로에 붙인 보정은 버린다. */
    fun startNewRoute() {
        delaySec = 0
        routeSeq++
        publish()
    }

    /** 목적지 변경 시연. 내비가 없을 때 데모 설정에서 부른다. */
    fun changeDestination(remainingSec: Int) {
        baseSec = remainingSec
        startNewRoute()
    }

    private fun publish() {
        val remaining = baseSec?.let { if (it > 0) (it + delaySec).coerceAtLeast(MIN_SEC) else it }
        _state.value = TripState(remaining, driving, delaySec, routeSeq)
    }

    private companion object {
        const val MIN_SEC = 60
    }
}

/**
 * 화면들이 함께 보는 주행 상태. 시작값 12분은 샘플 블록(총 14분)으로 정체·목적지 변경 시연이
 * 잘 보이는 길이다. 내비가 경로 안내 중이면 곧 내비 값으로 바뀐다.
 */
object TripStates {
    val injected = InjectedTripStateSource().apply { set(remainingSec = 12 * 60, driving = false) }
}
