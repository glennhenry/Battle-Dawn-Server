package game.amf

/**
 * Byte markers for AMF0 format.
 */
object AmfMarker {
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

/**
 * Byte markers for AMF3 format.
 */
object Amf3Marker {
    const val UNDEFINED: Byte = 0x00
    const val NULL: Byte = 0x01
    const val BOOLEAN_FALSE: Byte = 0x02
    const val BOOLEAN_TRUE: Byte = 0x03
    const val INTEGER: Byte = 0x04
    const val DOUBLE: Byte = 0x05
    const val STRING: Byte = 0x06
    const val XMLDOCUMENT: Byte = 0x07
    const val DATE: Byte = 0x08
    const val ARRAY: Byte = 0x09
    const val OBJECT: Byte = 0x0A
    const val XML: Byte = 0x0B
    const val BYTE_ARRAY: Byte = 0x0C
}
