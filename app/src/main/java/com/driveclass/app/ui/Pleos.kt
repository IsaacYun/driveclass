package com.driveclass.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveclass.app.R

/**
 * Pleos Connect 디자인 시스템을 Compose로 옮긴 것.
 * 색·글자 단계·반경·그림자는 디자인 시스템 tokens.json, 컴포넌트 규격은 components/bundle.css 값을 그대로 쓴다.
 * 이름은 토큰 이름을 camelCase로 바꾼 것이다.
 */
data class PleosColors(
    val backgroundPrimary: Color,
    val backgroundPopup: Color,
    val surfaceBasic: Color,
    val surfaceLow: Color,
    val surfaceHigh: Color,
    val textDarkPrimary: Color,
    val textDarkSecondary: Color,
    val textDarkTertiary: Color,
    val textDarkQuaternary: Color,
    val textLightPrimary: Color,
    val iconDarkPrimary: Color,
    val buttonBasicEnabled: Color,
    val buttonBasicPressed: Color,
    val buttonFilledEnabled: Color,
    val buttonFilledPressed: Color,
    val alphaDark100: Color,
    val overlayDefault: Color,
    val informativePositive: Color,
    val controllerTabBackground: Color,
    val controllerSliderNormal: Color,
    val controllerSliderPressed: Color,
    val alphaDark200: Color,
    val controllerSliderKnob: Color,
    val switchOff: Color,
    val switchKnobOff: Color,
    val switchKnobOn: Color,
)

private val Light = PleosColors(
    backgroundPrimary = Color(0xFFF7F8FA), // basic_50
    backgroundPopup = Color(0xFFFFFFFF), // basic_00
    surfaceBasic = Color(0xFFFFFFFF), // basic_00
    surfaceLow = Color(0xFFF7F8FA), // basic_50
    surfaceHigh = Color(0xFFEDEEF2), // basic_100
    textDarkPrimary = Color(0xFF131417), // basic_static_dark_100
    textDarkSecondary = Color(0xD6131417), // 84%
    textDarkTertiary = Color(0xA3131417), // 64%
    textDarkQuaternary = Color(0x52131417), // 32%
    textLightPrimary = Color(0xFFFFFFFF),
    iconDarkPrimary = Color(0xFF131417),
    buttonBasicEnabled = Color(0x0D131417), // basic_alpha_dark_50
    buttonBasicPressed = Color(0x1A131417), // basic_alpha_dark_100
    buttonFilledEnabled = Color(0xFF44464E), // basic_600
    buttonFilledPressed = Color(0xFF313236), // basic_700
    alphaDark100 = Color(0x1A131417),
    overlayDefault = Color(0x33131417), // basic_alpha_dark_200
    informativePositive = Color(0xFF0064FF),
    controllerTabBackground = Color(0x0D131417), // basic_alpha_dark_50
    controllerSliderNormal = Color(0x33131417), // basic_alpha_dark_200
    controllerSliderPressed = Color(0x4D131417), // basic_alpha_dark_300
    alphaDark200 = Color(0x33131417),
    controllerSliderKnob = Color(0xFFFFFFFF),
    switchOff = Color(0x4D131417), // basic_alpha_dark_300
    switchKnobOff = Color(0xFFFFFFFF),
    switchKnobOn = Color(0xFFFFFFFF),
)

private val Dark = PleosColors(
    backgroundPrimary = Color(0xFF1D1E21),
    backgroundPopup = Color(0xFF313236),
    surfaceBasic = Color(0xFF1D1E21),
    surfaceLow = Color(0xFF131417),
    surfaceHigh = Color(0xFF313236),
    textDarkPrimary = Color(0xFFFFFFFF),
    textDarkSecondary = Color(0xD6FFFFFF),
    textDarkTertiary = Color(0xA3FFFFFF),
    textDarkQuaternary = Color(0x52FFFFFF),
    textLightPrimary = Color(0xFF131417),
    iconDarkPrimary = Color(0xFFFFFFFF),
    buttonBasicEnabled = Color(0x0DFFFFFF),
    buttonBasicPressed = Color(0x1AFFFFFF),
    buttonFilledEnabled = Color(0xFFE0E1E6),
    buttonFilledPressed = Color(0xFFC7C9CE),
    alphaDark100 = Color(0x1AFFFFFF),
    overlayDefault = Color(0x80131417),
    informativePositive = Color(0xFF0082FF),
    controllerTabBackground = Color(0x0DFFFFFF),
    controllerSliderNormal = Color(0x1AFFFFFF),
    controllerSliderPressed = Color(0x33FFFFFF),
    alphaDark200 = Color(0x33FFFFFF),
    controllerSliderKnob = Color(0xFFE0E1E6),
    switchOff = Color(0x1AFFFFFF),
    switchKnobOff = Color(0xFFA4A8AE), // basic_400
    switchKnobOn = Color(0xFFFFFFFF),
)

