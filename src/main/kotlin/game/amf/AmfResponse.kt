package game.amf

/**
 * Represent a response to a single AMF message within an AMF request.

 * @property uri The response URI which should match the
 *               corresponding [AmfMessage.responseUri].
 * @property netStatus Network [AmfStatus] to send to the client.
 * @property data Data object that matches the client's expectations.
 */
data class AmfResponse(
    val uri: String,
    val netStatus: AmfStatus,
    val data: Map<String, Any?>
)

/**
 * Represent the response status of an AMF request.
 */
enum class AmfStatus {
    /**
     * The request is fulfilled and response is success.
     */
    RESULT,

    /**
     * The request couldn't be fulfilled due to technical issues.
     */
    ON_STATUS,

    /**
     * The request is fulfilled but response is fail due to an application-level fault.
     */
    FAULT
}
