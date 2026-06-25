package game.amf

import io.ktor.utils.io.charsets.Charset
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import kotlin.text.Charsets

/**
 * AMF format serializer and deserializer.
 *
 * From [wikipedia](https://en.wikipedia.org/wiki/Action_Message_Format).
 */
object Amf {
    fun encode(response: AmfResponse): ByteArray {
        val stream = ByteArrayOutputStream()
        val output = DataOutputStream(stream)

        // amf version
        output.writeShort(3)

        // header count
        output.writeShort(0)

        // message count
        output.writeShort(1)

        val targetUri = when (response.netStatus) {
            AmfStatus.RESULT -> "${response.uri}/onResult"
            AmfStatus.ON_STATUS -> "${response.uri}/onStatus"
            AmfStatus.FAULT -> "${response.uri}/onFault"
        }
        val targetUriBytes = targetUri.toByteArray()

        // target uri length
        output.writeShort(targetUriBytes.size)

        // target uri string
        output.write(targetUriBytes)

        val responseUri = ""
        val responseUriBytes = responseUri.toByteArray()

        // response uri length
        output.writeShort(responseUriBytes.size)

        // response uri
        output.write(responseUriBytes)

        // write the body
        val bodyBuffer = ByteArrayOutputStream()
        val bodyOutput = DataOutputStream(bodyBuffer)
        writeAmfValue(response.data, bodyOutput)
        bodyOutput.flush()
        val bodyBytes = bodyBuffer.toByteArray()

        // body length
        output.writeInt(bodyBytes.size)

        // body
        output.write(bodyBytes)
        output.flush()

        return stream.toByteArray()
    }

    /**
     * No-impl: ECMA_ARRAY, XML_DOCUMENT, TYPED_OBJECT, SWITCH_TO_AMF3
     */
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

            headers.add(AmfHeader(headerNameString, header))

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

            val (service, method) = getServiceAndMethod(targetUriString)
            messages.add(
                AmfMessage(
                    target = targetUriString,
                    service = service,
                    method = method,
                    responseUri = responseUriString,
                    args = body.asList()
                )
            )

            messageCount -= 1
            order += 1
        }

        return AmfRequest(headers, messages)
    }

    private fun writeAmfValue(value: Any?, output: DataOutputStream) {
        when (value) {
            null -> {
                output.writeByte(AmfMarker.NULL.toInt())
            }

            is Number -> {
                output.writeByte(AmfMarker.NUMBER.toInt())
                output.writeDouble(value.toDouble())
            }

            is String -> {
                val bytes = value.toByteArray(Charsets.UTF_8)

                if (bytes.size > 65535) {
                    output.writeByte(AmfMarker.LONG_STRING.toInt())
                    output.writeInt(bytes.size)
                } else {
                    output.writeByte(AmfMarker.STRING.toInt())
                    output.writeShort(bytes.size)
                }

                output.write(bytes)
            }

            is Boolean -> {
                output.writeByte(AmfMarker.BOOLEAN.toInt())
                output.writeByte(if (!value) 0x00 else 0x01)
            }

            is List<Any?> -> {
                output.writeByte(AmfMarker.STRICT_ARRAY.toInt())
                output.writeInt(value.size)
                value.forEach { v ->
                    writeAmfValue(v, output)
                }
            }

            is Map<*, *> -> {
                output.writeByte(AmfMarker.OBJECT.toInt())
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
                output.writeByte(AmfMarker.OBJECT.toInt())
                error(Unit)
            }
        }
    }

    private fun readAmfValue(input: DataInputStream): Any? {
        return when (val typeMarker = input.readByte()) {
            AmfMarker.NULL -> null
            AmfMarker.NUMBER -> input.readDouble()
            AmfMarker.BOOLEAN -> input.readUnsignedByte() != 0
            AmfMarker.STRING -> {
                val len = input.readUnsignedShort()
                String(input.readNBytes(len), Charsets.UTF_8)
            }

            AmfMarker.LONG_STRING -> {
                val len = input.readInt()
                String(input.readNBytes(len), Charsets.UTF_8)
            }

            AmfMarker.STRICT_ARRAY -> {
                val size = input.readInt()
                buildList(size) {
                    repeat(size) {
                        add(readAmfValue(input))
                    }
                }
            }

            AmfMarker.OBJECT -> {
                val maps = mutableMapOf<String, Any?>()

                var keyLen = input.readUnsignedShort()
                while (keyLen != 0) {
                    val key = String(input.readNBytes(keyLen), Charsets.UTF_8)
                    val value = readAmfValue(input)
                    maps[key] = value
                    keyLen = input.readUnsignedShort()
                }

                val endMarker = input.readByte()
                if (endMarker != AmfMarker.OBJECT_END) {
                    error("Expected object end marker")
                }

                maps
            }

            AmfMarker.DATE -> {
                val date = input.readDouble()
                val timezoneOffset = input.readUnsignedShort()
                Pair(date, timezoneOffset)
            }

            else -> error("Unsupported AMF0 marker 0x${typeMarker.toString(16)}")
        }
    }

    private fun getServiceAndMethod(uri: String): Pair<String, String> {
        val idx = uri.lastIndexOf(".")
        return uri.substring(0, idx) to uri.substring(idx + 1, uri.length)
    }
}
