package com.example.data

import java.util.Locale

/**
 * Pure Arabic normalization & English offline query matching (lemmatization + Levenshtein fuzzy).
 * Zero Android dependencies so it can be unit-tested instantaneously on JVM.
 */
object ArabicNormalizer {
    private val TASHKEEL_AND_TATWEEL = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u0640]")
    private val MULTI_SPACE = Regex("\\s+")

    fun containsArabic(text: String): Boolean {
        return text.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' }
    }

    /**
     * Strips Arabic diacritics (tashkeel) and tatweel, and unifies common hamza/ta-marbuta/ya variants:
     * أ إ آ ٱ -> ا, ة -> ه, ى -> ي, ؤ -> و, ئ -> ي.
     * Matches `normalize_arabic()` in `scripts/build_offline_dictionary_db.py` character-for-character.
     */
    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        val stripped = TASHKEEL_AND_TATWEEL.replace(input, "")
        val sb = StringBuilder(stripped.length)
        for (ch in stripped) {
            sb.append(
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                    'ة' -> 'ه'
                    'ى', 'ئ' -> 'ي'
                    'ؤ' -> 'و'
                    else -> ch
                }
            )
        }
        return MULTI_SPACE.replace(sb.toString(), " ").trim().lowercase(Locale.ROOT)
    }
}

object OfflineQueryMatcher {

    /** Top common irregular English verb forms, comparatives, and plurals -> base lemma. */
    private val IRREGULAR_LEMMAS: Map<String, String> = mapOf(
        // Irregular verbs
        "was" to "be", "were" to "be", "been" to "be", "being" to "be", "am" to "be", "is" to "be", "are" to "be",
        "became" to "become", "becoming" to "become",
        "began" to "begin", "begun" to "begin", "beginning" to "begin",
        "blew" to "blow", "blown" to "blow",
        "broke" to "break", "broken" to "break",
        "brought" to "bring", "built" to "build", "bought" to "buy",
        "caught" to "catch", "chose" to "choose", "chosen" to "choose",
        "came" to "come", "coming" to "come",
        "dealt" to "deal", "did" to "do", "done" to "do", "does" to "do",
        "drew" to "draw", "drawn" to "draw", "drank" to "drink", "drunk" to "drink",
        "drove" to "drive", "driven" to "drive", "ate" to "eat", "eaten" to "eat",
        "fell" to "fall", "fallen" to "fall", "fed" to "feed", "felt" to "feel",
        "fought" to "fight", "found" to "find", "flew" to "fly", "flown" to "fly",
        "forbade" to "forbid", "forbidden" to "forbid", "forgot" to "forget", "forgotten" to "forget",
        "forgave" to "forgive", "forgiven" to "forgive", "froze" to "freeze", "frozen" to "freeze",
        "got" to "get", "gotten" to "get", "getting" to "get",
        "gave" to "give", "given" to "give", "went" to "go", "gone" to "go", "goes" to "go",
        "grew" to "grow", "grown" to "grow", "hung" to "hang", "had" to "have", "has" to "have", "having" to "have",
        "heard" to "hear", "hid" to "hide", "hidden" to "hide", "held" to "hold",
        "kept" to "keep", "knew" to "know", "known" to "know",
        "laid" to "lay", "led" to "lead", "left" to "leave", "lent" to "lend",
        "lay" to "lie", "lain" to "lie", "lying" to "lie", "lit" to "light", "lost" to "lose",
        "made" to "make", "making" to "make", "meant" to "mean", "met" to "meet",
        "paid" to "pay", "putting" to "put", "rode" to "ride", "ridden" to "ride",
        "rang" to "ring", "rung" to "ring", "rose" to "rise", "risen" to "rise",
        "ran" to "run", "running" to "run", "said" to "say", "saw" to "see", "seen" to "see",
        "sought" to "seek", "sold" to "sell", "sent" to "send", "shook" to "shake", "shaken" to "shake",
        "shone" to "shine", "shot" to "shoot", "shown" to "show", "shrank" to "shrink", "shrunk" to "shrink",
        "sang" to "sing", "sung" to "sing", "sank" to "sink", "sunk" to "sink",
        "sat" to "sit", "sitting" to "sit", "slept" to "sleep", "slid" to "slide",
        "spoke" to "speak", "spoken" to "speak", "spent" to "spend", "spun" to "spin",
        "stood" to "stand", "stole" to "steal", "stolen" to "steal", "stuck" to "stick",
        "stung" to "sting", "struck" to "strike", "swore" to "swear", "sworn" to "swear",
        "swept" to "sweep", "swam" to "swim", "swum" to "swim", "swung" to "swing",
        "took" to "take", "taken" to "take", "taking" to "take",
        "taught" to "teach", "tore" to "tear", "torn" to "tear", "told" to "tell",
        "thought" to "think", "threw" to "throw", "thrown" to "throw",
        "understood" to "understand", "woke" to "wake", "woken" to "wake",
        "wore" to "wear", "worn" to "wear", "won" to "win", "winning" to "win",
        "wound" to "wind", "wrote" to "write", "written" to "write", "writing" to "write",
        // Irregular plurals & comparatives
        "children" to "child", "men" to "man", "women" to "woman", "people" to "person",
        "mice" to "mouse", "teeth" to "tooth", "feet" to "foot", "geese" to "goose",
        "knives" to "knife", "lives" to "life", "wives" to "wife", "leaves" to "leaf",
        "shelves" to "shelf", "wolves" to "wolf", "halves" to "half", "thieves" to "thief",
        "better" to "good", "best" to "good", "worse" to "bad", "worst" to "bad",
        "further" to "far", "farther" to "far", "elder" to "old", "eldest" to "old",
        "criteria" to "criterion", "phenomena" to "phenomenon", "analyses" to "analysis",
        "crises" to "crisis", "hypotheses" to "hypothesis", "theses" to "thesis"
    )

