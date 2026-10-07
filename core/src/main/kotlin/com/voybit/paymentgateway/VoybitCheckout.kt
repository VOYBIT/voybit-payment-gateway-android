package com.voybit.paymentgateway

import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

class CheckoutException(message: String) : Exception(message)

data class CheckoutStatus(
    val publicId: String,
    val status: String,
    val checkoutUrl: String,
) {
    val confirmed: Boolean get() = status == "paid" || status == "overpaid"
}

object VoybitCheckout {
    const val CHECKOUT_ORIGIN = "https://voybit.com"
    const val API_ORIGIN = "https://api.voybit.com"
    private const val USER_AGENT = "voybit-payment-gateway-android/0.1.0"
    private val publicIdPattern = Regex("^[A-Za-z0-9_-]{22}$")

    fun publicId(checkoutUrl: String): String {
        val uri = try {
            URI(checkoutUrl.trim())
        } catch (_: Exception) {
            throw CheckoutException("checkout URL is invalid")
        }
        val id = uri.path?.removePrefix("/pay/")?.removeSuffix("/").orEmpty()
        val pathOk = uri.path == "/pay/$id" || uri.path == "/pay/$id/"
        if (uri.scheme != "https" || uri.host?.lowercase() != "voybit.com" || uri.userInfo != null ||
            !uri.rawQuery.isNullOrEmpty() || uri.fragment != null || !publicIdPattern.matches(id) || !pathOk
        ) {
            throw CheckoutException("checkout URL is invalid")
        }
        return id
    }

    fun checkoutUrl(publicId: String): String {
        if (!publicIdPattern.matches(publicId)) throw CheckoutException("checkout URL is invalid")
        return "$CHECKOUT_ORIGIN/pay/$publicId"
    }

    internal var apiOrigin: String = API_ORIGIN

    fun status(publicId: String): CheckoutStatus {
        val id = publicId(checkoutUrl(publicId))
        val connection = (URI("$apiOrigin/api/v1/checkout/$id").toURL().openConnection() as HttpURLConnection)
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 20_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            if (code !in 200..299) throw CheckoutException("checkout status returned HTTP $code")
            return parseStatus(readLimited(stream), id)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseStatus(body: String, publicId: String): CheckoutStatus {
        val status = jsonString(body, "status") ?: throw CheckoutException("checkout status was not JSON")
        val id = jsonString(body, "public_id") ?: publicId
        val checkoutUrl = jsonString(body, "checkout_url") ?: checkoutUrl(id)
        if (!publicIdPattern.matches(id)) throw CheckoutException("checkout status was not JSON")
        return CheckoutStatus(id, status, checkoutUrl)
    }

    private fun readLimited(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
            val buffer = CharArray(4096)
            val out = StringBuilder()
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (out.length + count > 1 shl 20) throw CheckoutException("checkout status was too large")
                out.append(buffer, 0, count)
            }
            return out.toString()
        }
    }
}
