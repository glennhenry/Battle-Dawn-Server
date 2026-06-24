package game.domain

import encore.fancam.Fancam
import encore.fancam.INDENT
import io.ktor.utils.io.charsets.Charset
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * Representation of AMF request.
 */
data class AmfRequest(
    val uri: String,
    val responseId: String,
    val amfMethod: String
)

/**
 * AMF format serializer and deserializer.
 *
 * From [wikipedia](https://en.wikipedia.org/wiki/Action_Message_Format).
 * From [wikipedia](https://blog.qdac.cc/?p=3605).
 */
object Amf {
    fun encore(data: Map<String, String>): ByteArray {
        return byteArrayOf()
    }

    fun decode(bytes: ByteArray): List<AmfRequest> {
        val output = mutableListOf<AmfRequest>()
        val input = DataInputStream(ByteArrayInputStream(bytes))

        // 2 bytes version
        val amfVersion = input.readUnsignedShort()
        if (amfVersion == 0) {
            Fancam.warn { "Got AMF0 message" }
        }

        // 2 bytes header-count
        var headerCount = input.readUnsignedShort()
        if (headerCount > 0) {
            Fancam.debug { "Got headerCount: $headerCount" }
        }

        // header-type-structure
        while (headerCount > 0) {
            // 2 bytes header-name-length
            val headerNameLength = input.readUnsignedShort()

            // 1 bytes * header-name-length = headerNameString
            val headerNameString = input.readNBytes(headerNameLength).toString(Charset.defaultCharset())

            // 1 bytes must-understand
            val mustUnderstand = input.readUnsignedByte()

            // 4 bytes header-length
            val headerLength = input.readInt()

            // 1 bytes * header-length = header (amf0 or amf3)
            val header = input.readNBytes(headerLength)

            Fancam.debug {
                buildString {
                    appendLine("headerNameString=$headerNameString")
                    appendLine("$INDENT mustUnderstand=$mustUnderstand")
                    append("$INDENT header=${header.toString(Charset.defaultCharset())}")
                }
            }
            headerCount -= 1
        }

        // 2 bytes message-count
        var messageCount = input.readUnsignedShort()
        Fancam.debug { "Got messageCount: $messageCount " }

        var order = 1
        // message-type-structure
        while (messageCount > 0) {
            // 2 bytes target-uri-length
            val targetUriLength = input.readUnsignedShort()

            // 1 bytes * target-uri-length = target-uri-string
            val targetUriString = input.readNBytes(targetUriLength).toString(Charset.defaultCharset())

            // 2 bytes response-uri-length
            val responseUriLength = input.readUnsignedShort()

            // 1 bytes * response-uri-length = response-uri-string
            val responseUriString = input.readNBytes(responseUriLength).toString(Charset.defaultCharset())

            Fancam.debug {
                buildString {
                    appendLine("$order) message:")
                    appendLine("$INDENT targetUriString=$targetUriString")
                    append("$INDENT responseUriString=$responseUriString")
                }
            }

            // 4 bytes message-length
            val messageLength = input.readInt()

            // 1 bytes amf switch marker (0A hex / 10 dec)
            val amfSwitch = input.readByte()

            // 1 bytes * message-length = message (amf0 or amf3)
            // val messageBytes = input.readNBytes(messageLength)
            // ...read directly from input

            // message payload
            // 1 bytes header(?)
            val messageHeader = input.readByte()

            // 3 bytes elementCount
            val elementCount = input.readNBytes(3).readTripleBytes()
            Fancam.debug { "Got elementCount: $elementCount" }

            // 1 bytes typeMarker
            val typeMarker = input.readByte()

            when (typeMarker) {
                NUMBER -> {
                    Fancam.debug { "Got NUMBER (no-impl)" }
                }

                BOOLEAN -> {
                    Fancam.debug { "Got BOOLEAN (no-impl)" }
                }

                STRING -> {
                    // 2 bytes stringLength
                    val stringLength = input.readUnsignedShort()

                    // 1 bytes * stringLength
                    val stringData = input.readNBytes(stringLength).toString(Charset.defaultCharset())

                    Fancam.debug { "Got STRING=$stringData" }
                    output.add(AmfRequest(targetUriString, responseUriString, stringData))
                }

                OBJECT -> {
                    Fancam.debug { "Got OBJECT (no-impl)" }
                }

                NULL -> {
                    Fancam.debug { "Got NULL (no-impl)" }
                }

                ECMA_ARRAY -> {
                    Fancam.debug { "Got ECMA_ARRAY (no-impl)" }
                }

                OBJECT_END -> {
                    Fancam.debug { "Got OBJECT_END (no-impl)" }
                }

                STRICT_ARRAY -> {
                    Fancam.debug { "Got STRICT_ARRAY (no-impl)" }
                }

                DATE -> {
                    Fancam.debug { "Got DATE (no-impl)" }
                }

                LONG_STRING -> {
                    Fancam.debug { "Got LONG_STRING (no-impl)" }
                }

                XML_DOCUMENT -> {
                    Fancam.debug { "Got XML_DOCUMENT (no-impl)" }
                }

                TYPED_OBJECT -> {
                    Fancam.debug { "Got TYPED_OBJECT (no-impl)" }
                }

                SWITCH_TO_AMF3 -> {
                    Fancam.debug { "Got SWITCH_TO_AMF3 (no-impl)" }
                }
            }

            messageCount -= 1
            order += 1
        }

        return output
    }

    fun ByteArray.readTripleBytes(): Int {
        require(this.size == 3)
        return ((this[0].toInt() and 0xFF) shl 16) or
                ((this[1].toInt() and 0xFF) shl 8) or
                ((this[2].toInt() and 0xFF))
    }

    // amf3 data format marker
//    const val UNDEFINED: Byte = 0x00
//    const val NULL: Byte = 0x01
//    const val BOOLEAN_FALSE: Byte = 0x02
//    const val BOOLEAN_TRUE: Byte = 0x03
//    const val INTEGER: Byte = 0x04
//    const val DOUBLE: Byte = 0x05
//    const val STRING: Byte = 0x06
//    const val XML_DOCUMENT: Byte = 0x07
//    const val DATE: Byte = 0X08
//    const val ARRAY: Byte = 0x09
//    const val OBJECT: Byte = 0x0A
//    const val XML: Byte = 0x0B
//    const val BYTE_ARRAY: Byte = 0x0C

    // amf0 data format marker
    const val NUMBER: Byte = 0x00
    const val BOOLEAN: Byte = 0x01
    const val STRING: Byte = 0x02
    const val OBJECT: Byte = 0x03
    const val NULL: Byte = 0x05
    const val ECMA_ARRAY: Byte = 0X08
    const val OBJECT_END: Byte = 0x09
    const val STRICT_ARRAY: Byte = 0x0A
    const val DATE: Byte = 0x0B
    const val LONG_STRING: Byte = 0x0C
    const val XML_DOCUMENT: Byte = 0x0F
    const val TYPED_OBJECT: Byte = 0x10
    const val SWITCH_TO_AMF3: Byte = 0x11
}