/** 디자인 시스템 서체 Asta Sans(42dot, OFL). 라이선스는 assets/licenses-asta-sans-OFL.txt. */
val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_bold, FontWeight.Bold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold),
)

/** Headline 60/56/48 › Title 40/36/32 › Body 30/28/26 › Label 24/20. mdpi 차량 화면이라 sp = px. */
object PleosType {
    private fun s(size: Int, line: Float, weight: FontWeight) =
        TextStyle(fontFamily = AstaSans, fontSize = size.sp, lineHeight = (size * line).sp, fontWeight = weight)

    val headlineLarge = s(60, 1.25f, FontWeight.Bold)
    val headlineMedium = s(56, 1.25f, FontWeight.Bold)
    val headlineSmall = s(48, 1.25f, FontWeight.Bold)
    val titleMedium = s(36, 1.3f, FontWeight.Bold)
    val titleSmall = s(32, 1.3f, FontWeight.Bold)
    val bodyLarge = s(30, 1.5f, FontWeight.Normal)
    val bodyLargeStrong = s(30, 1.5f, FontWeight.Bold)
    val bodyMedium = s(28, 1.5f, FontWeight.Normal)
    val bodyMediumStrong = s(28, 1.5f, FontWeight.Bold)
    val bodySmall = s(26, 1.5f, FontWeight.Normal)
    val labelMedium = s(24, 1.5f, FontWeight.Normal)
    val labelMediumStrong = s(24, 1.5f, FontWeight.Bold)
    val navMenu = s(32, 1.3f, FontWeight.Bold) // pl-nav__menu
    val navMenuSub = s(28, 1.3f, FontWeight.Bold) // pl-nav__menu--sub
}

/** spacing·radius 토큰. */
object PleosSize {
    val space24 = 24.dp
    val space40 = 40.dp
    val space48 = 48.dp
    val radius8 = 8.dp
    val radius16 = 16.dp
    val radius24 = 24.dp
    val radius32 = 32.dp
    val radius48 = 48.dp
    val buttonHeightLarge = 100.dp
    val buttonHeightMedium = 80.dp
}

val LocalPleos = staticCompositionLocalOf { Light }

object Pleos {
    val colors: PleosColors @Composable get() = LocalPleos.current
}

@Composable
fun PleosTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) Dark else Light
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.informativePositive,
        background = c.backgroundPrimary,
        surface = c.backgroundPrimary,
        onSurface = c.textDarkPrimary,
    )
    CompositionLocalProvider(LocalPleos provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/** shadow_level_1(0 6 24 rgba(0,0,0,.08))에 가까운 Compose 그림자. */
private fun Modifier.level1(shape: Shape) =
    shadow(10.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))

/** 상호작용 규칙: 누르면 95%로 줄고(pressed_scale) pressed 배경으로 바뀐다. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    pressedColor: Color,
    enabled: Boolean = true,
    role: Role = Role.Button,
    content: @Composable (pressed: Boolean) -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, label = "pressed")
    Box(
        modifier
            .scale(s)
            .clip(shape)
            .background(if (pressed) pressedColor else color)
            .clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content(pressed) }
}

/**
 * BasicButton. Primary = button_basic_*, Secondary = button_filled_* + text_light_primary(한 화면에 하나).
 * Large: 높이 100, radius_32, 글자 30 / Medium: 높이 80, radius_24, 글자 28. 아이콘 간격 12.
 */
@Composable
fun BasicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    large: Boolean = false,
    icon: Glyph? = null,
) {
    val c = Pleos.colors
    val fg = if (secondary) c.textLightPrimary else c.textDarkPrimary
    Pressable(
        onClick = onClick,
        modifier = modifier.height(if (large) PleosSize.buttonHeightLarge else PleosSize.buttonHeightMedium),
        shape = RoundedCornerShape(if (large) PleosSize.radius32 else PleosSize.radius24),
        color = if (secondary) c.buttonFilledEnabled else c.buttonBasicEnabled,
        pressedColor = if (secondary) c.buttonFilledPressed else c.buttonBasicPressed,
    ) {
        Row(
            Modifier.padding(horizontal = if (large) 44.dp else 36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) GlyphIcon(icon, fg, size = 28.dp)
            Text(text, style = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = if (large) 30.sp else 28.sp), color = fg)
        }
    }
}

