package com.driveclass.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.driveclass.app.ui.BasicButton
import com.driveclass.app.ui.Glyph
import com.driveclass.app.ui.GlyphIcon
import com.driveclass.app.ui.Icon3d
import com.driveclass.app.ui.PlainIconButton
import com.driveclass.app.ui.Pleos
import com.driveclass.app.ui.PleosCard
import com.driveclass.app.ui.PleosSize
import com.driveclass.app.ui.PleosType
import com.driveclass.app.ui.Pressable
import com.driveclass.engine.Block
import kotlinx.coroutines.delay

/**
 * 주행이 끝날 때 앱 패널 전체를 덮는 화면(시안 v4 ①~⑥).
 * 코스를 다 들었는지와 주행이 어떻게 끝났는지로 고른다.
 */
enum class EndKind {
    /** ① 코스를 다 듣고 목적지에 도착. */
    Arrived,
    /** ①과 같은 구성: 코스를 다 듣고 목적지 전에 주차. */
    StoppedDone,
    /** ② 코스를 다 들었고 아직 주행 중, 곧 도착. */
    DoneNearArrival,
    /** ③ 코스 중간에 목적지 전에 주행이 끝남. */
    Stopped,
    /** ④ 코스가 남은 채 목적지에 도착. */
    ArrivedEarly,
    /** ⑥ 코스를 다 들었고 도착까지 한참 남음. */
    DoneTimeLeft,
}

/** 이 시간보다 많이 남았으면 ② 대신 ⑥(다음 회차 권유)을 띄운다. */
const val TIME_LEFT_SEC = 15 * 60

/** ⑥에서 고르지 않으면 조용히 가기로 넘어가는 시간, ⑤에서 이어 듣기로 넘어가는 시간. */
private const val QUIET_AUTO_SEC = 10
const val RESUME_AUTO_SEC = 5

private val NEXT_EPISODE: Episode
    get() = CourseCatalog.subjects[CourseCatalog.PLAYABLE_SUBJECT].episodes[CourseCatalog.PLAYABLE_EPISODE]

@Composable
fun EndScreen(
    kind: EndKind,
    progress: CourseProgress,
    recall: List<RecallStore.Item>,
    remainingSec: Int,
    fromNavi: Boolean,
    driveMin: Int,
    reviewMin: Int,
    onClose: () -> Unit,
    onOpenRecap: () -> Unit,
    onResumeNow: () -> Unit,
    onReview: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    val c = Pleos.colors
    Column(
        Modifier.fillMaxSize().background(c.backgroundPrimary)
            // 뒤의 주행 화면이 눌리지 않게 터치를 여기서 받는다.
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        EndTopBar(kind, remainingSec, fromNavi, onClose)
        Column(
            Modifier.weight(1f).padding(start = PleosSize.space48, end = PleosSize.space48, bottom = PleosSize.space48),
            verticalArrangement = Arrangement.spacedBy(PleosSize.space24),
        ) {
            when (kind) {
                EndKind.DoneNearArrival, EndKind.DoneTimeLeft ->
                    DoneWhileDriving(kind, progress, recall.size, remainingSec, reviewMin, onClose, onReview, onNextEpisode)
                else -> Summary(kind, progress, recall, driveMin, onClose, onOpenRecap, onResumeNow)
            }
        }
    }
}

