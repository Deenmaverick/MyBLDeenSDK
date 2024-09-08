package com.deenislam.sdk.service.models.payment

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
class PaymentModel(
    val title: String,
    val amount: String,
    val redirectPage: Int,
    val isTelcoEnable: Boolean = false,
    val isBkashEnable: Boolean = false,
    val isNagadEnable: Boolean = false,
    val isSSLEnable: Boolean = false,
    val isGpayEnable: Boolean = false,
    val serviceIDBkash: Int = 0,
    val serviceIDSSL: Int = 0,
    val serviceIDNagad: String = "",
    val serviceIDGpay: String? = null,
    val paySuccessMessage: String,
    val tcUrl: String? = null,
    val isRecurring:Boolean = false
) : Parcelable