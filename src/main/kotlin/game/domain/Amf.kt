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
    val version: Int,
    // header...
    val messages: List<AmfMessage>
)

data class AmfMessage(
    val uri: String,
    val responseId: String,
    val body: Any?
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

    fun decode(bytes: ByteArray): AmfRequest {
        val messages = mutableListOf<AmfMessage>()
        val input = DataInputStream(ByteArrayInputStream(bytes))

        val amfVersion = input.readUnsignedShort()
        var headerCount = input.readUnsignedShort()
        if (headerCount > 0) {
            Fancam.debug { "Got headerCount: $headerCount" }
        }

        // to-do update
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

        var messageCount = input.readUnsignedShort()
        var order = 1

        while (messageCount > 0) {
            val targetUriLength = input.readUnsignedShort()
            val targetUriString = input.readNBytes(targetUriLength).toString(Charset.defaultCharset())

            val responseUriLength = input.readUnsignedShort()
            val responseUriString = input.readNBytes(responseUriLength).toString(Charset.defaultCharset())

            val bodyLength = input.readInt()
            val body = readAmfValue(input)
            messages.add(AmfMessage(targetUriString, responseUriString, body))

            messageCount -= 1
            order += 1
        }

        return AmfRequest(amfVersion, messages)
    }

    private fun readAmfValue(input: DataInputStream): Any? {
        return when (val typeMarker = input.readByte()) {
            NUMBER -> input.readDouble()
            BOOLEAN -> input.readUnsignedByte() != 0
            STRING -> {
                val len = input.readUnsignedShort()
                String(input.readNBytes(len), Charsets.UTF_8)
            }

            OBJECT -> {
                Fancam.debug { "Got OBJECT (no-impl)" }
                error(Unit)
            }

            NULL -> null
            ECMA_ARRAY -> {
                val size = input.readInt()
                buildList(size) {
                    repeat(size) {
                        add(readAmfValue(input))
                    }
                }
            }

            OBJECT_END -> {
                Fancam.debug { "Got OBJECT_END (no-impl)" }
                error(Unit)
            }

            STRICT_ARRAY -> {
                val size = input.readInt()
                buildList(size) {
                    repeat(size) {
                        add(readAmfValue(input))
                    }
                }
            }

            DATE -> {
                Fancam.debug { "Got DATE (no-impl)" }
                error(Unit)
            }

            LONG_STRING -> {
                val len = input.readInt()
                String(input.readNBytes(len), Charsets.UTF_8)
            }

            XML_DOCUMENT -> {
                Fancam.debug { "Got XML_DOCUMENT (no-impl)" }
                error(Unit)
            }

            TYPED_OBJECT -> {
                Fancam.debug { "Got TYPED_OBJECT (no-impl)" }
                error(Unit)
            }

            SWITCH_TO_AMF3 -> {
                Fancam.debug { "Got SWITCH_TO_AMF3 (no-impl)" }
                error(Unit)
            }

            else -> error("Unsupported AMF0 marker 0x${typeMarker.toString(16)}")
        }
    }

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