@Composable
private fun EndTopBar(kind: EndKind, remainingSec: Int, fromNavi: Boolean, onClose: () -> Unit) {
    val c = Pleos.colors
    Row(
        Modifier.fillMaxWidth().height(128.dp).padding(horizontal = PleosSize.space48),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PlainIconButton(Glyph.Close, "닫기", onClose)
        Text("드라이브클래스", style = PleosType.navMenu, color = c.textDarkPrimary, modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(Modifier.weight(1f))
        when (kind) {
            EndKind.Arrived, EndKind.ArrivedEarly -> EndChip("목적지 도착", null)
            EndKind.Stopped, EndKind.StoppedDone -> EndChip("주차 중", "목적지 전")
            else -> ArrivalChip(remainingSec, fromNavi)
        }
    }
}

@Composable
private fun EndChip(title: String, sub: String?) {
    val c = Pleos.colors
    Row(
        Modifier.height(80.dp).clip(RoundedCornerShape(50)).background(c.surfaceHigh).padding(start = 12.dp, end = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon3d(R.drawable.icon3d_arrival, 72.dp)
        Text(title, style = PleosType.titleSmall, color = c.textDarkPrimary)
        if (sub != null) Text(sub, style = PleosType.labelMedium, color = c.textDarkTertiary)
    }
}

/* ───────────── ①·③·④: 주차한 뒤의 요약(교육 목록 + 오늘의 정리) ───────────── */

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.Summary(
    kind: EndKind,
    p: CourseProgress,
    recall: List<RecallStore.Item>,
    driveMin: Int,
    onClose: () -> Unit,
    onOpenRecap: () -> Unit,
    onResumeNow: () -> Unit,
) {
    val c = Pleos.colors
    val blocks = p.blocks
    val partial = !p.finished
    PleosCard(Modifier.fillMaxWidth(), padding = PleosSize.space40) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PleosSize.space40)) {
            Icon3d(
                if (kind == EndKind.Stopped) R.drawable.icon3d_parking else R.drawable.icon3d_arrival,
                180.dp,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when (kind) {
                        EndKind.Stopped -> "여기까지 들었어요"
                        EndKind.StoppedDone -> "오늘 코스를 다 들었어요"
                        else -> "목적지에 도착했어요"
                    },
                    style = PleosType.headlineMedium,
                    color = c.textDarkPrimary,
                )
                Text(
                    when (kind) {
                        EndKind.Stopped -> "들은 자리는 저장해 뒀어요"
                        EndKind.ArrivedEarly -> "남은 블록은 다음에 이어 들어요"
                        EndKind.StoppedDone -> "목적지 전에 주차했어요"
                        else -> if (driveMin >= 1) "${driveMin}분 주행 동안 오늘 코스를 끝까지 들었어요" else "오늘 코스를 끝까지 들었어요"
                    },
                    style = PleosType.bodyMedium,
                    color = c.textDarkSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PleosSize.space48), modifier = Modifier.padding(end = 8.dp)) {
                StatText("${p.doneCount.coerceAtMost(blocks.size)} / ${blocks.size}", if (kind == EndKind.Stopped) "다 들은 블록" else "들은 블록")
                StatText(heardLabel(p.heardSec), "들은 시간")
                StatText("${recall.size}개", "모은 표현")
            }
        }
    }
    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
        val lessonList = rememberLazyListState()
        ListCard(
            icon = R.drawable.icon3d_speech,
            title = "오늘의 교육",
            meta = null,
            // 코스가 남았을 때만 이어 들을 자리를 알려 준다.
            desc = if (partial) "다음 주행에서 ${p.doneCount + 1}번 블록 ${clock(p.partialSec)}부터 이어 들어요" else null,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            LazyColumn(state = lessonList, modifier = Modifier.fillMaxSize().scrollbar(lessonList, c.alphaDark200)) {
                itemsIndexed(blocks, key = { _, b -> b.id }) { i, b ->
                    val state = when {
                        i < p.doneCount -> LessonState.Done
                        i == p.doneCount -> LessonState.Partial
                        else -> LessonState.Left
                    }
                    LessonRow(b, state, if (state == LessonState.Partial) p.partialSec else 0, showDoneLabel = partial, divider = i > 0)
                }
            }
        }
        val exprList = rememberLazyListState()
        // 문장 옆 재생 버튼: 원문 → 번역문 순서로 들려준다. 화면을 닫으면 멈춘다.
        val sentences = rememberSentencePlayer()
        ListCard(
            icon = R.drawable.icon3d_notebook,
            title = "오늘의 정리",
            meta = null,
            desc = if (recall.isEmpty()) "이번 주행에서는 아직 모은 표현이 없어요" else null,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            LazyColumn(state = exprList, modifier = Modifier.fillMaxSize().scrollbar(exprList, c.alphaDark200)) {
                itemsIndexed(recall, key = { _, it -> it.key }) { i, item ->
                    ExprRow(item, divider = i > 0, playing = sentences.playingKey == item.key) { sentences.toggle(item) }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
        Spacer(Modifier.weight(1f))
        if (kind == EndKind.Stopped) {
            BasicButton("오늘은 그만 듣기", onClose, large = true)
            BasicButton("지금 이어 듣기", onResumeNow, Modifier.widthIn(min = 360.dp), secondary = true, large = true)
        } else {
            BasicButton("닫기", onClose, large = true)
            BasicButton("오늘의 정리 모두 보기", onOpenRecap, Modifier.widthIn(min = 360.dp), secondary = true, large = true)
        }
    }
}

@Composable
private fun StatText(value: String, label: String) {
    val c = Pleos.colors
    Column(Modifier.widthIn(min = 150.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = PleosType.headlineSmall.copy(fontFeatureSettings = "tnum"), color = c.textDarkPrimary)
        Text(label, style = PleosType.bodySmall, color = c.textDarkSecondary)
    }
}

private enum class LessonState { Done, Partial, Left }

/** 오늘의 교육 한 줄: 상태 표시(체크·들은 비율·빈 원), 블록 이름, 오른쪽 정보. */
@Composable
private fun LessonRow(b: Block, state: LessonState, partialSec: Int, showDoneLabel: Boolean, divider: Boolean) {
    val c = Pleos.colors
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    LessonState.Done -> GlyphIcon(Glyph.Check, c.informativePositive, size = 32.dp)
                    LessonState.Partial -> {
                        val track = c.controllerSliderNormal
                        val fill = c.informativePositive
                        val f = partialSec.toFloat() / b.durationSec
                        Canvas(Modifier.size(20.dp)) {
                            drawCircle(track)
                            drawArc(fill, -90f, 360f * f, useCenter = true)
                        }
                    }
                    LessonState.Left -> Box(Modifier.size(20.dp).border(3.dp, c.controllerSliderNormal, CircleShape))
                }
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    b.title,
                    style = PleosType.bodyLarge,
                    color = if (state == LessonState.Left) c.textDarkTertiary else c.textDarkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state == LessonState.Partial) Text("여기서 이어 듣기", style = PleosType.labelMediumStrong, color = c.informativePositive)
            }
            Text(
                when {
                    state == LessonState.Partial -> "${clock(partialSec)} / ${clock(b.durationSec)}"
                    state == LessonState.Done && showDoneLabel -> "다 들음"
                    else -> "${kindLabel(b)} · ${(b.durationSec + 59) / 60}분"
                },
                style = PleosType.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = c.textDarkTertiary,
            )
        }
    }
}

