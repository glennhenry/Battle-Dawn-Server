package game.domain.others

import encore.fancam.Fancam
import game.routes.I18NData
import java.io.File

/**
 * String table for the game.
 *
 * - Uses `.txt` file (<key>=<value> format) for simplicity.
 * - Call [load] before usage.
 * - [strings] contains every loaded strings.
 * - Does not support `langA`, `langB`, `langC`, or `langD` yet.
 */
object StringsTable {
    private const val STRING_ASSETS_PATH = "assets/game/strings.txt"
    val strings = mutableListOf<I18NData>()
    var loaded = false

    fun load() {
        if (loaded) {
            Fancam.warn { "Strings table are already loaded" }
            return
        }
        loaded = true
        val stringsFile = File(STRING_ASSETS_PATH)
        for (line in stringsFile.readLines()) {
            val (code, text) = line.split("=", limit = 2)
            strings.add(I18NData(code, text))
        }
        Fancam.info { "Loaded a total of ${strings.size} strings entry" }
    }
}
