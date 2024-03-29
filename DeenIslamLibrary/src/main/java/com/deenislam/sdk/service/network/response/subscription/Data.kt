package com.deenislam.sdk.service.network.response.subscription

import androidx.annotation.Keep
import com.deenislam.sdk.service.network.response.payment.recurring.CheckRecurringResponse

@Keep
internal data class Data(
    val paymentTypes: List<PaymentType>,
    val premiumFeatures: List<PremiumFeature>,
    val pageResponse: CheckRecurringResponse?
)