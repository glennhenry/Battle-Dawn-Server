package game.routes

import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.route.RouteHandler
import encore.route.guard.NoAuthGuard
import encore.route.handle
import encore.utils.safeAsciiString
import encore.utils.toJsonString
import game.amf.Amf
import game.amf.AmfMessage
import game.amf.AmfResponse
import game.amf.AmfStatus
import game.amf.asDouble
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class AmfRouteHandler : RouteHandler {
    override fun Route.install() {
        post("/services/amfphp/gateway.php") {
            handle(call, NoAuthGuard) {
                val bytes = call.receive<ByteArray>()
                val request = Amf.decode(bytes)

                Fancam.debug {
                    buildString {
                        appendLine("Received AMF message:")
                        append("$INDENT ${request.toJsonString(INDENT.length)}")
                    }
                }

                request.messages.forEach { handleMessage(it) }
            }
        }
    }

    private suspend fun RoutingContext.handleMessage(msg: AmfMessage) {
        when (msg.service) {
            "net.battlegate.secure.AcctServices" -> {
                when (msg.method) {
                    "getUserData" -> {
                        // args [getUserData]
                        // getUserData: method

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "user_id" to 123,
                                "ROLES" to "",
                                "display_name" to "keplian",
                                "avatar_data" to mapOf(
                                    "avatar_link" to "https://picsum.photos/50/50",
                                    "avatar_width" to 50,
                                    "avatar_height" to 50,
                                )
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getUserData with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.insecure.BDGlobalsIServices" -> {
                when (msg.method) {
                    "getPreLoginData" -> {
                        // args [0, 0, 3, getPreLoginData]
                        // 0: languageID from loaderParams (default=0/null)
                        // 0: not known
                        // 3: not known
                        // getPreloginData: method

                        val args = msg.args.iterator()
                        val languageId = args.next().asDouble().toInt()
                        val notKnown1 = args.next().asDouble().toInt()
                        val notKnown2 = args.next().asDouble().toInt()

                        val worlds = listOf(
                            WorldTable.dummy()
                        )
                        val languages = listOf(
                            LanguageTable.dummy()
                        )
                        val i18n = I18NTable()

                        val resultObject = mapOf(
                            "worldsTable" to mapOf(
                                "result" to worlds
                            ),
                            "languagesTable" to mapOf(
                                "result" to languages
                            ),
                            "I18NTable" to mapOf(
                                "result" to i18n
                            ),
                            "urlFlags" to "echo", // not used anywhere
                            "extFlags" to "echo", // not used anywhere
                            "isLoggedIn" to mapOf(
                                "success" to true
                            )
                        )

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to resultObject
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getPreLoginData with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            else -> {
                Fancam.debug { "Unhandled message for '${msg.target}'" }
            }
        }
    }
}

data class WorldTable(
    val worldID: String,
    val nPlayers: Int,
    val nMaxCapacity: Int,
    val nTick: Int,
    val nMaxTick: Int,
    val bSolo: Int,   // whether the world is solo (no in-game group)
    val bActive: Int, // whether the world is still active (not ended yet)
    val themeID: Int  // 1-based index (earth,mars,fantasy,galaxy)
) {
    companion object {
        fun dummy(): WorldTable {
            return WorldTable(
                worldID = "planet",
                nPlayers = 1,
                nMaxCapacity = 100,
                nTick = 1,
                nMaxTick = 1000,
                bSolo = 0,
                bActive = 1,
                themeID = 1
            )
        }
    }
}

data class LanguageTable(
    val languageID: Int,
    val sFlag: String,
    val sName: String
) {
    companion object {
        fun dummy(): LanguageTable {
            return LanguageTable(
                languageID = 0,
                sFlag = "us",
                sName = "English"
            )
        }
    }
    // available sFlag:
    // "us" "gr" "il" "de" "ru" "jp" "sa" "es" "fr" "tr" "br" "pt" "ir" "cn"
}

// I don't know what is langA, B, C, and D
// but each could be a dictionary between languages
data class I18NTable(
    val langA: List<I18NData> = emptyList(),
    val langB: List<I18NData> = emptyList(),
    val langC: List<I18NData> = emptyList(),
    val langD: List<I18NData> = emptyList(),
)

// string code and string text pair
// e.g., "helpText" to "Need help!" (in english data)
// e.g., "helpText" to "Butuh bantuan!" (in indonesia data)
data class I18NData(
    val sCode: String,
    val sText: String
)
