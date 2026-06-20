package website.ahdesign.vocalis

/**
 * How the suggested reply should be written so the user can read/pronounce it.
 * Moved here from AppSettings so both :app and :wear share one definition.
 */
enum class ReplyScript(
    val id: String,
    val displayName: String,
) {
    LATIN("latin", "Latin (transliterated)"),
    NATIVE("native", "Native (same script as source language)"),
    CYRILLIC("cyrillic", "Cyrillic"),
    PERSIAN("persian", "Persian / Arabic script"),
    ;

    companion object {
        fun fromId(id: String?): ReplyScript = values().firstOrNull { it.id == id } ?: LATIN
    }
}
