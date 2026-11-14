package com.driveclass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.driveclass.app.trip.TripStates
import com.driveclass.app.ui.Icon3d
import com.driveclass.app.ui.PageNavigation
import com.driveclass.app.ui.Pleos
import com.driveclass.app.ui.PleosCard
import com.driveclass.app.ui.PleosSize
import com.driveclass.app.ui.PleosTheme
import com.driveclass.app.ui.PleosType

/**
 * 정차 전용 화면(시안 v2의 Parked): 오늘 들은 블록의 정리 카드.
 * 매니페스트에서 DO(distractionOptimized)를 선언하지 않아 주행 중에는 플랫폼이 막는다.
 * 앱에서도 주행 상태가 되면 스스로 닫는다(이중 잠금).
 */
class ParkedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 주행 중 블록을 들으며 모은 표현(RecallStore)을 블록 순서대로 묶는다.
            val keys by RecallStore.keys(this).collectAsState()
            val collected = RecallStore.items(keys.orEmpty())
            val cards = SampleCatalog.orderedIds.mapNotNull { id ->
                val mine = collected.filter { it.blockId == id }.sortedBy { it.index }
                if (mine.isEmpty()) null else SampleCatalog.block(id)?.let { b -> b to mine }
            }
            val sentenceCount = collected.size
            val trip by TripStates.injected.state.collectAsState()
            LaunchedEffect(trip.driving) { if (trip.driving) finish() }
            // 문장 옆 재생 버튼: 원문 → 번역문 순서로 들려준다. 화면을 떠나면 멈춘다.
            val sentences = rememberSentencePlayer()
            PleosTheme {
                val c = Pleos.colors
                Column(Modifier.fillMaxSize().background(c.backgroundPrimary)) {
                    PageNavigation(title = "오늘의 정리", onBack = { finish() })
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 640.dp),
                        contentPadding = PaddingValues(start = PleosSize.space48, end = PleosSize.space48, bottom = PleosSize.space48),
                        horizontalArrangement = Arrangement.spacedBy(PleosSize.space24),
                        verticalArrangement = Arrangement.spacedBy(PleosSize.space24),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PleosCard(Modifier.fillMaxWidth(), padding = PleosSize.space40) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PleosSize.space40)) {
                                    Icon3d(R.drawable.icon3d_notebook, 180.dp)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            if (cards.isEmpty()) "아직 모은 표현이 없어요" else "오늘 표현 ${sentenceCount}개를 모았어요",
                                            style = PleosType.headlineSmall,
                                            color = c.textDarkPrimary,
                                        )
                                        Text(
                                            if (cards.isEmpty()) "주행 중에 블록을 들으면 되새길 표현이 하나씩 모여요"
                                            else "블록 ${cards.size}개 · 영어 회의 표현",
                                            style = PleosType.bodyMedium,
                                            color = c.textDarkSecondary,
                                        )
                                    }
                                }
                            }
                        }
                        items(cards) { (block, got) ->
                            PleosCard(Modifier.fillMaxWidth(), padding = PleosSize.space40) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon3d(R.drawable.icon3d_speech, 88.dp)
                                    Text(block.title, style = PleosType.titleSmall, color = c.textDarkPrimary)
                                }
                                Spacer(Modifier.height(8.dp))
                                got.forEachIndexed { i, item ->
                                    val playing = sentences.playingKey == item.key
                                    Row(
                                        Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.en, style = PleosType.bodyLargeStrong, color = if (playing) c.informativePositive else c.textDarkPrimary)
                                            Spacer(Modifier.height(4.dp))
                                            Text(item.ko, style = PleosType.bodySmall, color = c.textDarkSecondary)
                                        }
                                        SentencePlayButton(playing) { sentences.toggle(item) }
                                    }
                                    if (i < got.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(c.alphaDark100))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