/** IconButton Plain: 80×80, radius_24, 배경 없음(눌리면 button_basic_enabled). 이름(contentDescription) 필수. */
@Composable
fun PlainIconButton(glyph: Glyph, label: String, onClick: () -> Unit) {
    val c = Pleos.colors
    Pressable(
        onClick = onClick,
        modifier = Modifier.size(80.dp).semantics { contentDescription = label },
        shape = RoundedCornerShape(PleosSize.radius24),
        color = Color.Transparent,
        pressedColor = c.buttonBasicEnabled,
    ) { GlyphIcon(glyph, c.iconDarkPrimary, size = 28.dp) }
}

/**
 * PageNavigation(pl-nav). Main: 앱 아이콘 + Primary/Secondary 메뉴 + Suffix.
 * Depth: 앱 아이콘 자리에 뒤로 버튼([onBack]).
 */
@Composable
fun PageNavigation(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    suffix: @Composable RowScope.() -> Unit = {},
) {
    val c = Pleos.colors
    Row(
        Modifier.fillMaxWidth().height(128.dp).padding(horizontal = PleosSize.space48),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (onBack != null) {
            PlainIconButton(Glyph.Back, "뒤로", onBack)
        } else {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8F1FF)).scale(1.5f),
            )
        }
        Text(title, style = PleosType.navMenu, color = c.textDarkPrimary, modifier = Modifier.padding(horizontal = 12.dp))
        if (subtitle != null) Text(subtitle, style = PleosType.navMenuSub, color = c.textDarkTertiary)
        Spacer(Modifier.weight(1f))
        suffix()
    }
}

/** 카드: surface_basic, radius_48, shadow_level_1. 디자인 시스템에 카드 컴포넌트는 없고 Widget의 시각 언어를 따른다. */
@Composable
fun PleosCard(
    modifier: Modifier = Modifier,
    padding: Dp = PleosSize.space48,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pleos.colors
    val shape = RoundedCornerShape(PleosSize.radius48)
    if (onClick != null) {
        Pressable(onClick, modifier.level1(shape), shape, c.surfaceBasic, c.surfaceBasic) {
            Column(Modifier.fillMaxWidth().padding(padding), content = content)
        }
    } else {
        Column(modifier.level1(shape).clip(shape).background(c.surfaceBasic).padding(padding), content = content)
    }
}

/** Switch: 80×48 트랙(radius_16), 32×32 핸들(radius_8). 켜짐 트랙은 switch_on 토큰이 시스템에 없어 informative_positive를 쓴다. */
@Composable
fun PleosSwitch(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val c = Pleos.colors
    Pressable(
        onClick = { onChange(!checked) },
        modifier = Modifier.width(80.dp).height(48.dp).semantics { contentDescription = label },
        shape = RoundedCornerShape(PleosSize.radius16),
        color = if (checked) c.informativePositive else c.switchOff,
        pressedColor = if (checked) c.informativePositive else c.switchOff,
        role = Role.Switch,
    ) {
        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(
                Modifier.size(32.dp).level1(RoundedCornerShape(PleosSize.radius8))
                    .clip(RoundedCornerShape(PleosSize.radius8))
                    .background(if (checked) c.switchKnobOn else c.switchKnobOff),
            )
        }
    }
}

/**
 * Continuous slider(Basic). 트랙 controller_slider_normal(높이 64, radius_24),
 * 채움 controller_slider_knob + shadow_level_1. 누르거나 끌어서 값을 바꾼다.
 */
@Composable
fun PleosSlider(value: Float, range: ClosedFloatingPointRange<Float>, label: String, onChange: (Float) -> Unit) {
    val c = Pleos.colors
    val shape = RoundedCornerShape(PleosSize.radius24)
    var widthPx by remember { androidx.compose.runtime.mutableIntStateOf(1) }
    fun set(x: Float) = onChange((range.start + (x / widthPx).coerceIn(0f, 1f) * (range.endInclusive - range.start)))
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Box(
        Modifier.fillMaxWidth().height(64.dp).clip(shape).background(c.controllerSliderNormal)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(range) {
                detectTapGestures { set(it.x) }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures { change, _ -> set(change.position.x) }
            }
            .semantics { contentDescription = label },
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).level1(shape).clip(shape).background(c.controllerSliderKnob))
        }
    }
}

/** 3D 아이콘(icon3d_*). 장식이라 접근성 이름은 붙이지 않는다. */
@Composable
fun Icon3d(res: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(res), contentDescription = null, modifier = modifier.size(size))
}

/** 칩 모양(radius_full) 배경. */
val PillShape = CircleShape

/** shadow_level_2(0 8 32 rgba(0,0,0,.16)): Dropdown·Toast. */
private fun Modifier.level2(shape: Shape) =
    shadow(16.dp, shape, ambientColor = Color(0x29000000), spotColor = Color(0x29000000))

