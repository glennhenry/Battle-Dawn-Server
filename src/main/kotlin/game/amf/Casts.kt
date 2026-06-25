package game.amf

/**
 * Cast this to [T] or return `null` if fails.
 */
fun <T> Any?.castToOrNull(): T? {
    @Suppress("UNCHECKED_CAST")
    return this as? T
}

/**
 * Convert this into `String` or throw [IllegalStateException].
 */
fun Any?.asString(): String {
    return this.castToOrNull<String>()
        ?: error("Cannot cast to string: $this")
}

/**
 * Convert this into `Boolean` or throw [IllegalStateException].
 */
fun Any?.asBoolean(): Boolean {
    return this.castToOrNull<Boolean>()
        ?: error("Cannot cast to boolean: $this")
}

/**
 * Convert this into `Int` or throw [IllegalStateException].
 */
fun Any?.asInt(): Int {
    return this.castToOrNull<Int>()
        ?: error("Cannot cast to int: $this")
}

/**
 * Convert this into `Double` or throw [IllegalStateException].
 */
fun Any?.asDouble(): Double {
    return this.castToOrNull<Double>()
        ?: error("Cannot cast to double: $this")
}

/**
 * Convert this into `List<Any?>` or throw [IllegalStateException].
 */
fun Any?.asList(): List<Any?> {
    return this.castToOrNull<List<Any?>>()
        ?: error("Cannot cast to list: $this")
}

/**
 * Convert this into `Map<String, Any?>` or throw [IllegalStateException].
 */
fun Any?.asObject(): Map<String, Any?> {
    return this.castToOrNull<Map<String, Any?>>()
        ?: error("Cannot cast to object: $this")
}
