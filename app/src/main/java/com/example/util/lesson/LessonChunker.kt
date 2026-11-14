package com.example.util.lesson

data class LessonChunk(
    val leadIn: String?,
    val text: String
)

object LessonChunker {

    private val ABBREVIATIONS = setOf(
        "mis", "dll", "dsb", "dst", "vs", "no", "dr", "prof",
        "hlm", "gb", "gbr", "pers", "dgn", "spt", "tsb", "dkk"
    )

    private val CODE_TOKEN_REGEX = Regex(
        pattern = "^(?:M\\d{2}[A-Z]?|T\\d+(?:\\.\\d+)*|[A-Z]\\d+(?:\\.\\d+)*|\\d+)$",
        option = RegexOption.IGNORE_CASE
    )

    fun chunk(detailFull: String, summaryShort: String): List<LessonChunk> {
        val cleanDetail = detailFull.trim()
        val cleanSummary = summaryShort.trim()

        if (cleanDetail.length < 40) {
            val fallback = cleanSummary.ifEmpty { cleanDetail }.ifEmpty { "-" }
            return listOf(LessonChunk(leadIn = null, text = fallback))
        }

        val paragraphs = cleanDetail
            .split(Regex("\\n\\s*\\n+"))
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) {
            val fallback = cleanSummary.ifEmpty { cleanDetail }
            return listOf(LessonChunk(leadIn = null, text = fallback))
        }

        val rawChunks = mutableListOf<LessonChunk>()

        for (paragraph in paragraphs) {
            val sentences = splitSentences(paragraph)
            if (sentences.isEmpty()) continue

            if (sentences.size == 1) {
                rawChunks.add(LessonChunk(leadIn = null, text = sentences[0]))
                continue
            }

            val firstSentence = sentences.first()
            val hasShortLeadIn = firstSentence.length <= 60
            val bodySentences = if (hasShortLeadIn) sentences.drop(1) else sentences
            val groupedBodies = groupSentences(bodySentences)

            if (groupedBodies.isEmpty()) {
                rawChunks.add(LessonChunk(leadIn = null, text = firstSentence))
            } else {
                groupedBodies.forEachIndexed { index, bodyText ->
                    val lead = if (index == 0 && hasShortLeadIn) firstSentence else null
                    rawChunks.add(LessonChunk(leadIn = lead, text = bodyText))
                }
            }
        }

        if (rawChunks.isEmpty()) {
            return listOf(LessonChunk(leadIn = null, text = cleanSummary.ifEmpty { cleanDetail }))
        }

        return enforceMaxEightChunks(rawChunks)
    }

    internal fun splitSentences(paragraph: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var parenDepth = 0
        val len = paragraph.length
        var i = 0

        while (i < len) {
            val ch = paragraph[i]
            when (ch) {
                '(', '[', '{' -> parenDepth++
                ')', ']', '}' -> if (parenDepth > 0) parenDepth--
            }
            sb.append(ch)

            if (parenDepth == 0 && (ch == '.' || ch == '!' || ch == '?')) {
                val nextIdx = i + 1
                if (nextIdx < len && paragraph[nextIdx].isWhitespace()) {
                    var afterWs = nextIdx
                    while (afterWs < len && paragraph[afterWs].isWhitespace()) {
                        afterWs++
                    }
                    if (afterWs < len) {
                        val nextChar = paragraph[afterWs]
                        val startsNewSentence = nextChar.isUpperCase() || nextChar.isDigit() || nextChar == '(' || nextChar == '"' || nextChar == '“'
                        if (startsNewSentence && !isProtectedPeriod(paragraph, i, nextChar)) {
                            val built = sb.toString().trim()
                            if (built.isNotEmpty()) {
                                result.add(built)
                            }
                            sb.setLength(0)
                            i = afterWs - 1
                        }
                    }
                }
            }
            i++
        }

        val tail = sb.toString().trim()
        if (tail.isNotEmpty()) {
            result.add(tail)
        }
        return result
    }

    private fun isProtectedPeriod(text: String, periodIndex: Int, nextNonSpaceChar: Char): Boolean {
        if (text[periodIndex] != '.') return false

        // Check decimal or code with immediate non-space (already excluded by whitespace check,
        // but check token immediately preceding the period)
        var tokenStart = periodIndex - 1
        while (tokenStart >= 0 && !text[tokenStart].isWhitespace() && text[tokenStart] != '(' && text[tokenStart] != '[') {
            tokenStart--
        }
        val rawToken = text.substring(tokenStart + 1, periodIndex).trim()
        if (rawToken.isEmpty()) return false

        val lowerToken = rawToken.lowercase().trimStart('"', '\'', '“')
        if (lowerToken in ABBREVIATIONS) return true

        // Single uppercase letter or short code token like M07A., T1.7., 1., 2.
        if (rawToken.length == 1 && rawToken[0].isLetter()) return true
        if (CODE_TOKEN_REGEX.matches(rawToken)) return true

        // Check if token contains internal dots like T1.7 or e.g.
        if ('.' in rawToken && rawToken.any { it.isDigit() || it.isUpperCase() }) return true

        // If next token starts with lowercase, do not split
        if (nextNonSpaceChar.isLowerCase()) return true

        return false
    }

    private fun groupSentences(sentences: List<String>): List<String> {
        if (sentences.isEmpty()) return emptyList()
        val groups = mutableListOf<String>()
        var current = mutableListOf<String>()
        var currentLen = 0

        for (sentence in sentences) {
            val s = sentence.trim()
            if (s.isEmpty()) continue
            if (current.isEmpty()) {
                current.add(s)
                currentLen = s.length
            } else {
                val projectedLen = currentLen + 1 + s.length
                val canFitTwo = current.size < 2 && projectedLen <= 220
                val canFitThreeShort = current.size == 2 && projectedLen <= 140
                if (canFitTwo || canFitThreeShort) {
                    current.add(s)
                    currentLen = projectedLen
                } else {
                    groups.add(current.joinToString(" "))
                    current = mutableListOf(s)
                    currentLen = s.length
                }
            }
        }
        if (current.isNotEmpty()) {
            groups.add(current.joinToString(" "))
        }
        return groups
    }

    private fun enforceMaxEightChunks(initial: List<LessonChunk>): List<LessonChunk> {
        val list = initial.toMutableList()
        while (list.size > 8) {
            // Find adjacent pair with smallest combined character length
            var bestIdx = 0
            var bestLen = Int.MAX_VALUE
            for (idx in 0 until list.size - 1) {
                val combinedLen = chunkTotalLength(list[idx]) + chunkTotalLength(list[idx + 1])
                if (combinedLen < bestLen) {
                    bestLen = combinedLen
                    bestIdx = idx
                }
            }
            val a = list[bestIdx]
            val b = list[bestIdx + 1]
            val bText = if (!b.leadIn.isNullOrBlank()) "${b.leadIn} ${b.text}".trim() else b.text
            val merged = LessonChunk(
                leadIn = a.leadIn,
                text = "${a.text} $bText".trim()
            )
            list[bestIdx] = merged
            list.removeAt(bestIdx + 1)
        }
        return list
    }

    private fun chunkTotalLength(chunk: LessonChunk): Int {
        return (chunk.leadIn?.length ?: 0) + chunk.text.length
    }
}
