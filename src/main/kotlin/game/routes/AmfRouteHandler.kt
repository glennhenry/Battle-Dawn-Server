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
                        val resObject = mapOf(
                            "success" to true,
                            "user_id" to 123,
                            "ROLES" to "",
                            "display_name" to "keplian",
                            "avatar_data" to mapOf(
                                "avatar_link" to "https://picsum.photos/50/50",
                                "avatar_width" to 50,
                                "avatar_height" to 50,
                            ),
                        )
                        val response = Amf.encode(msg.responseUri, resObject)
                        Fancam.debug {
                            "Responding to getUserData with: ${response.safeAsciiString()}"
                        }
                        call.respondBytes(response, status = HttpStatusCode.OK)
                    }

                    else -> {
                        Fancam.debug { "Unhandled method '${msg.method}' for service '${msg.service}'" }
                    }
                }
            }

            else -> {
                Fancam.debug { "Unhandled message for '${msg.target}'" }
            }
        }
    }
}
