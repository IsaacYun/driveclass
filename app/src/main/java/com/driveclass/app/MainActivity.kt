package com.driveclass.app

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.os.SystemClock
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.driveclass.app.ui.GlyphIcon
import com.driveclass.app.ui.LargeIconButton
import com.driveclass.app.ui.SkipGlyph
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.driveclass.app.ui.PleosTabs
import com.driveclass.engine.BlockKind
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.driveclass.app.playback.PlaybackClient
import com.driveclass.app.trip.InjectedTripStateSource
import com.driveclass.app.trip.NaviBridge
import com.driveclass.app.trip.TripStates
import com.driveclass.app.ui.BasicButton
import com.driveclass.app.ui.Glyph
import com.driveclass.app.ui.Icon3d
import com.driveclass.app.ui.PageNavigation
import com.driveclass.app.ui.PillShape
import com.driveclass.app.ui.Pressable
import com.driveclass.app.ui.PlainIconButton
import com.driveclass.app.ui.Pleos
import com.driveclass.app.ui.PleosCard
import com.driveclass.app.ui.PleosSize
import com.driveclass.app.ui.PleosSlider
import com.driveclass.app.ui.PleosSwitch
import com.driveclass.app.ui.PleosTheme
import com.driveclass.app.ui.PleosType
import com.driveclass.app.ui.ToastPopup
import com.driveclass.engine.Block
import com.driveclass.engine.Catalog
import com.driveclass.engine.Plan
import com.driveclass.engine.Planner
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val tripSource = TripStates.injected
    private lateinit var client: PlaybackClient
    private lateinit var navi: NaviBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = PlaybackClient(applicationContext).also { it.connect() }
        navi = NaviBridge(this, tripSource).also { it.start() }
        val available = SampleCatalog.availableIds(this)
        val catalog = SampleCatalog.catalog(available)
        setContent {
            PleosTheme { DrivingScreen(tripSource, client, catalog, available, navi) }
        }
    }

    override fun onDestroy() {
        navi.stop()
        client.release()
        super.onDestroy()
    }
}