/**
 * ToastPopup(pl-toast): 앱이 자기 결과를 잠깐 알릴 때 쓴다.
 * radius_full, 배경 button_filled_enabled, 글자 text_light_primary 26, 패딩 24·40, shadow_level_2,
 * 최대 너비 toast_max_width(792). 위치는 앱 패널 아래쪽 space_24 여백(호출하는 쪽에서 정렬).
 */
@Composable
fun ToastPopup(text: String, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Box(
        modifier
            .widthIn(max = 792.dp)
            .level2(PillShape)
            .clip(PillShape)
            .background(c.buttonFilledEnabled)
            .padding(horizontal = 40.dp, vertical = 24.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        // 한국어는 어절 단위로 줄을 바꾼다(API 33+). 그 아래에서는 기본 줄바꿈.
        Text(
            text,
            style = PleosType.bodySmall.copy(
                lineBreak = LineBreak(LineBreak.Strategy.Balanced, LineBreak.Strictness.Strict, LineBreak.WordBreak.Phrase),
            ),
            color = c.textLightPrimary,
        )
    }
}

/**
 * Tabs(pl-tabs). Box: 컨테이너 controller_tab_background, 패딩 6, 간격 4, radius_24.
 * 탭은 높이 64, 좌우 32, radius 18, 글자 26 Bold. 선택된 탭은 surface_basic + shadow_level_1.
 * Line: 배경 없이 아래 2px 선(basic_alpha_dark_100), 선택된 탭은 아래 4px text_dark_primary.
 * 항목이 많으면 가로로 넘긴다. [marked]에 든 탭은 라벨 뒤에 informative_positive 점을 붙인다.
 */
@Composable
fun PleosTabs(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    line: Boolean = false,
    marked: Set<Int> = emptySet(),
) {
    val c = Pleos.colors
    val scroll = rememberScrollState()
    LaunchedEffect(selected, labels) {
        // 선택된 탭이 보이게 대략 그 위치로 넘긴다.
        if (labels.isNotEmpty()) scroll.animateScrollTo((scroll.maxValue * selected / labels.lastIndex.coerceAtLeast(1)))
    }
    val container = if (line) {
        modifier.drawBehind {
            val h = 2.dp.toPx()
            drawRect(c.alphaDark100, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
        }
    } else {
        modifier.clip(RoundedCornerShape(PleosSize.radius24)).background(c.controllerTabBackground).padding(6.dp)
    }
    Row(
        container.horizontalScroll(scroll).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(if (line) 8.dp else 4.dp),
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selected
            val shape = RoundedCornerShape(if (line) 0.dp else 18.dp)
            val base = Modifier.height(64.dp)
            val look = when {
                line && on -> base.drawBehind {
                    val h = 4.dp.toPx()
                    drawRect(c.textDarkPrimary, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                }
                on -> base.level1(shape).clip(shape).background(c.surfaceBasic)
                else -> base.clip(shape)
            }
            Row(
                look.selectable(on, role = Role.Tab, onClick = { onSelect(i) }).padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    label,
                    style = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 26.sp),
                    color = if (on) c.textDarkPrimary else c.textDarkTertiary,
                )
                if (i in marked) Box(Modifier.size(10.dp).clip(CircleShape).background(c.informativePositive))
            }
        }
    }
}

/**
 * IconButton Basic Large: 100×100, radius_32, button_basic_enabled(눌리면 pressed).
 * 비활성은 아이콘을 text_dark_quaternary로. 이름(contentDescription) 필수.
 */
@Composable
fun LargeIconButton(label: String, onClick: () -> Unit, enabled: Boolean = true, content: @Composable (Color) -> Unit) {
    val c = Pleos.colors
    Pressable(
        onClick = onClick,
        modifier = Modifier.size(PleosSize.buttonHeightLarge).semantics { contentDescription = label },
        shape = RoundedCornerShape(PleosSize.radius32),
        color = c.buttonBasicEnabled,
        pressedColor = c.buttonBasicPressed,
        enabled = enabled,
    ) { content(if (enabled) c.iconDarkPrimary else c.textDarkQuaternary) }
}

/** 초 단위로 앞뒤로 가는 아이콘(원호 화살표 + 숫자). */
@Composable
fun SkipGlyph(seconds: Int, forward: Boolean, color: Color) {
    Box(contentAlignment = Alignment.Center) {
        GlyphIcon(if (forward) Glyph.Forward else Glyph.Rewind, color, size = 56.dp)
        Text(
            "$seconds",
            style = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 17.sp),
            color = color,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
