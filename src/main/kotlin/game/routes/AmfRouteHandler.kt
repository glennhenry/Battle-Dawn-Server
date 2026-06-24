package game.routes

import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.route.RouteHandler
import encore.route.guard.NoAuthGuard
import encore.route.handle
import game.domain.Amf
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class AmfRouteHandler: RouteHandler {
    override fun Route.install() {
        post("/services/amfphp/gateway.php") {
            handle(call, NoAuthGuard) {
                val bytes = call.receive<ByteArray>()

                val decoded = Amf.decode(bytes)
                Fancam.debug {
                    buildString {
                        appendLine("Received AMF messages:")
                        decoded.forEachIndexed { index, request ->
                            if (index == decoded.lastIndex) {
                                append("$INDENT - $request")
                            } else {
                                appendLine("$INDENT - $request")
                            }
                        }
                    }
                }

                call.respondBytes(bytes, status = HttpStatusCode.OK)
            }
        }
    }
}