/** 주행 화면(시안 v2의 Main). 상단 내비게이션, 지금 듣는 블록 카드, 다음 블록·오늘의 정리 카드. */
@Composable
private fun DrivingScreen(
    tripSource: InjectedTripStateSource,
    client: PlaybackClient,
    catalog: Catalog,
    available: Set<String>,
    navi: NaviBridge,
) {
    val c = Pleos.colors
    val context = LocalContext.current
    val trip by tripSource.state.collectAsState()
    val fromNavi by navi.receiving.collectAsState()
    val naviStatus by navi.status.collectAsState()
    val remainingSec = trip.remainingSec ?: 0
    val plan = remember(remainingSec, catalog) { Planner.plan(remainingSec, catalog) }
    val queued = client.queueIds.mapNotNull(SampleCatalog::block)
    val started = queued.isNotEmpty()
    val course = if (started) queued else plan.blocks.filter { it.id in available }
    val index = if (started) client.currentIndex.coerceIn(0, course.lastIndex) else 0
    val current = course.getOrNull(index)
    var showSettings by remember { mutableStateOf(false) }

    // 코스 진행 막대용 재생 위치. 재생 중에만 1초마다 읽는다.
    var positionSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(client.playing, index) {
        positionSec = client.positionSec()
        while (client.playing) {
            delay(1000)
            positionSec = client.positionSec()
        }
    }

    // 지금 블록에서 지나간 표현을 오늘의 정리에 하나씩 모은다(화면이 꺼져 있을 때는 재생 서비스가 모은다).
    // 끌거나 건너뛰어 지나친 구간은 모으지 않는다: 1초씩 이어 들은 구간만 넘긴다.
    val recallKeys by RecallStore.keys(context).collectAsState()
    var lastReach by remember { mutableStateOf<Pair<String, Int>?>(null) }
    LaunchedEffect(current?.id, positionSec, started) {
        if (!started || current == null) return@LaunchedEffect
        val prev = lastReach
        val from = when {
            prev?.first == current.id && positionSec - prev.second in 0..3 -> prev.second
            prev?.first != current.id && positionSec <= 3 -> -1
            else -> null
        }
        if (from != null) RecallStore.reach(context, current.id, positionSec, from)
        lastReach = current.id to positionSec
    }
    val scope = rememberCoroutineScope()
    fun refreshPosition() = scope.launch {
        delay(200)
        positionSec = client.positionSec()
    }

    // 도착 예정이 바뀌어 코스가 달라지면 ToastPopup으로 알리고, 새로 넣은 블록에 표시를 단다.
    val latestCourse by rememberUpdatedState(course)
    val latestTrip by rememberUpdatedState(trip)
    var lastEtaSec by remember { mutableIntStateOf(remainingSec) }
    var lastRouteSeq by remember { mutableIntStateOf(trip.routeSeq) }
    var lastDelaySec by remember { mutableIntStateOf(trip.delaySec) }
    var notice by remember { mutableStateOf<String?>(null) }
    var noticeSeq by remember { mutableIntStateOf(0) }
    var newIds by remember { mutableStateOf(emptySet<String>()) }

    // 슬라이더를 끌 때마다 부르지 않도록 잠깐 기다렸다가 재편성을 확인한다.
    LaunchedEffect(remainingSec) {
        delay(400)
        val prevEta = lastEtaSec
        val before = if (client.queueIds.isNotEmpty()) client.queueIds
        else Planner.plan(prevEta, catalog).blocks.filter { it.id in available }.map { it.id }
        if (remainingSec > 0) client.onRemainingChanged(remainingSec, catalog, available)
        delay(600) // 서비스 큐가 바뀐 결과가 화면 상태로 돌아올 때까지 기다린다.
        lastEtaSec = remainingSec
        val cause = when {
            latestTrip.routeSeq != lastRouteSeq -> EtaCause.Destination
            latestTrip.delaySec != lastDelaySec -> EtaCause.Traffic
            else -> EtaCause.Unknown
        }
        lastRouteSeq = latestTrip.routeSeq
        lastDelaySec = latestTrip.delaySec
        if (prevEta <= 0 || remainingSec <= 0) return@LaunchedEffect
        val after = latestCourse.map { it.id }
        val added = after.filter { it !in before }.mapNotNull(SampleCatalog::block)
        val removed = before.filter { it !in after }.mapNotNull(SampleCatalog::block)
        val text = courseChangeNotice(remainingSec - prevEta, remainingSec, cause, added, removed) ?: return@LaunchedEffect
        newIds = added.map { it.id }.toSet()
        notice = text
        noticeSeq++
    }
    LaunchedEffect(noticeSeq) {
        delay(6000)
        notice = null
    }

    // ── 주행이 끝날 때(시안 v4 ①~⑥) ──
    // 코스를 다 들었는지(재생 서비스가 알려 주는 finishedIds)와 주행이 어떻게 끝났는지(도착·주차)로 화면을 고른다.
    var endKind by remember { mutableStateOf<EndKind?>(null) }
    var endProgress by remember { mutableStateOf<CourseProgress?>(null) }
    /** 이번 주행에서 끝까지 들은 코스. 복습을 더 들어도 도착하면 ①로 정리한다. */
    var completed by remember { mutableStateOf<CourseProgress?>(null) }
    var handledFinish by remember { mutableStateOf<List<String>>(emptyList()) }
    var sawDriving by remember { mutableStateOf(trip.driving) }
    var wasDriving by remember { mutableStateOf(trip.driving) }
    var driveStartMs by remember { mutableStateOf<Long?>(null) }
    var resumeOffer by remember { mutableStateOf<CourseProgress?>(null) }
    val finished = client.finishedIds
    val arrived = trip.remainingSec == 0
    fun showEnd(kind: EndKind, p: CourseProgress) {
        // 주행이 끝난 화면(①③④)에서는 듣던 오디오를 멈춘다. ②⑥은 주행 중이라 그대로 둔다.
        if (kind != EndKind.DoneNearArrival && kind != EndKind.DoneTimeLeft) client.pause()
        endKind = kind
        endProgress = p
    }
    /** 주행 하나를 마무리한다(①·③·④를 닫을 때). */
    fun endTrip() {
        endKind = null
        completed = null
        sawDriving = false
        driveStartMs = null
        client.clearFinished()
    }
    /** 코스 중간에 주행이 끝났다: 재생을 멈추고 들은 자리를 저장한다. 들은 게 없으면 null. */
    fun stopMidCourse(): CourseProgress? {
        if (!started) return null
        val p = CourseProgress(client.queueIds, index, client.positionSec())
        if (p.heardSec <= 0) return null
        client.pause()
        ResumeStore.save(context, p)
        return p
    }
    // DEMO 창에서 바꾸는 중인 값(창을 닫기 전까지는 화면에 반영하지 않는다).
    var demoDriving by remember { mutableStateOf<Boolean?>(null) }
    var demoRemainingMin by remember { mutableStateOf<Int?>(null) }
    /** DEMO 창을 닫고 [DEMO_APPLY_DELAY_MS] 뒤에 바꾼 값과 [action]을 적용한다. */
    fun closeDemo(action: (() -> Unit)? = null) {
        showSettings = false
        val driving = demoDriving
        val min = demoRemainingMin
        demoDriving = null
        demoRemainingMin = null
        scope.launch {
            delay(DEMO_APPLY_DELAY_MS)
            if (min != null && min != tripSource.state.value.remainingSec?.div(60)) tripSource.setManual(min * 60)
            if (driving != null && driving != tripSource.state.value.driving) tripSource.setDriving(driving)
            action?.invoke()
        }
    }
    /** 오늘의 정리(정차 전용) 화면을 연다. 문장을 다시 보고 들을 수 있게 듣던 강의는 일시정지한다. */
    fun openRecap() {
        client.pause()
        context.startActivity(Intent(context, ParkedActivity::class.java))
    }
    // 내비가 도착을 알렸는데 '주행 중'이 꺼지지 않았어도 강의는 멈춘다. 정리 화면은 정차한 뒤에 띄운다.
    LaunchedEffect(arrived) {
        if (arrived && trip.driving && started) client.pause()
    }
    LaunchedEffect(trip.driving, arrived, finished, started) {
        // 정차 전용 종료 화면(①③④)을 열어 둔 채 다시 출발하면 닫는다. 문장 목록은 주행 중에 보이지 않아야 한다.
        if (trip.driving && !wasDriving) {
            when (endKind) {
                EndKind.Stopped, EndKind.ArrivedEarly -> {
                    client.stop()
                    endTrip()
                    // 저장해 둔 자리부터 이어 들을지 묻는다(⑤).
                    ResumeStore.load(context)?.let { resumeOffer = it }
                }
                EndKind.Arrived, EndKind.StoppedDone -> endTrip()
                else -> {}
            }
        }
        // 이어 듣기를 묻는 중에 다시 멈추면 자동 재생을 취소한다. 저장한 자리는 남겨 다음 출발 때 다시 묻는다.
        if (!trip.driving) resumeOffer = null
        if (trip.driving) {
            sawDriving = true
            if (driveStartMs == null) driveStartMs = SystemClock.elapsedRealtime()
        }
        // ⑤ 다시 출발: 저장해 둔 자리가 있으면 이어 듣기를 묻는다.
        if (trip.driving && !wasDriving && !started && endKind == null) {
            ResumeStore.load(context)?.let { resumeOffer = it }
        }
        wasDriving = trip.driving
        if (finished.isEmpty()) handledFinish = emptyList()
        if (finished.isNotEmpty() && finished != handledFinish) {
            handledFinish = finished
            // 코스를 끝낸 뒤 복습까지 들으면 앞서 끝낸 코스에 더한다(도착 정리에 둘 다 남게).
            val ids = ((completed?.ids ?: emptyList()) + finished).distinct()
            val done = CourseProgress(ids, ids.size, 0)
            completed = done
            // ②·⑥ 주행 중에 코스가 끝났다.
            if (trip.driving && !arrived && endKind == null) {
                showEnd(if (remainingSec > TIME_LEFT_SEC) EndKind.DoneTimeLeft else EndKind.DoneNearArrival, done)
            }
        }
        if (trip.driving) return@LaunchedEffect
        if (endKind != null && endKind != EndKind.DoneNearArrival && endKind != EndKind.DoneTimeLeft) return@LaunchedEffect
        val done = completed
        when {
            arrived && done != null -> showEnd(EndKind.Arrived, done)
            arrived -> stopMidCourse()?.let { showEnd(EndKind.ArrivedEarly, it) }
            sawDriving && done != null -> showEnd(EndKind.StoppedDone, done)
            sawDriving -> stopMidCourse()?.let { showEnd(EndKind.Stopped, it) }
        }
    }

    Box(Modifier.fillMaxSize().background(c.backgroundPrimary)) {
        Column(Modifier.fillMaxSize()) {
            val playable = CourseCatalog.subjects[CourseCatalog.PLAYABLE_SUBJECT]
            PageNavigation(title = "드라이브클래스") {
                DemoButton { showSettings = true }
                ArrivalChip(remainingSec, fromNavi)
            }
            Column(
                Modifier.weight(1f).padding(start = PleosSize.space48, end = PleosSize.space48, bottom = PleosSize.space48),
                verticalArrangement = Arrangement.spacedBy(PleosSize.space24),
            ) {
                CoursePicker(onUnavailable = { text ->
                    notice = text
                    noticeSeq++
                })
                NowPlayingCard(
                    course = course,
                    index = index,
                    current = current,
                    started = started,
                    playing = client.playing,
                    positionSec = if (started) positionSec else 0,
                    remainingSec = remainingSec,
                    allIncluded = available.all { id -> course.any { it.id == id } },
                    newIds = newIds,
                    onToggle = { client.togglePlay(plan, remainingSec, available) },
                    onPrev = { client.previous(); refreshPosition() },
                    onNext = { client.next() },
                    onSkip = { client.seekBy(it); refreshPosition() },
                    onSeek = { i, sec ->
                        client.seekTo(i, sec)
                        positionSec = sec
                    },
                )
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                    TodayBlocksCard(
                        course = course,
                        index = index,
                        started = started,
                        newIds = newIds,
                        note = if (course.isEmpty()) "목적지를 정하면 코스를 짜요" else null,
                        modifier = Modifier.weight(1.4f).fillMaxHeight(),
                    )
                    SummaryCard(
                        driving = trip.driving,
                        items = RecallStore.items(recallKeys.orEmpty()),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onOpen = { openRecap() },
                    )
                }
            }
        }
        AnimatedVisibility(visible = endKind != null, enter = fadeIn(tween(MOTION_MS)), exit = fadeOut(tween(MOTION_MS))) {
            var shownKind by remember { mutableStateOf(EndKind.Arrived) }
            endKind?.let { shownKind = it }
            val p = endProgress ?: return@AnimatedVisibility
            val ids = p.ids.toSet()
            EndScreen(
                kind = shownKind,
                progress = p,
                recall = RecallStore.items(recallKeys.orEmpty()).filter { it.blockId in ids },
                remainingSec = remainingSec,
                fromNavi = fromNavi,
                driveMin = driveStartMs?.let { ((SystemClock.elapsedRealtime() - it) / 60_000).toInt() } ?: 0,
                reviewMin = (catalog.reviews.sumOf { it.durationSec } + 59) / 60,
                onClose = {
                    when (shownKind) {
                        EndKind.DoneNearArrival, EndKind.DoneTimeLeft -> endKind = null
                        EndKind.Stopped, EndKind.ArrivedEarly -> {
                            client.stop()
                            endTrip()
                        }
                        else -> endTrip()
                    }
                },
                onOpenRecap = {
                    if (shownKind == EndKind.ArrivedEarly) client.stop()
                    endTrip()
                    openRecap()
                },
                onResumeNow = {
                    ResumeStore.clear(context)
                    client.play()
                    endKind = null
                    sawDriving = false
                },
                onReview = {
                    endKind = null
                    client.start(catalog.reviews, 0, remainingSec, available)
                },
                onNextEpisode = {
                    endKind = null
                    val p0 = CourseCatalog.subjects[CourseCatalog.PLAYABLE_SUBJECT]
                    notice = "'${p0.name} ${CourseCatalog.PLAYABLE_EPISODE + 1}회'는 준비 중이에요 · 시연은 ${CourseCatalog.PLAYABLE_EPISODE}회만 들을 수 있어요"
                    noticeSeq++
                },
            )
        }
        resumeOffer?.let { offer ->
            ResumePopup(
                p = offer,
                remainingSec = remainingSec,
                onResume = {
                    resumeOffer = null
                    ResumeStore.clear(context)
                    val left = offer.leftBlocks
                    if (left.isNotEmpty()) {
                        val done = offer.blocks.take(offer.doneCount).map { it.id }.toSet()
                        val blocks = if (remainingSec > 0) {
                            Planner.replan(
                                Plan(Planner.formatFor(remainingSec), left), 0,
                                left.first().durationSec - offer.partialSec, remainingSec, catalog, done,
                            ).blocks
                        } else left
                        client.start(blocks, offer.partialSec, remainingSec, available)
                    }
                },
                onNewCourse = {
                    resumeOffer = null
                    ResumeStore.clear(context)
                    client.togglePlay(plan, remainingSec, available)
                },
            )
        }
        AnimatedVisibility(
            visible = notice != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(PleosSize.space24),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut(),
        ) {
            // 사라지는 동안에도 마지막 문구를 보여 준다.
            var shown by remember { mutableStateOf("") }
            notice?.let { shown = it }
            ToastPopup(shown)
        }
        if (showSettings) {
            // 창이 떠 있는 동안 바꾼 값은 모아 두었다가, 창을 닫고 0.5초 뒤에 한꺼번에 적용한다.
            DemoSettingsPopup(
                driving = demoDriving ?: trip.driving,
                remainingMin = demoRemainingMin ?: (remainingSec / 60),
                naviStatus = naviStatus,
                fromNavi = fromNavi,
                onDriving = { demoDriving = it },
                onRemainingMin = { demoRemainingMin = it },
                onScenario = { scenario ->
                    val baseSec = demoRemainingMin?.times(60) ?: remainingSec
                    closeDemo {
                        when (scenario) {
                            DemoScenario.Jam -> tripSource.addDelay(JAM_SEC)
                            DemoScenario.Clear -> tripSource.addDelay(-CLEAR_SEC)
                            DemoScenario.NewDestination -> tripSource.changeDestination(nextDestinationSec(baseSec))
                        }
                    }
                },
                onClearRecall = {
                    closeDemo {
                        RecallStore.clear(context)
                        HeardStore.clear(context)
                        ResumeStore.clear(context)
                    }
                },
                onEnding = { ending ->
                    closeDemo {
                        when (ending) {
                            DemoEnding.Arrive -> tripSource.set(0, false)
                            DemoEnding.Park -> tripSource.setDriving(false)
                            DemoEnding.FinishCourse -> latestCourse.lastOrNull()?.takeIf { client.queueIds.isNotEmpty() }?.let {
                                client.seekTo(latestCourse.lastIndex, it.durationSec - 3)
                            }
                        }
                    }
                },
                onClose = { closeDemo() },
            )
        }
    }
}

