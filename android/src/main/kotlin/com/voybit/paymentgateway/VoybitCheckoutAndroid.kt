package com.voybit.paymentgateway

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun VoybitCheckout.open(context: Context, checkoutUrl: String) {
    val uri = Uri.parse(checkoutUrl(publicId(checkoutUrl)))
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, uri)
}
