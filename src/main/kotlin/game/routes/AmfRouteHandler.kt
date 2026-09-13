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
import game.domain.others.StringsTable
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.Int

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
                        val i18n = I18NTable(langA = StringsTable.strings)

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
                                // empty table, no string extension
                                "result" to I18NTable()
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

                    "getAllRulers" -> {
                        // args []

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to Ruler.dummy()
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getAllRulers with: ${response.safeAsciiString()}"
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

            "com.battledawn.insecure.BDExternalAccountIServices" -> {
                when (msg.method) {
                    "getExternalAccounts" -> {
                        // args [exampleSessionKey]
                        // supplied via 'exSessionKey' flashvar

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to mapOf(
                                    "externalAccounts" to MyFriends.dummy(),
                                    // not sure; external site stuff
                                    "myExternalSources" to emptyList<String>()
                                )
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getExternalAccounts with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.insecure.BDAllianceIServices" -> {
                when (msg.method) {
                    "getAllAlliances" -> {
                        // args []

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to Alliance.dummy()
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getAllAlliances with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }
                }
            }

            "com.battledawn.insecure.BDMapIServices" -> {
                when (msg.method) {
                    "getColonies" -> {
                        // args []

                        val amfResponse = AmfResponse(
                            uri = msg.responseUri,
                            netStatus = AmfStatus.RESULT,
                            data = mapOf(
                                "success" to true,
                                "result" to Colony.empty()
                            )
                        )
                        val response = Amf.encode(amfResponse)
                        Fancam.debug {
                            "Responding to getColonies with: ${response.safeAsciiString()}"
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

// many of the settings should be changed based on world being entered
// this fixed the settings into the earth world
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
                    sValue = "earth/"
                ),
                SettingsTable(
                    sName = "URL_CLIENT",
                    sValue = ""
                ),
                SettingsTable(
                    sName = "URL_TERRAIN_SWF",
                    sValue = "terrainmap.swf"
                ),
                SettingsTable(
                    sName = "RANK_ICON_MAX",
                    sValue = "7"
                ),
                SettingsTable(
                    sName = "RANK_ICON_POWER_REQUIRED",
                    sValue = "1"
                ),
                SettingsTable(
                    // whether player is allowed to join the world
                    sName = "SERVER_JOIN_ENABLED",
                    sValue = "1"
                ),
                SettingsTable(
                    sName = "URL_MINIMAP_IMG",
                    sValue = "minimap.jpg"
                ),
                SettingsTable(
                    // this was cosmetics name
                    sName = "SERVER_NAME",
                    sValue = "Loona"
                ),
                SettingsTable(
                    sName = "AVATAR_DEFAULT_THEME_1",
                    sValue = Avatar().packIntoString()
                ),
                SettingsTable(
                    sName = "CHAT_ENABLED",
                    sValue = "1"
                ),
                SettingsTable(
                    sName = "URL_MUSIC",
                    sValue = "game/resources/earth/music/"
                ),
                SettingsTable(
                    sName = "URL_TUTORIAL_IMG",
                    sValue = "tutorial/screenshots/"
                ),
                SettingsTable(
                    sName = "IMG_EXTENSION_TUTORIAL",
                    sValue = ".jpg"
                ),
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
// some tutorial ID are not in order, like 1 -> 2 -> 4
// don't know when 3 will happen
data class TutorialsTable(
    val tutorialID: Int, // client show an access tutorialsTable[9999]
    val sImg: String, // sprite image? reference to setting 'IMG_EXTENSION_TUTORIAL'
    val nBonusM: Int = 0, // bonus metal
    val nBonusO: Int = 0, // bonus oil
    val nBonusE: Int = 0, // bonus energy
    val nBonusP: Int = 0, // bonus population
) {
    companion object {
        fun dummy(): List<TutorialsTable> {
            return listOf(
                TutorialsTable(
                    tutorialID = 1,
                    sImg = "defaultPicture"
                ),
                TutorialsTable(
                    tutorialID = 2,
                    sImg = "defaultPicture"
                ),
                TutorialsTable(
                    tutorialID = 4,
                    sImg = "buildColony"
                ),
            )
        }
    }
}

// ruler is the in-game ranking
// many of the fields are okay if null - they will be defaulted by client themselves
// those are commented out
// should fill them later
data class Ruler(
    val rulerID: Int, // this seems like playerID
    val cBanned: Int = 0,
    val sAvatar: String = "",
    val nRankIconNumber: Int = 0,
    val nPower: Int = 0,
    val nConquers: Int = 0,
    val nRelics: Int = 0,
    val sBio: String = "",
    val bBioLoaded: Boolean = false,
    val bFacebook: Boolean = false,
    val medalsA: List<Int> = emptyList(),
    val medalsB: List<Int> = emptyList(),
    val medalsC: List<Int> = emptyList(),
    val medalsD: List<Int> = emptyList(),
    val showMyMedals: Boolean = true,
    val myMulti: Boolean = false
) {
    companion object {
        fun dummy(): List<Ruler> {
            return listOf(
                Ruler(
                    rulerID = 1,
                    cBanned = 0,
                    sAvatar = "",
                    nPower = 123
                ),
                Ruler(
                    rulerID = 2,
                    cBanned = 0,
                    sAvatar = "xyz",
                    nPower = 100
                ),
            )
        }
    }
}

data class MyFriends(
    val facebook: List<Int> // rulerID
) {
    companion object {
        fun dummy(): MyFriends {
            return MyFriends(emptyList())
        }
    }
}

data class Alliance(
    val allianceID: Int,
    val nRelics: Int = 0,
    val nResC: Int = 0,
    val nMembers: Int = 0,
    val gotAllianceRoles: Boolean = false,
) {
    companion object {
        fun dummy(): List<Alliance> {
            return listOf(
                Alliance(
                    allianceID = 1,
                    nRelics = 12,
                    nResC = 3,
                    nMembers = 1,
                    gotAllianceRoles = false
                )
            )
        }
    }
}

// colony is the player's main base
// the result of colony request means requesting every colony (i.e, player's base) that exist in the world
data class Colony(
    val colonyID: Int,
    val mainColonyID: Int,
    val rulerID: Int = -1, // if someone has conquered player's colony
    val myColony: Boolean = false,
    val myAllianceColony: Boolean = false,
    val nType: Int = 0,
    val nState: Int = 0,
    val nXPos: Int = 0,
    val nYPos: Int = 0,
    val nRadarRange: Int = 1,
    val nBattery: Int = 0,
    val bAutoShield: Int = 0,
    val cSpyProtect: Int = 0,
    val nResC: Int = 0
) {
    companion object {
        fun empty(): List<Colony> {
            return listOf()
        }

        fun dummy(): List<Colony> {
            return listOf(
                Colony(
                    colonyID = 1,
                    mainColonyID = 1,
                    rulerID = -1,
                    nXPos = 500,
                    nYPos = 500
                )
            )
        }
    }
}

// avatar is sent as string data (can also be sent as avatarURL)
// avatar had customization like the type of beret, the color
// the type of nose, the x/y position
data class Avatar(
    val avatarURL: String = "",
    val race: String = "1",
    val gender: String = "1",
    val beretNFrame: String = "1",
    val beretColor: String = "1",
    val hairNFrame: String = "1",
    val hairColor: String = "1",
    val eyesNFrame: String = "1",
    val eyesColor: String = "1",
    val eyesXPos: String = "1",
    val eyesYPos: String = "1",
    val eyesXStretch: String = "1",
    val eyesYStretch: String = "1",
    val noseNFrame: String = "1",
    val noseColor: String = "1",
    val noseXPos: String = "1",
    val noseYPos: String = "1",
    val noseXStretch: String = "1",
    val noseYStretch: String = "1",
    val mouthNFrame: String = "1",
    val mouthColor: String = "1",
    val mouthXPos: String = "1",
    val mouthYPos: String = "1",
    val mouthXStretch: String = "1",
    val mouthYStretch: String = "1",
    val beardNFrame: String = "1",
    val beardColor: String = "1",
    val uniformNFrame: String = "1",
    val uniformColor: String = "1",
    val bodyNFrame: String = "1",
    val bodyColor: String = "1",
    val backgroundNFrame: String = "1",
    val backgroundColor: String = "1",
) {
    fun packIntoString(): String {
        return listOf(
            race, gender, beretNFrame, beretColor, hairNFrame,
            hairColor, eyesNFrame, eyesColor, eyesXPos, eyesYPos,
            eyesXStretch, eyesYStretch, noseNFrame, noseColor, noseXPos,
            noseYPos, noseXStretch, noseYStretch, mouthNFrame, mouthColor,
            mouthXPos, mouthYPos, mouthXStretch, mouthYStretch, beardNFrame,
            beardColor, uniformNFrame, uniformColor, bodyNFrame, bodyColor,
            backgroundNFrame, backgroundColor
        ).joinToString(",")
    }
}
