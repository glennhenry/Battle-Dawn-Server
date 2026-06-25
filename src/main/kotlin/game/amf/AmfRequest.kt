package game.amf

/**
 * Representation of a decoded AMF packet.
 *
 * An AMF packet may contain zero or more headers and one or more messages.
 */
data class AmfRequest(
    val headers: List<AmfHeader>,
    val messages: List<AmfMessage>
)

/**
 * Representation of an AMF packet header.
 *
 * Headers provide additional metadata that applies to the request, such as
 * authentication information or protocol-specific flags.
 *
 * @property name Header name.
 * @property value Decoded header value.
 */
data class AmfHeader(
    val name: String,
    val value: Any?
)

/**
 * Representation of a single AMF message.
 *
 * @property target Full target URI specified by the client,
 *                  including both the service name and method name.
 *                  e.g., `net.battlegate.secure.AcctServices.getUserData`
 * @property service Service portion of the target URI.
 *                   e.g., `net.battlegate.secure.AcctServices`
 * @property method Method portion of the target URI. e.g., `getUserData`
 * @property responseUri Response URI supplied by the client.
 *                       This value is used by the client to match a
 *                       request with the server's response.
 * @property args Arguments supplied for the remote method invocation.
 */
data class AmfMessage(
    val target: String,
    val service: String,
    val method: String,
    val responseUri: String,
    val args: List<Any?>
)