    /**
     * Produces ordered candidate base forms for an inflected English query (excluding the raw query itself).
     * Examples: "abandoned" -> ["abandon"], "studying" -> ["study"], "happier" -> ["happy"], "children" -> ["child"].
     */
    fun generateEnglishStemCandidates(rawQuery: String): List<String> {
        val q = rawQuery.trim().lowercase(Locale.ROOT)
        if (q.length < 3 || ArabicNormalizer.containsArabic(q)) return emptyList()

        val out = LinkedHashSet<String>()
        IRREGULAR_LEMMAS[q]?.let { out.add(it) }

        // Plurals / 3rd person singular
        if (q.endsWith("ies") && q.length > 4) out.add(q.dropLast(3) + "y")
        if (q.endsWith("ves") && q.length > 4) {
            out.add(q.dropLast(3) + "f")
            out.add(q.dropLast(3) + "fe")
        }
        if (q.endsWith("es") && q.length > 4) {
            out.add(q.dropLast(2))
            out.add(q.dropLast(1))
        }
        if (q.endsWith("s") && !q.endsWith("ss") && q.length > 3) {
            out.add(q.dropLast(1))
        }

        // -ing forms (running -> run, making -> make, studying -> study, abandoning -> abandon)
        if (q.endsWith("ing") && q.length > 5) {
            val base = q.dropLast(3)
            out.add(base)
            out.add(base + "e")
            if (base.length >= 3 && base[base.length - 1] == base[base.length - 2]) {
                out.add(base.dropLast(1))
            }
            if (base.endsWith("y")) {
                out.add(base.dropLast(1) + "ie")
            }
        }

        // -ed forms (abandoned -> abandon, arrived -> arrive, studied -> study, stopped -> stop)
        if (q.endsWith("ied") && q.length > 4) {
            out.add(q.dropLast(3) + "y")
        }
        if (q.endsWith("ed") && q.length > 4) {
            val base = q.dropLast(2)
            out.add(base)
            out.add(base + "e")
            if (base.length >= 3 && base[base.length - 1] == base[base.length - 2]) {
                out.add(base.dropLast(1))
            }
        }

        // -er / -est comparatives (happier -> happy, bigger -> big, larger -> large)
        if (q.endsWith("ier") && q.length > 4) out.add(q.dropLast(3) + "y")
        if (q.endsWith("iest") && q.length > 5) out.add(q.dropLast(4) + "y")
        if (q.endsWith("er") && q.length > 4) {
            val base = q.dropLast(2)
            out.add(base)
            out.add(base + "e")
            if (base.length >= 3 && base[base.length - 1] == base[base.length - 2]) {
                out.add(base.dropLast(1))
            }
        }
        if (q.endsWith("est") && q.length > 5) {
            val base = q.dropLast(3)
            out.add(base)
            out.add(base + "e")
            if (base.length >= 3 && base[base.length - 1] == base[base.length - 2]) {
                out.add(base.dropLast(1))
            }
        }

        // -ly adverbs (quickly -> quick, happily -> happy)
        if (q.endsWith("ily") && q.length > 4) out.add(q.dropLast(3) + "y")
        if (q.endsWith("ly") && q.length > 4) out.add(q.dropLast(2))

        out.remove(q)
        return out.filter { it.length >= 2 }
    }

    /**
     * Standard bounded Damerau-Levenshtein edit distance (supports adjacent transpositions like "teh" -> "the").
     */
    fun editDistance(a: String, b: String): Int {
        val s1 = a.lowercase(Locale.ROOT)
        val s2 = b.lowercase(Locale.ROOT)
        if (s1 == s2) return 0
        val m = s1.length
        val n = s2.length
        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                var best = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    best = minOf(best, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = best
            }
        }
        return dp[m][n]
    }

    /**
     * Ranks candidate words by fuzzy edit distance (maxDist = 1 for short words <= 4, else 2).
     */
    fun rankFuzzyMatches(query: String, candidates: Collection<String>, limit: Int = 5): List<String> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.length < 3) return emptyList()
        val maxDist = if (q.length <= 4) 1 else 2

        return candidates.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && kotlin.math.abs(it.length - q.length) <= maxDist }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .map { cand -> cand to editDistance(q, cand) }
            .filter { (_, dist) -> dist in 1..maxDist }
            .sortedWith(
                compareBy<Pair<String, Int>> { it.second }
                    .thenBy { if (it.first.lowercase(Locale.ROOT).startsWith(q.take(2))) 0 else 1 }
                    .thenBy { it.first.length }
            )
            .take(limit)
            .map { it.first }
            .toList()
    }
}
