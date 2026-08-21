package com.niben.app.data

/**
 * (히라가나, 가타카나, 로마자) 순서쌍. 청음 46 + 탁음 20 + 반탁음 5 + 요음 33 = 104개.
 * 한 번 정의한 발음표를 히라가나/가타카나 양쪽 ContentItem 생성에 재사용한다.
 */
private val KANA_CHART: List<Triple<String, String, String>> = listOf(
    // 청음 (46)
    Triple("あ", "ア", "a"), Triple("い", "イ", "i"), Triple("う", "ウ", "u"), Triple("え", "エ", "e"), Triple("お", "オ", "o"),
    Triple("か", "カ", "ka"), Triple("き", "キ", "ki"), Triple("く", "ク", "ku"), Triple("け", "ケ", "ke"), Triple("こ", "コ", "ko"),
    Triple("さ", "サ", "sa"), Triple("し", "シ", "shi"), Triple("す", "ス", "su"), Triple("せ", "セ", "se"), Triple("そ", "ソ", "so"),
    Triple("た", "タ", "ta"), Triple("ち", "チ", "chi"), Triple("つ", "ツ", "tsu"), Triple("て", "テ", "te"), Triple("と", "ト", "to"),
    Triple("な", "ナ", "na"), Triple("に", "ニ", "ni"), Triple("ぬ", "ヌ", "nu"), Triple("ね", "ネ", "ne"), Triple("の", "ノ", "no"),
    Triple("は", "ハ", "ha"), Triple("ひ", "ヒ", "hi"), Triple("ふ", "フ", "fu"), Triple("へ", "ヘ", "he"), Triple("ほ", "ホ", "ho"),
    Triple("ま", "マ", "ma"), Triple("み", "ミ", "mi"), Triple("む", "ム", "mu"), Triple("め", "メ", "me"), Triple("も", "モ", "mo"),
    Triple("や", "ヤ", "ya"), Triple("ゆ", "ユ", "yu"), Triple("よ", "ヨ", "yo"),
    Triple("ら", "ラ", "ra"), Triple("り", "リ", "ri"), Triple("る", "ル", "ru"), Triple("れ", "レ", "re"), Triple("ろ", "ロ", "ro"),
    Triple("わ", "ワ", "wa"), Triple("を", "ヲ", "wo"),
    Triple("ん", "ン", "n"),
    // 탁음 (20)
    Triple("が", "ガ", "ga"), Triple("ぎ", "ギ", "gi"), Triple("ぐ", "グ", "gu"), Triple("げ", "ゲ", "ge"), Triple("ご", "ゴ", "go"),
    Triple("ざ", "ザ", "za"), Triple("じ", "ジ", "ji"), Triple("ず", "ズ", "zu"), Triple("ぜ", "ゼ", "ze"), Triple("ぞ", "ゾ", "zo"),
    Triple("だ", "ダ", "da"), Triple("ぢ", "ヂ", "ji"), Triple("づ", "ヅ", "zu"), Triple("で", "デ", "de"), Triple("ど", "ド", "do"),
    Triple("ば", "バ", "ba"), Triple("び", "ビ", "bi"), Triple("ぶ", "ブ", "bu"), Triple("べ", "ベ", "be"), Triple("ぼ", "ボ", "bo"),
    // 반탁음 (5)
    Triple("ぱ", "パ", "pa"), Triple("ぴ", "ピ", "pi"), Triple("ぷ", "プ", "pu"), Triple("ぺ", "ペ", "pe"), Triple("ぽ", "ポ", "po"),
    // 요음 (33)
    Triple("きゃ", "キャ", "kya"), Triple("きゅ", "キュ", "kyu"), Triple("きょ", "キョ", "kyo"),
    Triple("しゃ", "シャ", "sha"), Triple("しゅ", "シュ", "shu"), Triple("しょ", "ショ", "sho"),
    Triple("ちゃ", "チャ", "cha"), Triple("ちゅ", "チュ", "chu"), Triple("ちょ", "チョ", "cho"),
    Triple("にゃ", "ニャ", "nya"), Triple("にゅ", "ニュ", "nyu"), Triple("にょ", "ニョ", "nyo"),
    Triple("ひゃ", "ヒャ", "hya"), Triple("ひゅ", "ヒュ", "hyu"), Triple("ひょ", "ヒョ", "hyo"),
    Triple("みゃ", "ミャ", "mya"), Triple("みゅ", "ミュ", "myu"), Triple("みょ", "ミョ", "myo"),
    Triple("りゃ", "リャ", "rya"), Triple("りゅ", "リュ", "ryu"), Triple("りょ", "リョ", "ryo"),
    Triple("ぎゃ", "ギャ", "gya"), Triple("ぎゅ", "ギュ", "gyu"), Triple("ぎょ", "ギョ", "gyo"),
    Triple("じゃ", "ジャ", "ja"), Triple("じゅ", "ジュ", "ju"), Triple("じょ", "ジョ", "jo"),
    Triple("びゃ", "ビャ", "bya"), Triple("びゅ", "ビュ", "byu"), Triple("びょ", "ビョ", "byo"),
    Triple("ぴゃ", "ピャ", "pya"), Triple("ぴゅ", "ピュ", "pyu"), Triple("ぴょ", "ピョ", "pyo"),
)

object KanaTable {
    fun hiraganaItems(): List<ContentItem> = KANA_CHART.map { (hira, _, romaji) ->
        ContentItem(
            category = ContentCategory.HIRAGANA,
            japaneseText = hira,
            reading = romaji,
            meaningKo = romaji,
            level = 1,
            source = "hardcoded (고정 문자표)"
        )
    }

    fun katakanaItems(): List<ContentItem> = KANA_CHART.map { (_, kata, romaji) ->
        ContentItem(
            category = ContentCategory.KATAKANA,
            japaneseText = kata,
            reading = romaji,
            meaningKo = romaji,
            level = 1,
            source = "hardcoded (고정 문자표)"
        )
    }
}
