package com.example.data.lesson

import android.content.Context
import android.util.Log
import com.example.data.LegacyNodeCodes
import org.json.JSONArray
import org.json.JSONObject

data class StoryQuestion(
    val id: String,
    val scenario: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val followUps: List<String> = emptyList(),
    val reviewed: Boolean = false,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null
)

data class RevealPrediction(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class RevealChainStep(
    val text: String,
    val prediction: RevealPrediction? = null
)

data class InteractiveContent(
    val type: String = "REVEAL_CHAIN",
    val title: String,
    val intro: String,
    val steps: List<RevealChainStep>,
    val reviewed: Boolean = false
)

data class NodeLessonExtras(
    val nodeCode: String,
    val nanoHints: List<String> = emptyList(),
    val nanoHintsReviewed: Boolean = false,
    val recap: List<String> = emptyList(),
    val recapReviewed: Boolean = false,
    val stories: List<StoryQuestion> = emptyList(),
    val interactive: InteractiveContent? = null
)

data class LessonExtras(
    val version: Int = 1,
    val nodes: Map<String, NodeLessonExtras> = emptyMap()
) {
    fun forNode(rawCode: String): NodeLessonExtras? {
        val norm = LegacyNodeCodes.normalize(rawCode)
        return nodes[norm]
    }
}

object LessonExtrasLoader {
    private const val TAG = "LessonExtrasLoader"
    private const val ASSET_FILE = "lesson_extras.json"

    @Volatile
    private var cached: LessonExtras? = null

    fun load(context: Context): LessonExtras {
        cached?.let { return it }
        synchronized(this) {
            cached?.let { return it }
            val loaded = try {
                val jsonText = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
                parseJson(jsonText)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memuat $ASSET_FILE, menggunakan LessonExtras kosong", e)
                LessonExtras()
            }
            cached = loaded
            return loaded
        }
    }

    fun parseJson(jsonString: String): LessonExtras {
        if (jsonString.isBlank()) return LessonExtras()
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val nodesObj = root.optJSONObject("nodes") ?: return LessonExtras(version = version)
            val nodesMap = mutableMapOf<String, NodeLessonExtras>()

            val keys = nodesObj.keys()
            while (keys.hasNext()) {
                val rawKey = keys.next()
                val normKey = LegacyNodeCodes.normalize(rawKey)
                val nodeObj = nodesObj.optJSONObject(rawKey) ?: continue
                val parsed = parseNodeExtras(normKey, nodeObj)
                val existing = nodesMap[normKey]
                nodesMap[normKey] = if (existing == null) {
                    parsed
                } else {
                    Log.w(TAG, "Kunci duplikat setelah normalisasi pada $normKey (asal=$rawKey); menggabungkan entri")
                    mergeNodeExtras(existing, parsed)
                }
            }
            LessonExtras(version = version, nodes = nodesMap)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mem-parse JSON lesson_extras", e)
            LessonExtras()
        }
    }

    private fun parseNodeExtras(nodeCode: String, obj: JSONObject): NodeLessonExtras {
        val (nanoHints, nanoHintsObjReviewed) = parseReviewedStringSection(obj, "nanoHints")
        val nanoHintsReviewed = if (obj.has("nanoHintsReviewed")) {
            obj.optBoolean("nanoHintsReviewed", false)
        } else {
            nanoHintsObjReviewed
        }

        val (recap, recapObjReviewed) = parseReviewedStringSection(obj, "recap")
        val recapReviewed = if (obj.has("recapReviewed")) {
            obj.optBoolean("recapReviewed", false)
        } else {
            recapObjReviewed
        }

        val stories = mutableListOf<StoryQuestion>()
        val storiesArr = obj.optJSONArray("stories")
        if (storiesArr != null) {
            for (i in 0 until storiesArr.length()) {
                val sObj = storiesArr.optJSONObject(i) ?: continue
                val id = sObj.optString("id").trim()
                val scenario = sObj.optString("scenario").trim()
                val question = sObj.optString("question").trim()
                val options = sObj.optJSONArray("options").toStringList()
                val correctIndex = sObj.optInt("correctIndex", 0)
                val explanation = sObj.optString("explanation").trim()
                val followUps = sObj.optJSONArray("followUps").toStringList()
                val reviewed = sObj.optBoolean("reviewed", false)
                val reviewedBy = sObj.optString("reviewedBy").takeIf { it.isNotBlank() && it != "null" }
                val reviewedAt = sObj.optString("reviewedAt").takeIf { it.isNotBlank() && it != "null" }

                if (id.isNotEmpty() && question.isNotEmpty() && options.size >= 2 && correctIndex in options.indices) {
                    stories.add(
                        StoryQuestion(
                            id = id,
                            scenario = scenario,
                            question = question,
                            options = options,
                            correctIndex = correctIndex,
                            explanation = explanation,
                            followUps = followUps,
                            reviewed = reviewed,
                            reviewedBy = reviewedBy,
                            reviewedAt = reviewedAt
                        )
                    )
                }
            }
        }

        val interactiveObj = obj.optJSONObject("interactive")
        val interactive = if (interactiveObj != null) {
            val type = interactiveObj.optString("type", "REVEAL_CHAIN")
            val title = interactiveObj.optString("title").trim()
            val intro = interactiveObj.optString("intro").trim()
            val reviewed = interactiveObj.optBoolean("reviewed", false)
            val stepsArr = interactiveObj.optJSONArray("steps")
            val steps = mutableListOf<RevealChainStep>()
            if (stepsArr != null) {
                for (i in 0 until stepsArr.length()) {
                    val stepObj = stepsArr.optJSONObject(i) ?: continue
                    val text = stepObj.optString("text").trim()
                    val predObj = stepObj.optJSONObject("prediction")
                    val prediction = if (predObj != null) {
                        val pQuestion = predObj.optString("question").trim()
                        val pOptions = predObj.optJSONArray("options").toStringList()
                        val pCorrect = predObj.optInt("correctIndex", 0)
                        if (pQuestion.isNotEmpty() && pOptions.size >= 2 && pCorrect in pOptions.indices) {
                            RevealPrediction(
                                question = pQuestion,
                                options = pOptions,
                                correctIndex = pCorrect
                            )
                        } else null
                    } else null
                    if (text.isNotEmpty()) {
                        steps.add(RevealChainStep(text = text, prediction = prediction))
                    }
                }
            }
            if (steps.isNotEmpty()) {
                InteractiveContent(
                    type = type,
                    title = title,
                    intro = intro,
                    steps = steps,
                    reviewed = reviewed
                )
            } else null
        } else null

        return NodeLessonExtras(
            nodeCode = nodeCode,
            nanoHints = nanoHints,
            nanoHintsReviewed = nanoHintsReviewed,
            recap = recap,
            recapReviewed = recapReviewed,
            stories = stories,
            interactive = interactive
        )
    }

    private fun parseReviewedStringSection(obj: JSONObject, key: String): Pair<List<String>, Boolean> {
        val arr = obj.optJSONArray(key)
        if (arr != null) {
            return arr.toStringList() to false
        }
        val wrapper = obj.optJSONObject(key)
        if (wrapper != null) {
            val items = wrapper.optJSONArray("items").toStringList()
            val reviewed = wrapper.optBoolean("reviewed", false)
            return items to reviewed
        }
        return emptyList<String>() to false
    }

    private fun mergeNodeExtras(first: NodeLessonExtras, second: NodeLessonExtras): NodeLessonExtras {
        val mergedHints = (first.nanoHints + second.nanoHints).distinct()
        val mergedRecap = (first.recap + second.recap).distinct()
        val mergedStories = (first.stories + second.stories).distinctBy { it.id }
        val hintsReviewed = when {
            first.nanoHints.isEmpty() -> second.nanoHintsReviewed
            second.nanoHints.isEmpty() -> first.nanoHintsReviewed
            else -> first.nanoHintsReviewed && second.nanoHintsReviewed
        }
        val recapReviewed = when {
            first.recap.isEmpty() -> second.recapReviewed
            second.recap.isEmpty() -> first.recapReviewed
            else -> first.recapReviewed && second.recapReviewed
        }
        return NodeLessonExtras(
            nodeCode = first.nodeCode,
            nanoHints = mergedHints,
            nanoHintsReviewed = hintsReviewed,
            recap = mergedRecap,
            recapReviewed = recapReviewed,
            stories = mergedStories,
            interactive = second.interactive ?: first.interactive
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            if (isNull(i)) continue
            val raw = opt(i)
            if (raw == null || raw === JSONObject.NULL) continue
            val item = optString(i, "").trim()
            if (item.isNotEmpty()) list.add(item)
        }
        return list
    }
}
