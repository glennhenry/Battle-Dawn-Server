package game.domain

import encore.fancam.Fancam
import io.ktor.utils.io.charsets.Charset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.text.Charsets

/**
 * Representation of AMF request.
 */
data class AmfRequest(
    val version: Int,
    val headers: List<AmfHeader>,
    val messages: List<AmfMessage>
)

data class AmfMessage(
    val uri: String,
    val responseId: String,
    val body: Any?
)

data class AmfHeader(
    val name: String,
    val mustUnderstand: Int,
    val value: Any?
)

/**
 * AMF format serializer and deserializer.
 *
 * From [wikipedia](https://en.wikipedia.org/wiki/Action_Message_Format).
 */
object Amf {
    fun encode(): ByteArray {
        val stream = ByteArrayOutputStream()
        val output = DataOutputStream(stream)

        // amf version
        output.writeShort(3)

        // header count
        output.writeShort(0)

        // message count
        output.writeShort(1)

        val targetUri = "net.battlegate.secure.AcctServices.getUserData"
        val targetUriBytes = targetUri.toByteArray()

        // target uri length
        output.writeShort(targetUriBytes.size)

        // target uri string
        output.write(targetUriBytes)

        val responseUri = "/1"
        val responseUriBytes = responseUri.toByteArray()

        // response uri length
        output.writeShort(responseUriBytes.size)

        // response uri
        output.write(responseUriBytes)

        // body length
        output.writeInt(Int.MAX_VALUE)

        // marker for object
        output.writeByte(0x03)

        val response = mapOf(
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

        writeAmfValue(response, output)

        output.flush()
        return stream.toByteArray()
    }

    fun writeAmfValue(value: Any?, output: DataOutputStream) {
        when (value) {
            is Number -> {
                output.writeByte(NUMBER.toInt())
                output.writeDouble(value.toDouble())
            }

            is Boolean -> {
                output.writeByte(BOOLEAN.toInt())
                output.writeByte(if (!value) 0x00 else 0x01)
            }

            is String -> {
                val len = value.length
                if (len > 65536) {
                    // long string
                    output.writeByte(LONG_STRING.toInt())
                    output.writeInt(len)
                } else {
                    output.writeByte(STRING.toInt())
                    output.writeShort(len)
                }
                output.writeBytes(value)
            }

            null -> {
                output.writeByte(NULL.toInt())
                output.writeByte(0x05)
            }

            is Iterable<Any?> -> {
                output.writeByte(ECMA_ARRAY.toInt())
                output.writeInt(value.count())
                value.forEach { v ->
                    writeAmfValue(v, output)
                }
            }

            is Map<*, *> -> {
                output.writeByte(OBJECT.toInt())
                value.forEach { (k, v) ->
                    require(k is String)
                    // write key
                    output.writeShort(k.length)
                    output.writeBytes(k)

                    // write value
                    writeAmfValue(v, output)
                }
                output.writeShort(0)
                output.writeByte(0x09)
            }

            else -> {
                output.writeByte(OBJECT.toInt())
                error(Unit)
            }
        }
    }

    fun decode(bytes: ByteArray): AmfRequest {
        val messages = mutableListOf<AmfMessage>()
        val headers = mutableListOf<AmfHeader>()
        val input = DataInputStream(ByteArrayInputStream(bytes))

        val amfVersion = input.readUnsignedShort()
        var headerCount = input.readUnsignedShort()

        while (headerCount > 0) {
            val headerNameLength = input.readUnsignedShort()
            val headerNameString = input.readNBytes(headerNameLength).toString(Charset.defaultCharset())
            val mustUnderstand = input.readUnsignedByte()
            val headerLength = input.readInt()
            val header = readAmfValue(input)

            headers.add(AmfHeader(headerNameString, mustUnderstand, header))

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

        return AmfRequest(amfVersion, headers, messages)
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
                val maps = mutableMapOf<String, Any?>()

                var keyLen = input.readUnsignedShort()
                while (keyLen != 0) {
                    val key = String(input.readNBytes(keyLen), Charsets.UTF_8)
                    val value = readAmfValue(input)
                    maps[key] = value
                    keyLen = input.readUnsignedShort()
                }

                if (input.readByte().toInt() != 0x09) {
                    Fancam.warn { "end of map is not 0x09" }
                }

                maps
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
