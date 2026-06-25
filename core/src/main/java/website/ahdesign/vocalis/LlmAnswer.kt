package website.ahdesign.vocalis

import org.json.JSONObject

data class LlmAnswer(
    val translation: String,
    val reply: String,
)

/**
 * Parse the JSON object the LLM returns, with a safe fallback if the model
 * misbehaves and emits plain text or wraps the JSON in code fences.
 * Pure function - shared by the online (OpenRouter) and offline (on-device LLM) paths.
 */
fun parseLlmAnswer(raw: String): LlmAnswer {
    val cleaned =
        raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    return runCatching {
        val obj = JSONObject(cleaned)
        LlmAnswer(
            translation = obj.optString("translation").trim(),
            reply = obj.optString("reply").trim(),
        )
    }.getOrElse {
        LlmAnswer(translation = "", reply = cleaned)
    }
}
