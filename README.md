# Voybit checkout for Android

Your server creates the payment and returns `checkout_url`. This library does not take an API key.

```kotlin
VoybitCheckout.open(this, checkoutUrl)
```

`open` validates the URL and loads `https://voybit.com/pay/{id}` in a Custom Tab.

```kotlin
val status = VoybitCheckout.status(publicId)
if (status.confirmed) {
    // paid or overpaid — refresh the screen only
}
```

Fulfil the order from the webhook on your server. `status` calls `GET https://api.voybit.com/api/v1/checkout/{public_id}` and is only for the screen.

The `android` module depends on `core`. `minSdk` is 24. Not published to Maven Central; add this repository to your Gradle build.