@Composable
private fun ExprRow(item: RecallStore.Item, divider: Boolean, playing: Boolean, onToggle: () -> Unit) {
    val c = Pleos.colors
    Column {
        if (divider) Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.en, style = PleosType.bodyLargeStrong, color = if (playing) c.informativePositive else c.textDarkPrimary)
                Text(item.ko, style = PleosType.bodySmall, color = c.textDarkSecondary)
            }
            SentencePlayButton(playing, onToggle)
        }
    }
}

/** 문장 재생/일시정지 버튼(72, 원형). 재생 중에는 filled 배경과 일시정지 아이콘. 종료 화면과 오늘의 정리 화면이 함께 쓴다. */
@Composable
internal fun SentencePlayButton(playing: Boolean, onToggle: () -> Unit) {
    val c = Pleos.colors
    Pressable(
        onClick = onToggle,
        modifier = Modifier.size(72.dp).semantics { contentDescription = if (playing) "문장 일시정지" else "문장 재생" },
        shape = CircleShape,
        color = if (playing) c.buttonFilledEnabled else c.buttonBasicEnabled,
        pressedColor = if (playing) c.buttonFilledPressed else c.buttonBasicPressed,
    ) {
        GlyphIcon(if (playing) Glyph.Pause else Glyph.Play, if (playing) c.textLightPrimary else c.iconDarkPrimary, size = 32.dp)
    }
}

/* ───────────── ②·⑥: 주행 중 코스 완료(글 목록 없이 큰 글씨와 버튼) ───────────── */

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DoneWhileDriving(
    kind: EndKind,
    p: CourseProgress,
    recallCount: Int,
    remainingSec: Int,
    reviewMin: Int,
    onQuiet: () -> Unit,
    onReview: () -> Unit,
    onNextEpisode: () -> Unit,
) {
    val c = Pleos.colors
    // ⑥은 고르지 않으면 잠시 뒤 조용히 가기로 넘어간다(운전 중 조작을 강요하지 않게).
    if (kind == EndKind.DoneTimeLeft) {
        LaunchedEffect(Unit) {
            delay(QUIET_AUTO_SEC * 1000L)
            onQuiet()
        }
    }
    PleosCard(Modifier.fillMaxWidth().weight(1f), padding = PleosSize.space48) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PleosSize.space40, Alignment.CenterVertically),
        ) {
            Icon3d(R.drawable.icon3d_headphones, 220.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("오늘 코스를 다 들었어요", style = PleosType.headlineLarge, color = c.textDarkPrimary, textAlign = TextAlign.Center)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                StatTile(StatIcon.Blocks, "들은 블록", "${p.doneCount.coerceAtMost(p.ids.size)} / ${p.ids.size}")
                StatTile(StatIcon.Time, "들은 시간", heardLabel(p.heardSec))
                StatTile(StatIcon.Notes, "모은 표현", "${recallCount}개")
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24, Alignment.CenterHorizontally)) {
        if (kind == EndKind.DoneNearArrival) {
            BasicButton("남은 ${(remainingSec / 60).coerceAtLeast(1)}분 동안 복습 듣기", onReview, Modifier.widthIn(min = 420.dp), large = true)
            BasicButton("조용히 가기", onQuiet, Modifier.widthIn(min = 420.dp), secondary = true, large = true)
        } else {
            BasicButton("조용히 가기", onQuiet, Modifier.widthIn(min = 320.dp), large = true)
            BasicButton("복습 듣기 · ${reviewMin}분", onReview, Modifier.widthIn(min = 380.dp), large = true)
            BasicButton("다음 회차 · ${NEXT_EPISODE.no}회 ${NEXT_EPISODE.title}", onNextEpisode, Modifier.widthIn(min = 520.dp), secondary = true, large = true)
        }
    }
}

