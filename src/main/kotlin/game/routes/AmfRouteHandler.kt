package game.routes

import encore.fancam.Fancam
import encore.fancam.INDENT
import encore.route.RouteHandler
import encore.route.guard.NoAuthGuard
import encore.route.handle
import encore.utils.safeAsciiString
import game.domain.Amf
import game.utils.prettyPrint
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
                        append("$INDENT ${request.prettyPrint(INDENT.length)}")
                    }
                }

                val response = Amf.encode()

                Fancam.debug { "Responding with: ${response.safeAsciiString()}" }

                call.respondBytes(response, status = HttpStatusCode.OK)
            }
        }
    }
}
