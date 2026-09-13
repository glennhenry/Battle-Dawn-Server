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
                                "ROLES" to "TEMPORARY ACCOUNTS",
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

                    "getI18NTableNew" -> {
                        // args [0, 1, 0, getI18NTable]
                        // 0: languageID (must be number, supplied on flashvar, defined by LanguageTable)
                        // 1: themeID of the selected world
                        // 0: hardcoded number
                        // getI18NTable: the method name

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to I18NTable.dummy()
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getI18NTableNew with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }

                    "getAllTablesNoColony" -> {
                        // args []

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to mapOf(
                                    "globalSettingsTable" to mapOf(
                                        "result" to SettingsTable.dummy()
                                    ),
                                    "flagsTable" to mapOf(
                                        "result" to FlagsTable.dummy()
                                    ),
                                    "tutorialsTable" to mapOf(
                                        "result" to TutorialsTable.dummy()
                                    ),
                                )
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getAllTablesNoColony with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.secure.BDUserSServices" -> {
                when (msg.method) {
                    // very weird, createTemporaryAccount does not call anything else
                    // once you click new player, the UI is disabled and nothing else happened
                    // what supposed to happen is world selection is shown and you select world to enter
                    // but createTemporaryAccount instead do something very weird,
                    // it literally call ScreenRegister and ScreenTopLeft which doesn't exist yet
                    // they are literally in-game screens, but why would it be called?????
                    // they result in null error, which can be ignored, but since nothing else happened
                    // you just stuck
                    "createTemporaryAccount" -> {
                        // args [createTemporaryAccount]
                        // createTemporaryAccount: method

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to mapOf(
                                    "pixelURL" to ""
                                )
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to createTemporaryAccount with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.insecure.BDRulerIServices" -> {
                when (msg.method) {
                    "userHasRuler" -> {
                        // args []

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to false
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to userHasRuler with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.insecure.BDGameIServices" -> {
                when (msg.method) {
                    "getLocalSettingsTable" -> {
                        // args []
                        // the response is similar to global setting table getAllTablesNoColony
                        // but this is specific to the selected world and has a call to generateTutorialDetails
                        // the 77 tutorial details are fortunately generated by client-side

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to SettingsTable.dummy()
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getLocalSettingsTable with: ${response.safeAsciiString()}"
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
                // worldID can't be string!
                // the game never mentions the type of dict's key
                // and every access to the dict is normal `.worldID`
                // but there are code somewhere comparing that with integer 17 (what's up with the number 17 anyway?)
                // editing this to number somehow give the server a new request
                // but by using integer, we somehow never reach the world selection screen
                // it skips directly to world loading??
                // it turns out that worldID numbers are hardcoded in the game's code.
                // number >= 0 selects a world automatically (probably last visited world or server recommended)
                // number == -1 means no world is selected yet <== USE THIS!
                worldID = "-1",
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
                        sCode = "UI_LOGIN_LOGIN_ERROR",
                        sText = "Login Error",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_NEW_PLAYER",
                        sText = "New player",
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
                    I18NData(
                        sCode = "UI_LOGIN_GUIDE_THEME",
                        sText = "Guide Theme",
                    ),
                    I18NData(
                        sCode = "UI_LOGIN_GUIDE_WORLD",
                        sText = "Guide World",
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

data class SettingsTable(
    // setting name & setting value
    // referenced directly by client with dataM.getSettings
    val sName: String,
    val sValue: String,
) {
    companion object {
        fun dummy(): List<SettingsTable> {
            return listOf(
                SettingsTable(
                    sName = "SERVER_TOKENS_DISCOUNT",
                    sValue = "50"
                ),
                SettingsTable(
                    sName = "TUTORIAL_VERSION",
                    sValue = "1"
                ),
                SettingsTable(
                    sName = "TECH_ALLOW_MIL_INFANTRY",
                    sValue = "1"
                ),
                SettingsTable(
                    sName = "TECH_ALLOW_OP_OUTPOST",
                    sValue = "1"
                ),
                SettingsTable(
                    sName = "TIPS_COUNT",
                    sValue = "0"
                ),
                SettingsTable(
                    sName = "MAP_NAME",
                    sValue = "shallowest"
                ),
                SettingsTable(
                    sName = "URL_THEME_GFX",
                    sValue = "themeGfx/themeGfx.swf"
                ),
                SettingsTable(
                    sName = "URL_RESOURCES_DOMAIN",
                    sValue = "game/"
                ),
                SettingsTable(
                    sName = "URL_RESOURCES_PATH",
                    sValue = "resources/"
                ),
                SettingsTable(
                    sName = "URL_THEME",
                    sValue = "earth/" // should be changed dynamically based on world
                ),
                SettingsTable(
                    sName = "URL_CLIENT",
                    sValue = ""
                )
            )
        }
    }
}

data class FlagsTable(
    val flagID: String,
    val sISO: String, // probably assets path to flag icon
) {
    companion object {
        fun dummy(): List<FlagsTable> {
            return listOf(
                FlagsTable(
                    flagID = "US",
                    sISO = "787"
                ),
                FlagsTable(
                    flagID = "JP",
                    sISO = "799"
                )
            )
        }
    }
}

// probably need video reference to fill this
data class TutorialsTable(
    val tutorialID: Int, // client show an access tutorialsTable[9999]
    val sImg: String, // sprite image? reference to setting 'IMG_EXTENSION_TUTORIAL'
    val nBonusM: Int, // bonus metal
    val nBonusO: Int, // bonus oil
    val nBonusE: Int, // bonus energy
    val nBonusP: Int, // bonus population
) {
    companion object {
        fun dummy(): List<TutorialsTable> {
            return listOf(
                TutorialsTable(
                    tutorialID = 1,
                    sImg = "",
                    nBonusM = 123,
                    nBonusO = 234,
                    nBonusE = 345,
                    nBonusP = 456
                ),
                TutorialsTable(
                    tutorialID = 2,
                    sImg = "xxx",
                    nBonusM = 111,
                    nBonusO = 222,
                    nBonusE = 333,
                    nBonusP = 444
                ),
            )
        }
    }
}
