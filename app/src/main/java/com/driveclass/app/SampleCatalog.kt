package com.driveclass.app

import android.content.Context
import com.driveclass.engine.Block
import com.driveclass.engine.BlockKind
import com.driveclass.engine.Catalog

/**
 * 블록 정의의 단일 출처(영어 회화). 오디오는 assets/blocks/<id>.mp3 에 있다.
 * durationSec는 실제 mp3 길이(초, 올림)다. 오디오를 바꾸면 여기 [BLOCKS]만 고친다.
 */
object SampleCatalog {
    private val BLOCKS = listOf(
        Block("intro", BlockKind.INTRO, "오늘 배울 표현", 44),
        Block("core1", BlockKind.CORE, "회의 시작하기", 151),
        Block("core2", BlockKind.CORE, "의견 묻고 답하기", 260),
        Block("core3", BlockKind.CORE, "일정 조율하기", 171),
        Block("review1", BlockKind.REVIEW, "따라 말하기 복습", 190),
        Block("closing", BlockKind.CLOSING, "오늘의 한 문장", 45),
    )

    /** 정차 화면의 정리 카드에 보여 줄 핵심 문장(영어, 뜻). */
    val keySentences: Map<String, List<Pair<String, String>>> = mapOf(
        "intro" to listOf("Let's get started." to "시작하겠습니다."),
        "core1" to listOf(
            "Shall we get started?" to "시작할까요?",
            "We're here to talk about the budget." to "예산 이야기를 하려고 모였습니다.",
            "Let's keep it short." to "짧게 하죠.",
        ),
        "core2" to listOf(
            "What do you think about this plan?" to "이 계획 어떻게 생각하세요?",
            "I agree with you on that." to "그 점은 동의합니다.",
            "I see your point, but I have a concern." to "무슨 말씀인지 알겠습니다만, 걱정되는 점이 있습니다.",
            "Let me think about it and get back to you." to "생각해 보고 다시 말씀드리겠습니다.",
        ),
        "core3" to listOf(
            "Are you available on Thursday?" to "목요일에 시간 되세요?",
            "I'm afraid I can't make it." to "죄송하지만 어렵겠습니다.",
            "It's due by Friday." to "금요일까지입니다.",
        ),
        "closing" to listOf("Let's pick this up tomorrow." to "내일 이어서 하죠."),
    )

    val orderedIds: List<String> get() = BLOCKS.map { it.id }

    private val byId = BLOCKS.associateBy { it.id }

    fun block(id: String): Block? = byId[id]

    fun assetUri(id: String) = "asset:///blocks/$id.mp3"

    /** assets/blocks 에 실제로 들어 있는 블록 id. */
    fun availableIds(context: Context): Set<String> =
        context.assets.list("blocks").orEmpty()
            .filter { it.endsWith(".mp3") }
            .map { it.removeSuffix(".mp3") }
            .filter { it in byId }
            .toSet()

    /**
     * 오디오가 있는 블록만 담은 카탈로그. 마무리 블록은 Catalog가 null을 허용하지 않아
     * 파일이 없어도 담고, 큐를 만들 때 건너뛴다.
     */
    fun catalog(available: Set<String>): Catalog {
        fun ok(b: Block) = b.id in available
        return Catalog(
            intro = BLOCKS.firstOrNull { it.kind == BlockKind.INTRO && ok(it) },
            cores = BLOCKS.filter { it.kind == BlockKind.CORE && ok(it) },
            reviews = BLOCKS.filter { it.kind == BlockKind.REVIEW && ok(it) },
            closing = BLOCKS.first { it.kind == BlockKind.CLOSING },
        )
    }
}
