package com.voybit.paymentgateway

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun main() {
    val id = "nYVvXxsYGr5LZk8Dn7hU0Q"
    check(VoybitCheckout.publicId("https://voybit.com/pay/$id") == id, "public id")
    check(VoybitCheckout.checkoutUrl(id) == "https://voybit.com/pay/$id", "canonical")
    expectInvalid("http://voybit.com/pay/$id")
    expectInvalid("https://voybit.com/pay/$id?x=1")
    expectInvalid("https://example.com/pay/$id")

    val nested = """
        {"deposit_instructions":{"status":"ready","address":"secret-address"},"note":"\"status\":\"paid\"","status":"pending","public_id":"$id","checkout_url":"https://voybit.com/pay/$id"}
    """.trim()
    val pending = VoybitCheckout.parseStatus(nested, id)
    check(pending.status == "pending" && !pending.confirmed, "nested status")
    check(!pending.toString().contains("secret-address"), "address omitted")

    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    var sawKey = false
    server.createContext("/api/v1/checkout/$id") { exchange ->
        sawKey = exchange.requestHeaders.getFirst("X-Voybit-Api-Key") != null
        val body = """{"status":"paid","public_id":"$id","checkout_url":"https://voybit.com/pay/$id","deposit_instructions":{"address":"secret-address"}}"""
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
    server.start()
    try {
        VoybitCheckout.apiOrigin = "http://127.0.0.1:${server.address.port}"
        val paid = VoybitCheckout.status(id)
        check(paid.confirmed && paid.status == "paid" && !sawKey, "paid")
        check(!paid.toString().contains("secret-address"), "response address omitted")
    } finally {
        VoybitCheckout.apiOrigin = VoybitCheckout.API_ORIGIN
        server.stop(0)
    }
    println("kotlin ok")
}

private fun expectInvalid(url: String) {
    try {
        VoybitCheckout.publicId(url)
        throw AssertionError("accepted $url")
    } catch (_: CheckoutException) {
    }
}

private fun check(condition: Boolean, message: String) {
    if (!condition) throw AssertionError(message)
}