private enum class StatIcon { Blocks, Time, Notes }

/** 숫자 카드: 위에 아이콘과 이름, 아래에 큰 숫자. surface_low 바탕에 얇은 테두리. */
@Composable
private fun StatTile(icon: StatIcon, label: String, value: String) {
    val c = Pleos.colors
    val shape = RoundedCornerShape(PleosSize.radius32)
    Column(
        Modifier.width(300.dp).clip(shape).background(c.surfaceLow).border(2.dp, c.alphaDark100, shape).padding(PleosSize.space40),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatGlyph(icon, c.informativePositive)
            Text(label, style = PleosType.bodyMedium, color = c.textDarkSecondary)
        }
        Text(value, style = PleosType.headlineMedium.copy(fontFeatureSettings = "tnum"), color = c.textDarkPrimary)
    }
}

@Composable
private fun StatGlyph(icon: StatIcon, color: Color) {
    if (icon == StatIcon.Blocks) {
        GlyphIcon(Glyph.Check, color, size = 36.dp)
        return
    }
    Canvas(Modifier.size(36.dp)) {
        val u = size.width / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round)
        if (icon == StatIcon.Time) {
            drawCircle(color, radius = 8.5f * u, style = stroke)
            drawLine(color, Offset(12 * u, 7.5f * u), Offset(12 * u, 12 * u), 2f * u, StrokeCap.Round)
            drawLine(color, Offset(12 * u, 12 * u), Offset(15 * u, 14 * u), 2f * u, StrokeCap.Round)
        } else {
            drawRoundRect(color, Offset(6 * u, 4.5f * u), Size(13 * u, 15 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = stroke)
            drawLine(color, Offset(9 * u, 11 * u), Offset(16 * u, 11 * u), 2f * u, StrokeCap.Round)
            drawLine(color, Offset(9 * u, 14.5f * u), Offset(14 * u, 14.5f * u), 2f * u, StrokeCap.Round)
        }
    }
}

/* ───────────── ⑤: 다시 출발할 때 이어 듣기 팝업 ───────────── */

@Composable
fun ResumePopup(
    p: CourseProgress,
    remainingSec: Int,
    onResume: () -> Unit,
    onNewCourse: () -> Unit,
) {
    val c = Pleos.colors
    var left by remember { mutableIntStateOf(RESUME_AUTO_SEC) }
    // 기다리는 동안 남은 시간이 바뀌어도 가장 최근 값으로 이어 듣도록 최신 콜백을 쓴다.
    val resume by rememberUpdatedState(onResume)
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1000)
            left--
        }
        resume()
    }
    Box(
        Modifier.fillMaxSize().background(c.overlayDefault)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(808.dp).clip(RoundedCornerShape(PleosSize.radius48)).background(c.backgroundPopup).padding(PleosSize.space48),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon3d(R.drawable.icon3d_headphones, 140.dp)
            Text("지난번 멈춘 곳부터 이어 들을까요?", style = PleosType.titleMedium, color = c.textDarkPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PleosSize.space24)) {
                BasicButton("새 코스로 시작", onNewCourse, Modifier.weight(1f), large = true)
                BasicButton("이어 듣기 · ${left}초", onResume, Modifier.weight(1f), secondary = true, large = true)
            }
        }
    }
}

/** 들은 시간 표기: 1분이 안 되게 들었으면 "0분" 대신 "1분 미만". */
internal fun heardLabel(sec: Int): String = if (sec in 1..29) "1분 미만" else "${(sec + 30) / 60}분"