/**
 * 상단 강의 선택 영역. 종목은 Tabs(Box), 회차는 Tabs(Line).
 * 지금은 '영어 회의 표현' 3회만 들을 수 있어서, 다른 회차를 고르면 토스트로 알리고 선택은 그대로 둔다.
 * 종목은 넘겨 보며 회차 목록을 볼 수 있다.
 */
@Composable
private fun CoursePicker(onUnavailable: (String) -> Unit) {
    val c = Pleos.colors
    val subjects = CourseCatalog.subjects
    var subject by remember { mutableIntStateOf(CourseCatalog.PLAYABLE_SUBJECT) }
    val episodes = subjects[subject].episodes
    val playingTab = if (subject == CourseCatalog.PLAYABLE_SUBJECT) CourseCatalog.PLAYABLE_EPISODE - 1 else -1
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PleosSize.space40),
    ) {
        PleosTabs(
            labels = subjects.map { it.name },
            selected = subject,
            onSelect = { subject = it },
            modifier = Modifier.weight(1.1f, fill = false),
        )
        PleosTabs(
            labels = episodes.map { "${it.no}회" },
            selected = playingTab,
            onSelect = { i ->
                val e = episodes[i]
                if (!CourseCatalog.isPlayable(subject, e.no)) {
                    val p = subjects[CourseCatalog.PLAYABLE_SUBJECT]
                    onUnavailable("'${subjects[subject].name} ${e.no}회'는 준비 중이에요 · 시연은 ${p.name} ${CourseCatalog.PLAYABLE_EPISODE}회만 들을 수 있어요")
                }
            },
            modifier = Modifier.weight(1f, fill = false),
            line = true,
            marked = if (playingTab >= 0) setOf(playingTab) else emptySet(),
        )
    }
    }
}

