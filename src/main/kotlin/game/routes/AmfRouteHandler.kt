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
                        val i18n = I18NTable.dummy()

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
                                // must be false if not actually logged in!
                                "success" to false
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

                    "getMyWorlds" -> {
                        // args [getMyWorlds]
                        // getMyWorlds: method

                        // param1.screen=screenEvents, param1.action=newDataFromServer
                        // param1.subData = param2 type: eventsLoaded

                        val events = listOf(EventData.dummy())
                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to events
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
// but each is a dictionary of strings text
// langA, langB, langC, langD could possibly be "language packs"
// pack A maybe contains system UI, pack B maybe the game texts
// or it could be packA for earth world, packB for fantasy world etc
// and that the reason they are not in single data is for data optimization
// actual usage by client:
// dataM.getText("UI_LOGIN_ENTER_USER_AND_PASS")
data class I18NTable(
    val langA: List<I18NData> = emptyList(),
    val langB: List<I18NData> = emptyList(),
    val langC: List<I18NData> = emptyList(),
    val langD: List<I18NData> = emptyList(),
) {
    companion object {
        // must manually fill data
        // I don't know whether the original i18n table exist as an archive
        // specifically, strings aren't client-side, and not even downloaded by client
        // they are sent by server from a php API
        // the question: is it ever archived?
        // this includes every strings in the game like in-game events too
        fun dummy(): I18NTable {
            return I18NTable(
                langA = listOf(
                    I18NData(
                        sCode = "UI_LOGIN_ENTER_USER_AND_PASS",
                        sText = "Enter username and password",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_USERNAME",
                        sText = "Username",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_PASSWORD",
                        sText = "Password",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_FORGOT_PASSWORD",
                        sText = "Forgot password?",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_LOGIN",
                        sText = "Login",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_NEW_PLAYER",
                        sText = "hacked!!!",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_ENTER",
                        sText = "Enter",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_MY_WORLDS",
                        sText = "My Worlds",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_LOGOUT",
                        sText = "Logout",
                    ),
                    I18NData(
                        sCode = "UI_NEWS_NEWS",
                        sText = "News",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_FORUM",
                        sText = "Forum",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_GUIDE",
                        sText = "Guide",
                    ),
                )
            )
        }
    }
}

// string code and string text pair
// e.g., "helpText" to "Need help!" (in english data)
// e.g., "helpText" to "Butuh bantuan!" (in indonesia data)
data class I18NData(
    val sCode: String,
    val sText: String
)

// probably in-game notification (the [!] logo)
data class EventData(
    val eventID: String,

    // probably system data for event
    val bArchived: String,
    val bNew: String,
    val nType: Int,
    val nCategory: Int,

    val sTag: String?,   // another object
    val sData: String?,  // subData, another object
    val sDescription: String,

    // probably data for different kind of events
    val allianceID: String?,
    val rulerID: String?,
    val colonyID: String?,
    val squadID: String?,

    val nTick: Int,
    val tsSent: Int,
) {
    companion object {
        fun dummy(): EventData {
            return EventData(
                eventID = "event123",
                bArchived = "0",
                bNew = "1",
                nType = 1,
                nCategory = 1,
                sTag = "tag",
                sDescription = "This is a description",
                allianceID = null,
                rulerID = null,
                colonyID = null,
                squadID = null,
                sData = null,
                nTick = 1,
                tsSent = 1,
            )
        }
    }
}
