package com.driveclass.app

/**
 * 상단 강의 선택 영역의 목록. 지금 들을 수 있는 건 '영어 회의 표현' 3회(SampleCatalog 블록)뿐이고,
 * 나머지 종목·회차는 화면 구성을 보여 주기 위한 샘플 목록이다.
 */
data class Episode(val no: Int, val title: String, val minutes: Int)

data class Subject(val name: String, val episodes: List<Episode>)

object CourseCatalog {
    val subjects = listOf(
        Subject(
            "영어 회의 표현",
            listOf(
                Episode(1, "자기소개와 인사", 12),
                Episode(2, "회의 안건 말하기", 13),
                Episode(3, "회의 시작부터 일정 조율까지", 14),
                Episode(4, "반대 의견 부드럽게 말하기", 15),
                Episode(5, "숫자와 그래프 설명하기", 14),
                Episode(6, "회의 마무리와 후속 메일", 12),
            ),
        ),
        Subject(
            "비즈니스 이메일",
            listOf(
                Episode(1, "첫 연락 메일", 10),
                Episode(2, "요청과 독촉", 11),
                Episode(3, "사과와 해명", 12),
                Episode(4, "일정 변경 알리기", 10),
            ),
        ),
        Subject(
            "여행 일본어",
            listOf(
                Episode(1, "공항과 입국", 9),
                Episode(2, "호텔 체크인", 10),
                Episode(3, "식당에서 주문하기", 11),
                Episode(4, "길 묻기", 9),
                Episode(5, "쇼핑과 계산", 10),
            ),
        ),
        Subject(
            "경제 뉴스 읽기",
            listOf(
                Episode(1, "금리가 오르면 생기는 일", 15),
                Episode(2, "환율 기사 읽는 법", 14),
                Episode(3, "실적 발표 이해하기", 16),
            ),
        ),
        Subject(
            "세계사 한 토막",
            listOf(
                Episode(1, "실크로드의 상인들", 18),
                Episode(2, "대항해 시대", 20),
                Episode(3, "산업혁명과 철도", 19),
                Episode(4, "냉전의 시작", 21),
            ),
        ),
    )

    /** 실제로 들을 수 있는 강의: 종목 0번의 3회. */
    const val PLAYABLE_SUBJECT = 0
    const val PLAYABLE_EPISODE = 3

    fun isPlayable(subject: Int, episodeNo: Int) = subject == PLAYABLE_SUBJECT && episodeNo == PLAYABLE_EPISODE
}
