package game.routes

import encore.fancam.Fancam
import encore.route.RouteHandler
import encore.route.guard.NoAuthGuard
import encore.route.handle
import encore.utils.safeAsciiString
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

class AmfRouteHandler: RouteHandler {
    override fun Route.install() {
        post("/services/amfphp/gateway.php") {
            handle(call, NoAuthGuard) {
                val bytes = call.receive<ByteArray>()
                Fancam.debug { "Received POST AMF message: ${bytes.safeAsciiString()}" }
            }
        }
    }
}
