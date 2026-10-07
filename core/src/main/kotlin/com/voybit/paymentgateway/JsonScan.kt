package com.voybit.paymentgateway

internal class JsonScan(private val text: String) {
    private var index = 0

    fun skipWs() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    fun peek(): Char? = if (index >= text.length) null else text[index]

    fun consume(expected: Char): Boolean {
        if (peek() != expected) return false
        index++
        return true
    }

    fun string(): String? {
        if (!consume('"')) return null
        val out = StringBuilder()
        while (index < text.length) {
            val character = text[index++]
            when (character) {
                '"' -> return out.toString()
                '\\' -> {
                    if (index >= text.length) return null
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> out.append(escaped)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            if (index + 4 > text.length) return null
                            val code = text.substring(index, index + 4).toIntOrNull(16) ?: return null
                            index += 4
                            out.append(code.toChar())
                        }
                        else -> return null
                    }
                }
                else -> {
                    if (character.code < 0x20) return null
                    out.append(character)
                }
            }
        }
        return null
    }

    fun skipValue() {
        skipWs()
        when (peek()) {
            '"' -> if (string() == null) throw CheckoutException("checkout status was not JSON")
            '{' -> skipContainer('{', '}')
            '[' -> skipContainer('[', ']')
            't' -> literal("true")
            'f' -> literal("false")
            'n' -> literal("null")
            '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> skipNumber()
            else -> throw CheckoutException("checkout status was not JSON")
        }
    }

    private fun skipContainer(open: Char, close: Char) {
        if (!consume(open)) throw CheckoutException("checkout status was not JSON")
        skipWs()
        if (consume(close)) return
        while (true) {
            if (open == '{') {
                if (string() == null) throw CheckoutException("checkout status was not JSON")
                skipWs()
                if (!consume(':')) throw CheckoutException("checkout status was not JSON")
            }
            skipValue()
            skipWs()
            if (consume(close)) return
            if (!consume(',')) throw CheckoutException("checkout status was not JSON")
            skipWs()
        }
    }

    private fun literal(word: String) {
        if (!text.startsWith(word, index)) throw CheckoutException("checkout status was not JSON")
        index += word.length
    }

    private fun skipNumber() {
        if (consume('-')) {
            // sign
        }
        if (peek()?.isDigit() != true) throw CheckoutException("checkout status was not JSON")
        if (peek() == '0') {
            index++
        } else {
            while (peek()?.isDigit() == true) index++
        }
        if (peek() == '.') {
            index++
            if (peek()?.isDigit() != true) throw CheckoutException("checkout status was not JSON")
            while (peek()?.isDigit() == true) index++
        }
        if (peek() == 'e' || peek() == 'E') {
            index++
            if (peek() == '+' || peek() == '-') index++
            if (peek()?.isDigit() != true) throw CheckoutException("checkout status was not JSON")
            while (peek()?.isDigit() == true) index++
        }
    }
}

internal fun jsonString(json: String, key: String): String? {
    val parser = JsonScan(json)
    parser.skipWs()
    if (!parser.consume('{')) return null
    while (true) {
        parser.skipWs()
        if (parser.consume('}')) return null
        val name = parser.string() ?: return null
        parser.skipWs()
        if (!parser.consume(':')) return null
        parser.skipWs()
        if (parser.peek() == '"') {
            val value = parser.string() ?: return null
            if (name == key) return value
        } else {
            parser.skipValue()
        }
        parser.skipWs()
        if (parser.consume('}')) return null
        if (!parser.consume(',')) return null
    }
}
