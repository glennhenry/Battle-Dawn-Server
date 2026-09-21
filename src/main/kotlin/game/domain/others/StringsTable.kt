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

// either use this or via strings.txt file
// still don't know how to format text for tutorial
// there is ChatParsing.as which tells custom format BBCode-like langauge
// use like /yellow OK or raw without parse <font color="FFFF00">text</font>
// when i use this on tutorial text, it doens't work (or am i doing it the wrong way?)
const val STRINGS_TXT = """
UI_LOGIN_ENTER_USER_AND_PASS=Enter username and password
UI_LOGIN_USERNAME=Username
UI_LOGIN_PASSWORD=Password
UI_LOGIN_FORGOT_PASSWORD=Forgot password?
UI_LOGIN_LOGIN=Login
UI_LOGIN_LOGIN_ERROR=Login Error
UI_LOGIN_NEW_PLAYER=New player
UI_LOGIN_ENTER=Enter
UI_LOGIN_MY_WORLDS=My Worlds
UI_LOGIN_LOGOUT=Logout
UI_NEWS_NEWS=News
UI_LOGIN_FORUM=Forum
UI_LOGIN_GUIDE=Guide
UI_LOGIN_GUIDE_THEME=Welcome to Earth/Mars/Fantasy version of Battle Dawn
UI_LOGIN_GUIDE_WORLD=This is the Earth world
UI_LOGIN_OK=Ok
UI_LOGIN_EARTH=Earth
UI_LOGIN_MARS=Mars
UI_LOGIN_FANTASY=Fantasy
UI_LOGIN_AUTHENTICATION_SUCCESSFUL=Logged in
UI_LOGIN_SELECT_A_WORLD=Select a world to play on
UI_LOGIN_WORLD_LIST_WORLD=Worlds
UI_LOGIN_WORLD_LIST_WORLD_NAME=Name
UI_LOGIN_WORLD_LIST_SPEED=Speed
UI_LOGIN_WORLD_LIST_TICK=Tick
UI_LOGIN_WORLD_LIST_TICKS_PER_HOUR=t/hr
UI_LOGIN_WORLD_LIST_PLAYERS=Players
UI_LOGIN_WORLD_LIST_LAST_LOGIN=Last login
UI_LOGIN_WORLD_LIST_RULER_NAME=Your username
TIP_LOADING=Get helpful tips while the game is loading!
UI_TUT_REWARD=Rewards
UI_BC_COLONY_TITLE=Build colony
UI_BC_COLONY_SELECT_LOCATION=Select location
UI_BC_RANDOM_START_LOCATION=Find a random start location
UI_BC_COLONY_ZOOM_IN=Zoom in in order to build your Colony
UI_TUT_MUTE=Mute
UI_TUT_TT_PHASE_BAR=Tutorial progress
UI_BOTTOMRIGHT_TT_OPEN_CLOSE_TUTORIAL=Toggle tutorial
UI_TOPLEFT_TT_FIND=Find a location
UI_MAPFOCUS_INSTRUCTION=Click on the map to start interacting with it
UI_TOPRIGHT_TT_ZOOM_IN=Zoom In
UI_TOPRIGHT_TT_ZOOM_OUT=Zoom Out
TT_MAP_ZOOM_IN_TO_BUILD=(Zoom in to build)
TT_MAP_ON_WATER=Cannot build on water
GENERAL_LOADING=Loading
GENERAL_OK=OK
GENERAL_METAL=Metal
GENERAL_OIL=Oil
GENERAL_ENERGY=Energy
GENERAL_WORKERS=Worker
GENERAL_CRYSTALS=Crystal
GENERAL_MISSILE=Missile
GENERAL_SPY=Spy
GENERAL_ALLIED=Allied
GENERAL_HOSTILE=Hostile
GENERAL_NEUTRAL=Neutral
GENERAL_FRIENDLY=Friendly
GENERAL_COST=Cost
GENERAL_TIME_LEFT=Time left
GENERAL_DAYS=Days
GENERAL_NOTENOUGH=Not enough
GENERAL_NOT_ENOUGH=Not enough
GENERAL_CONFIRM=Confirm
GENERAL_NOT_ENOUGH_ENERGY=Not enough energy
GENERAL_NOT_ENOUGH_INFILTRATION=Not enough infiltration
GENERAL_SUBMIT=Submit
GENERAL_DESCRIPTION=Description
GENERAL_DELETE=Delete
GENERAL_ATTACKERS=Attackers
GENERAL_DEFENDERS=Defenders
GENERAL_RESTART=Restart
GENERAL_RULER=Ruler
GENERAL_RULER_NAME=Player name
GENERAL_ALLIANCE=Alliance
GENERAL_BUILD=Build
GENERAL_CANCEL=Cancel
GENERAL_CLOSE=Close
GENERAL_DISTANCE=Distance
GENERAL_EDIT=Edit
GENERAL_ACCEPT=Accept
GENERAL_DECLINE=Decline
GENERAL_NEXT=Next
GENERAL_PREVIOUS=Previous
GENERAL_RANK=Rank
GENERAL_NAME=Name
GENERAL_COLONY=Colony
GENERAL_TAG=Tag
GENERAL_MEMBERS=Members
GENERAL_NO_THANKS=No thanks
TUT_001=Welcome to Battle Dawn, a game of strategy, tactics, diplomacy & skill. Battle Dawn is a Persistent Browser Based Game, meaning you can play it from anywhere with an Internet Browser. This tutorial will cover all basic gameplay. Click the [OK] button to continue.
TUT_002=The tutorial can be accessed by clicking the [?] button at the top left of the screen. Completion of the tutorial steps will reward you with plenty of resources to get you started.
TUT_003=The first step we'll cover is controlling the map. |Zoom-in by using the mouse wheel or the +/- keys. You can also double-click on the area you wish to look at. |Try zooming the map now.
TUT_004=Good! Now pan the map by dragging it with your mouse or using the keyboard arrow keys. |Try panning the map now.
TUT_005=Task 1: Build a Colony| Excellent! It's time to establish your colony. Select a location by panning & zooming the map to the maximum. Then click on an unoccupied territory and enter your chosen player name.
"""
