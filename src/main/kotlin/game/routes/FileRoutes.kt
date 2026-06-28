package game.routes

import encore.route.RouteHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

/**
 * Serve file-related endpoints.
 *
 * This mostly serving static files:
 * - Game and website assets in the `assets` folder.
 * - Docs website on production in the `docs_build` folder.
 *
 * Since this is simple, it doesn't use the [RouteHandler]
 */
fun Route.fileRoutes() {
    // website
    get("/") {
        call.respondFile(File("assets/site/index.html"))
    }
    staticFiles("site", File("assets/site"))

    // game files
    get("/client.swf") {
        call.respondFile(File("assets/game/client.swf"))
    }
    get("music/Intro.mp3") {
        call.respondFile(File("assets/game/resources/Intro.mp3"))
    }
    get("/tutorialAudio/{world}/{audioname}") {
        val world = call.parameters["world"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val audioname = call.parameters["audioname"]
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        val tutorialAudioDir = File("assets/game/resources/$world/tutorial/voice/$audioname")

        call.respondFile(tutorialAudioDir)
    }
    staticFiles("resources", File("assets/game/resources"))

    val docsDir = File("docs_build")
    if (File(docsDir, "index.html").exists()) {
        staticFiles("docs", docsDir)
    } else {
        get("/docs") {
            call.respond(
                HttpStatusCode.NotFound,
                "Docs website not available. Please start it with a separate vite server. " +
                        "If in prod, build the documentation website to access it."
            )
        }
    }
}