/** 시연용 설정을 여는 DEMO 버튼. 실제 차에서는 내비와 차량 신호가 대신하므로 시연에서만 쓴다. */
@Composable
private fun DemoButton(onClick: () -> Unit) {
    val c = Pleos.colors
    Pressable(
        onClick = onClick,
        modifier = Modifier.height(80.dp).semantics { contentDescription = "데모 설정" },
        shape = PillShape,
        color = c.buttonBasicEnabled,
        pressedColor = c.buttonBasicPressed,
    ) {
        Text("DEMO", style = PleosType.labelMediumStrong, color = c.textDarkSecondary, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

/** 도착까지 남은 시간 칩. 분 숫자가 바뀌면 늘면 아래에서, 줄면 위에서 밀려 들어온다. */
@Composable
internal fun ArrivalChip(remainingSec: Int, fromNavi: Boolean) {
    val c = Pleos.colors
    Row(
        Modifier.height(80.dp).clip(PillShape).background(c.surfaceHigh).padding(start = 12.dp, end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon3d(R.drawable.icon3d_arrival, 72.dp)
        if (remainingSec > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("도착까지 ", style = PleosType.titleSmall, color = c.textDarkPrimary)
                AnimatedContent(
                    targetState = remainingSec / 60,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(tween(MOTION_MS)) { if (up) it else -it } + fadeIn(tween(MOTION_MS))) togetherWith
                            (slideOutVertically(tween(MOTION_MS)) { if (up) -it else it } + fadeOut(tween(MOTION_MS))) using
                            SizeTransform(clip = true)
                    },
                    label = "eta",
                ) { min -> Text("$min", style = PleosType.titleSmall, color = c.textDarkPrimary) }
                Text("분", style = PleosType.titleSmall, color = c.textDarkPrimary)
            }
        } else {
            Text("목적지를 정해 주세요", style = PleosType.titleSmall, color = c.textDarkPrimary)
        }
    }
}

/**
 * 지금 듣는 블록 카드(시안 v3 A안): 제목, 블록 타임라인, 조작 줄
 * [이전] [15초 뒤로] [재생/일시정지] [30초 앞으로] [다음].
 */
@Composable
private fun NowPlayingCard(
    course: List<Block>,
    index: Int,
    current: Block?,
    started: Boolean,
    playing: Boolean,
    positionSec: Int,
    remainingSec: Int,
    allIncluded: Boolean,
    newIds: Set<String>,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSkip: (Int) -> Unit,
    onSeek: (Int, Int) -> Unit,
) {
    val c = Pleos.colors
    val totalSec = course.sumOf { it.durationSec }
    val heardSec = course.take(index).sumOf { it.durationSec } + positionSec.coerceAtMost(current?.durationSec ?: 0)
    PleosCard(Modifier.fillMaxWidth(), padding = PleosSize.space40) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PleosSize.space40)) {
            Icon3d(R.drawable.icon3d_headphones, 140.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 시안 v5: 블록 제목만 둔다. 코스가 없을 때만 안내 한 줄.
                Text(current?.title ?: "목적지를 정하면 코스를 짜 드려요", style = PleosType.headlineMedium, color = c.textDarkPrimary)
                if (course.isEmpty()) {
                    Text("내비에서 경로 안내를 시작하면 도착 시간에 맞춰요", style = PleosType.bodyMedium, color = c.textDarkSecondary)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (course.isNotEmpty()) {
            CourseTimeline(course, index, if (started) positionSec else 0, started, newIds, onSeek)
            Spacer(Modifier.height(28.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (current != null) "${clock(positionSec.coerceAtMost(current.durationSec))} / ${clock(current.durationSec)}" else "",
                style = PleosType.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = c.textDarkTertiary,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LargeIconButton("이전 블록", onPrev, enabled = started) { GlyphIcon(Glyph.Prev, it, size = 40.dp) }
                LargeIconButton("15초 뒤로", { onSkip(-15) }, enabled = started) { SkipGlyph(15, forward = false, color = it) }
                BasicButton(
                    text = if (playing) "일시정지" else "재생",
                    icon = if (playing) Glyph.Pause else Glyph.Play,
                    onClick = onToggle,
                    modifier = Modifier.widthIn(min = 240.dp),
                    secondary = true,
                    large = true,
                )
                LargeIconButton("30초 앞으로", { onSkip(30) }, enabled = started) { SkipGlyph(30, forward = true, color = it) }
                LargeIconButton("다음 블록", onNext, enabled = started) { GlyphIcon(Glyph.Next, it, size = 40.dp) }
            }
            val endsBeforeMin = (remainingSec - (totalSec - heardSec)) / 60
            Text(
                when {
                    course.isEmpty() -> ""
                    endsBeforeMin >= 1 -> "도착 ${endsBeforeMin}분 전에 끝나요"
                    else -> "도착할 즈음 끝나요"
                },
                style = PleosType.bodyMedium,
                color = c.textDarkTertiary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 블록 길이에 비례해 나눈 코스 타임라인. 들은 부분은 informative_positive(진행),
 * 트랙은 controller_slider_normal. 칸마다 아래에 블록 종류 라벨을 단다.
 *
 * 길게 눌러 끌면(재생 중일 때) 손잡이와 말풍선이 따라오고, 손을 떼면 그 위치로 옮긴다.
 * 도착 시간이 바뀌어 블록이 들어오면 그 칸이 0에서 자라며 한 번 밝게 비치고,
 * 빠지는 블록은 줄어들며 사라진다.
 */
@Composable
private fun CourseTimeline(
    course: List<Block>,
    index: Int,
    positionSec: Int,
    enabled: Boolean,
    newIds: Set<String>,
    onSeek: (Int, Int) -> Unit,
) {
    val c = Pleos.colors
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val gap = 8.dp

    // 빠진 블록을 잠깐 남겨 두어 줄어드는 모습을 보여 준다.
    var shown by remember { mutableStateOf(course) }
    var leaving by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(course.map { it.id }) {
        val ids = course.map { it.id }.toSet()
        val gone = shown.filter { it.id !in ids }
        if (gone.isEmpty()) {
            shown = course
            return@LaunchedEffect
        }
        val merged = course.toMutableList()
        gone.forEach { g ->
            val pred = shown.take(shown.indexOf(g)).lastOrNull { it in merged }
            merged.add(if (pred == null) 0 else merged.indexOf(pred) + 1, g)
        }
        shown = merged
        leaving = gone.map { it.id }.toSet()
        delay(MOTION_MS + 150L)
        shown = course
        leaving = emptySet()
    }
    // 직전 화면에 있던 블록. 이번에 처음 나타난 칸만 0에서 자라게 한다(처음 그릴 때는 그대로).
    val known = remember { mutableSetOf<String>() }
    val firstDraw = known.isEmpty()
    SideEffect { known.clear(); known.addAll(shown.map { it.id }) }

    var dragX by remember { mutableStateOf<Float?>(null) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth.toFloat()
        val gapPx = with(density) { gap.toPx() }
        val totalSec = course.sumOf { it.durationSec }.coerceAtLeast(1)
        val usable = (widthPx - gapPx * (course.size - 1)).coerceAtLeast(1f)
        fun startOf(i: Int) = course.take(i).sumOf { it.durationSec } * usable / totalSec + gapPx * i
        fun widthOf(i: Int) = course[i].durationSec * usable / totalSec
        fun xOf(i: Int, sec: Int) = startOf(i) + widthOf(i) * (sec.toFloat() / course[i].durationSec).coerceIn(0f, 1f)
        fun pointAt(x: Float): Pair<Int, Int> {
            for (i in course.indices) {
                if (x <= startOf(i) + widthOf(i) + gapPx / 2 || i == course.lastIndex) {
                    val f = ((x - startOf(i)) / widthOf(i)).coerceIn(0f, 1f)
                    return i to (f * course[i].durationSec).toInt().coerceAtMost(course[i].durationSec - 1)
                }
            }
            return 0 to 0
        }
        val target = dragX?.let(::pointAt)
        // 제스처 블록은 시작할 때의 람다를 붙잡고 있으므로, 놓을 때는 지금 그려진 기준으로 다시 계산한다.
        val currentPointAt by rememberUpdatedState(::pointAt)
        val currentOnSeek by rememberUpdatedState(onSeek)
        val markIndex = target?.first ?: index
        val markSec = target?.second ?: positionSec
        val trackH by animateDpAsState(if (dragX != null) 24.dp else 16.dp, label = "track")

        Row(
            Modifier.fillMaxWidth()
                .semantics {
                    contentDescription = "코스 ${course.size}블록 중 ${index + 1}번째 블록 ${clock(positionSec)}. 길게 눌러 끌면 옮겨요"
                }
                .pointerInput(enabled, course.map { it.id }) {
                    if (!enabled) return@pointerInput
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            dragX = it.x.coerceIn(0f, widthPx)
                        },
                        onDrag = { change, _ -> dragX = change.position.x.coerceIn(0f, widthPx) },
                        onDragEnd = {
                            dragX?.let { currentPointAt(it) }?.let { (i, sec) -> currentOnSeek(i, sec) }
                            dragX = null
                        },
                        onDragCancel = { dragX = null },
                    )
                },
        ) {
            shown.forEachIndexed { i, b ->
                key(b.id) {
                    val ci = course.indexOfFirst { it.id == b.id }
                    val goingAway = b.id in leaving
                    val appearing = remember { !firstDraw && b.id !in known }
                    val weight = remember { Animatable(if (appearing) 0f else b.durationSec.toFloat()) }
                    val flash = remember { Animatable(if (appearing) 0.45f else 0f) }
                    LaunchedEffect(goingAway) {
                        weight.animateTo(if (goingAway) 0f else b.durationSec.toFloat(), tween(MOTION_MS))
                    }
                    LaunchedEffect(Unit) {
                        if (flash.value > 0f) {
                            delay(MOTION_MS.toLong())
                            flash.animateTo(0f, tween(1400))
                        }
                    }
                    val alpha by animateFloatAsState(if (goingAway) 0f else 1f, tween(MOTION_MS), label = "alpha")
                    val fill = when {
                        ci < 0 -> 0f
                        ci < markIndex -> 1f
                        ci == markIndex -> (markSec.toFloat() / b.durationSec).coerceIn(0f, 1f)
                        else -> 0f
                    }
                    Column(
                        Modifier.weight(weight.value.coerceAtLeast(0.001f))
                            .padding(start = if (i == 0) 0.dp else gap)
                            .graphicsLayer { this.alpha = alpha },
                    ) {
                        Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                            Box(
                                Modifier.fillMaxWidth().height(trackH).clip(PillShape)
                                    .background(if (dragX != null) c.controllerSliderPressed else c.controllerSliderNormal),
                            ) {
                                if (fill > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(c.informativePositive))
                                if (flash.value > 0f) {
                                    Box(Modifier.matchParentSize().background(c.informativePositive.copy(alpha = flash.value)))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val strong = ci == markIndex && (enabled || dragX != null)
                        Text(
                            kindLabel(b) + when {
                                dragX != null && ci == markIndex -> " · 여기로"
                                strong -> " · 듣는 중"
                                b.id in newIds -> " · 새로 넣음"
                                else -> ""
                            },
                            style = if (strong) PleosType.labelMediumStrong else PleosType.labelMedium,
                            color = if (strong) c.textDarkPrimary else c.textDarkTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // 손잡이(controller_slider_knob + 그림자). 재생 중에만 보인다.
        if (enabled && markIndex in course.indices) {
            val knob = if (dragX != null) 48.dp else 36.dp
            val x = dragX ?: xOf(markIndex, markSec)
            val knobPx = with(density) { knob.toPx() }
            Box(
                Modifier
                    .offset { IntOffset((x - knobPx / 2).toInt(), with(density) { (22.dp - knob / 2).roundToPx() }) }
                    .size(knob)
                    .shadow(if (dragX != null) 16.dp else 10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(c.controllerSliderKnob),
            )
        }
        // 끄는 동안 손잡이 위에 옮겨 갈 블록과 시각을 띄운다.
        if (dragX != null && target != null) {
            var bubbleW by remember { mutableIntStateOf(0) }
            val x = dragX ?: 0f
            Row(
                Modifier
                    .onSizeChanged { bubbleW = it.width }
                    .offset {
                        IntOffset(
                            (x - bubbleW / 2).toInt().coerceIn(0, (widthPx - bubbleW).toInt().coerceAtLeast(0)),
                            with(density) { -92.dp.roundToPx() },
                        )
                    }
                    .shadow(16.dp, PillShape)
                    .clip(PillShape)
                    .background(c.buttonFilledEnabled)
                    .padding(horizontal = 36.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val b = course[target.first]
                Text(b.title, style = PleosType.bodyMediumStrong, color = c.textLightPrimary)
                Text(
                    "${clock(target.second)} / ${clock(b.durationSec)}",
                    style = PleosType.bodyMedium.copy(fontFeatureSettings = "tnum"),
                    color = c.textLightPrimary,
                )
            }
        }
    }
}

/**
 * 다음에 들을 블록·오늘의 정리가 함께 쓰는 카드 틀(시안 v3): 아이콘 88 + 제목 + 오른쪽 개수,
 * 설명 한 줄, 그 아래 목록. 목록이 넘치면 오른쪽 여백에 스크롤바를 보인다.
 */
@Composable
internal fun ListCard(
    icon: Int,
    title: String,
    meta: String?,
    desc: String?,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = Pleos.colors
    PleosCard(modifier, padding = PleosSize.space40, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon3d(icon, 88.dp)
            Text(title, style = PleosType.titleSmall, color = c.textDarkPrimary, modifier = Modifier.weight(1f))
            if (meta != null) Text(meta, style = PleosType.bodyMedium, color = c.textDarkTertiary)
        }
        Spacer(Modifier.height(16.dp))
        if (desc != null) {
            Text(desc, style = PleosType.bodySmall, color = c.textDarkSecondary)
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}

/** 카드 목록의 한 줄: 순번(null이면 자리 없이 생략), 제목(+ 둘째 줄), 표시, 오른쪽 정보. 방금 바뀐 줄은 잠깐 surface_high로 비춘다. */
@Composable
internal fun ListRow(
    modifier: Modifier,
    no: String?,
    title: String,
    divider: Boolean,
    sub: String? = null,
    tag: String? = null,
    trailing: String? = null,
    highlight: Boolean = false,
    dimmed: Boolean = false,
    checked: Boolean = false,
) {
    val c = Pleos.colors
    val bg by animateColorAsState(if (highlight) c.surfaceHigh else c.surfaceBasic, tween(600), label = "row")
    Column(modifier) {
        if (divider) Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(PleosSize.radius24)).background(bg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (checked) GlyphIcon(Glyph.Check, c.informativePositive, size = 32.dp)
            else if (no != null) Text(no, style = PleosType.labelMediumStrong, color = c.textDarkTertiary, modifier = Modifier.width(32.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        title,
                        style = if (sub != null) PleosType.bodyLargeStrong else PleosType.bodyLarge,
                        color = if (dimmed) c.textDarkTertiary else c.textDarkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (tag != null) Text(tag, style = PleosType.labelMediumStrong, color = c.informativePositive)
                }
                if (sub != null) Text(sub, style = PleosType.labelMedium, color = c.textDarkTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (trailing != null) Text(trailing, style = PleosType.bodyMedium, color = c.textDarkTertiary)
        }
    }
}

/**
 * 목록이 카드보다 길 때만 오른쪽 여백(카드 패딩)에 스크롤바 손잡이를 그린다.
 * 디자인 시스템 Bar(Scroll bar) 규격: 손잡이 basic_alpha_dark_200, 넘칠 게 없으면 숨김.
 */
internal fun Modifier.scrollbar(state: LazyListState, color: Color) = drawWithContent {
    drawContent()
    val info = state.layoutInfo
    val items = info.visibleItemsInfo
    if (items.isEmpty() || !(state.canScrollForward || state.canScrollBackward)) return@drawWithContent
    val avg = items.sumOf { it.size }.toFloat() / items.size
    val content = avg * info.totalItemsCount
    val viewport = size.height
    if (content <= viewport) return@drawWithContent
    val scrolled = state.firstVisibleItemIndex * avg + state.firstVisibleItemScrollOffset
    val h = (viewport * viewport / content).coerceAtLeast(40.dp.toPx())
    val y = (scrolled / (content - viewport)).coerceIn(0f, 1f) * (viewport - h)
    val w = 6.dp.toPx()
    drawRoundRect(color, Offset(size.width + 17.dp.toPx(), y), Size(w, h), CornerRadius(w / 2))
}

/**
 * 오늘의 블록 전체. 이미 들은 블록(완료)과 지금 듣는 블록, 남은 블록을 한 목록에 보여 준다.
 * 들어온 블록은 펼쳐지며 잠깐 비치고, 빠진 블록은 흐려지며 접힌다.
 */
@Composable
private fun TodayBlocksCard(course: List<Block>, index: Int, started: Boolean, newIds: Set<String>, note: String?, modifier: Modifier) {
    val c = Pleos.colors
    val list = rememberLazyListState()
    var glowing by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(newIds) {
        glowing = newIds
        // 새로 넣은 블록이 화면 밖에 있을 때만 그 블록까지 넘긴다.
        val i = course.indexOfFirst { it.id in newIds }
        if (i >= 0) {
            val info = list.layoutInfo
            val shown = info.visibleItemsInfo.any { it.index == i && it.offset + it.size <= info.viewportEndOffset }
            if (!shown) list.animateScrollToItem(i)
        }
        delay(3000)
        glowing = emptySet()
    }
    // 블록이 넘어가면 지금 듣는 블록이 목록 맨 위 근처에 오도록 맞춘다.
    LaunchedEffect(index, started) {
        if (started && index in course.indices) list.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    ListCard(
        icon = R.drawable.icon3d_speech,
        title = "오늘의 블록",
        meta = null,
        desc = note,
        modifier = modifier,
    ) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().scrollbar(list, c.alphaDark200)) {
            itemsIndexed(course, key = { _, b -> b.id }) { i, b ->
                val done = started && i < index
                val now = started && i == index
                ListRow(
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(MOTION_MS),
                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                        fadeOutSpec = tween(MOTION_MS),
                    ),
                    no = "${i + 1}",
                    title = b.title,
                    divider = i > 0,
                    tag = when {
                        now -> "듣는 중"
                        b.id in newIds -> "새로 넣음"
                        else -> null
                    },
                    trailing = if (done) "들음" else "${kindLabel(b)} · ${(b.durationSec + 59) / 60}분",
                    highlight = b.id in glowing,
                    dimmed = done,
                    checked = done,
                )
            }
        }
    }
}

/**
 * 오늘의 정리 카드. 블록을 듣는 동안 지나간 표현이 하나씩 모인다.
 * 주행 중에는 표현 문장을 띄우지 않고 블록별 개수만, 정차 중에는 문장과 뜻을 최근 것부터 보여 준다.
 */
@Composable
private fun SummaryCard(
    driving: Boolean,
    items: List<RecallStore.Item>,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val c = Pleos.colors
    val list = rememberLazyListState()
    val latestKey = items.lastOrNull()?.key
    // 방금 모인 표현을 몇 초 동안 강조한다.
    var fresh by remember { mutableStateOf<String?>(null) }
    var seenKey by remember { mutableStateOf(latestKey) }
    LaunchedEffect(latestKey) {
        if (latestKey != null && latestKey != seenKey) {
            fresh = latestKey
            list.animateScrollToItem(0)
            delay(4000)
            fresh = null
        }
        seenKey = latestKey
    }
    ListCard(
        icon = if (driving) R.drawable.icon3d_parking else R.drawable.icon3d_notebook,
        title = "오늘의 정리",
        meta = null,
        desc = if (items.isEmpty()) "블록을 듣다 보면 되새길 표현이 하나씩 모여요" else null,
        modifier = modifier,
        onClick = if (driving) null else onOpen,
    ) {
        val rowMotion = Modifier
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().scrollbar(list, c.alphaDark200)) {
            if (driving) {
                val byBlock = items.groupBy { it.blockId }.toList().asReversed()
                itemsIndexed(byBlock, key = { _, it -> it.first }) { i, (id, got) ->
                    val b = SampleCatalog.block(id) ?: return@itemsIndexed
                    ListRow(
                        modifier = rowMotion.animateItem(fadeInSpec = tween(MOTION_MS), fadeOutSpec = tween(MOTION_MS)),
                        no = null,
                        title = b.title,
                        divider = i > 0,
                        tag = if (got.any { it.key == fresh }) "새 표현" else null,
                        trailing = "${got.size} / ${SampleCatalog.keySentences[id]?.size ?: got.size}",
                        highlight = got.any { it.key == fresh },
                    )
                }
            } else {
                itemsIndexed(items.asReversed(), key = { _, it -> it.key }) { i, item ->
                    ListRow(
                        modifier = rowMotion.animateItem(fadeInSpec = tween(MOTION_MS), fadeOutSpec = tween(MOTION_MS)),
                        no = null,
                        title = item.en,
                        sub = item.ko,
                        divider = i > 0,
                        tag = if (item.key == fresh) "새 표현" else null,
                        highlight = item.key == fresh,
                    )
                }
            }
        }
    }
}

internal fun kindLabel(b: Block) = when (b.kind) {
    BlockKind.INTRO -> "도입"
    BlockKind.CORE -> "핵심"
    BlockKind.REVIEW -> "복습"
    BlockKind.CLOSING -> "마무리"
}

/** 초를 m:ss로. */
internal fun clock(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** 코스가 바뀔 때의 모션 길이. 디자인 시스템 원칙(주행 중 시선 끌지 않기)에 따라 짧게 한 번만. */
internal const val MOTION_MS = 450

/** DEMO 창을 닫은 뒤 변경을 적용하기까지의 간격. 창이 사라진 화면에서 바뀌는 모습이 보이게 한다. */
private const val DEMO_APPLY_DELAY_MS = 500L

/**
 * 데모 설정 팝업(SystemPopup). 시연용 주행 상태와 남은 시간을 넣는다.
 * 컨테이너 background_popup, radius_48, 패딩 space_48, 뒤 화면은 overlay_default.
 */
@Composable
private fun DemoSettingsPopup(
    driving: Boolean,
    remainingMin: Int,
    naviStatus: String,
    fromNavi: Boolean,
    onDriving: (Boolean) -> Unit,
    onRemainingMin: (Int) -> Unit,
    onScenario: (DemoScenario) -> Unit,
    onClearRecall: () -> Unit,
    onEnding: (DemoEnding) -> Unit,
    onClose: () -> Unit,
) {
    val c = Pleos.colors
    Box(
        Modifier.fillMaxSize().background(c.overlayDefault),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(PleosSize.space48).fillMaxWidth(0.55f)
                .clip(RoundedCornerShape(PleosSize.radius48))
                .background(c.backgroundPopup)
                .verticalScroll(rememberScrollState())
                .padding(PleosSize.space48),
            verticalArrangement = Arrangement.spacedBy(PleosSize.space24),
        ) {
            Text("데모 설정", style = PleosType.titleMedium, color = c.textDarkPrimary, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("시연용 값이에요. 실제 차에서는 내비와 차량 신호가 대신 넣어요.", style = PleosType.bodyMedium, color = c.textDarkSecondary, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("주행 중", style = PleosType.bodyLarge, color = c.textDarkPrimary, modifier = Modifier.weight(1f))
                PleosSwitch(driving, "주행 중", onDriving)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("남은 주행 시간", style = PleosType.bodyLarge, color = c.textDarkPrimary, modifier = Modifier.weight(1f))
                Text("${remainingMin}분", style = PleosType.titleSmall, color = c.textDarkPrimary)
            }
            PleosSlider(remainingMin.toFloat(), 0f..120f, "남은 주행 시간") { onRemainingMin(it.toInt()) }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
            Text("도착 시간 바꿔 보기", style = PleosType.bodyLarge, color = c.textDarkPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                BasicButton("정체 +${JAM_SEC / 60}분", { onScenario(DemoScenario.Jam) }, Modifier.weight(1f))
                BasicButton("소통 원활 −${CLEAR_SEC / 60}분", { onScenario(DemoScenario.Clear) }, Modifier.weight(1f))
                if (!fromNavi) {
                    BasicButton(
                        "목적지 → ${nextDestinationSec(remainingMin * 60) / 60}분",
                        { onScenario(DemoScenario.NewDestination) },
                        Modifier.weight(1f),
                    )
                }
            }
            if (fromNavi) {
                Text("목적지는 내비에서 바꾸면 바로 반영돼요", style = PleosType.bodySmall, color = c.textDarkTertiary)
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
            Text("주행 끝내 보기", style = PleosType.bodyLarge, color = c.textDarkPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                BasicButton("목적지 도착", { onEnding(DemoEnding.Arrive) }, Modifier.weight(1f))
                BasicButton("중간에 주차", { onEnding(DemoEnding.Park) }, Modifier.weight(1f))
                BasicButton("코스 끝으로", { onEnding(DemoEnding.FinishCourse) }, Modifier.weight(1f))
            }
            Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("내비 연동", style = PleosType.bodyMedium, color = c.textDarkSecondary, modifier = Modifier.weight(1f))
                Text(naviStatus.removePrefix("내비 연동: "), style = PleosType.bodyMediumStrong, color = c.informativePositive)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                BasicButton("오늘의 정리 비우기", onClearRecall, Modifier.weight(1f), large = true)
                BasicButton("완료", onClose, Modifier.weight(1f), secondary = true, large = true)
            }
        }
    }
}

/** 데모 설정의 주행 끝 시나리오: 도착, 목적지 전 주차, 코스 마지막 몇 초로 건너뛰기. */
private enum class DemoEnding { Arrive, Park, FinishCourse }

/** 데모 설정의 도착 시간 시나리오. */
private enum class DemoScenario { Jam, Clear, NewDestination }

/** 도착 예정이 바뀐 이유. 내비 경로 시작·데모 목적지 변경은 Destination, 정체 보정은 Traffic. */
private enum class EtaCause { Destination, Traffic, Unknown }

private const val JAM_SEC = 8 * 60
private const val CLEAR_SEC = 5 * 60

/** 목적지 변경 시연: 지금보다 가까운 곳(6분)과 먼 곳(25분)을 번갈아 고른다. */
private fun nextDestinationSec(remainingSec: Int) = if (remainingSec > 10 * 60) 6 * 60 else 25 * 60

/**
 * 도착 예정 변화와 코스 변화를 한 문장으로. 변화가 작고 코스도 그대로면 null(알리지 않음).
 * 내비가 30초마다 주는 자연스러운 감소에는 울리지 않도록 재편성 기준(3분)을 함께 쓴다.
 */
private fun courseChangeNotice(
    deltaSec: Int,
    remainingSec: Int,
    cause: EtaCause,
    added: List<Block>,
    removed: List<Block>,
): String? {
    if (cause != EtaCause.Destination && added.isEmpty() && removed.isEmpty() &&
        kotlin.math.abs(deltaSec) < Planner.REPLAN_THRESHOLD_SEC
    ) return null
    val min = kotlin.math.abs(deltaSec) / 60
    val eta = when {
        cause == EtaCause.Destination -> "목적지 변경으로 도착까지 ${remainingSec / 60}분"
        min == 0 -> "도착 예정이 바뀌었어요"
        cause == EtaCause.Traffic && deltaSec > 0 -> "정체로 도착이 ${min}분 늦어져요"
        cause == EtaCause.Traffic -> "길이 뚫려 도착이 ${min}분 빨라져요"
        deltaSec > 0 -> "도착까지 ${min}분 더 걸려요"
        else -> "도착까지 ${min}분 줄었어요"
    }
    val course = when {
        added.isNotEmpty() && removed.isNotEmpty() -> "코스를 다시 짰어요"
        added.isNotEmpty() -> "${blockNames(added)} 더 넣었어요"
        removed.isNotEmpty() -> "${blockNames(removed)} 뺐어요"
        deltaSec > 0 -> "준비한 블록은 이미 다 담았어요"
        else -> "지금 코스 그대로 들어요"
    }
    return "$eta · $course"
}

/** '일정 조율하기'를 / 블록 2개를 */
private fun blockNames(blocks: List<Block>): String {
    val first = "'${blocks.first().title}'"
    if (blocks.size > 1) return "블록 ${blocks.size}개를"
    val last = blocks.first().title.last()
    val hasFinal = last in '가'..'힣' && (last - '가') % 28 != 0
    return first + if (hasFinal) "을" else "를"
}
