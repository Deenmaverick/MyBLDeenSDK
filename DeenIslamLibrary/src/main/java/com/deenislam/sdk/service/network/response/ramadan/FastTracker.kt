package com.deenislam.sdk.service.network.response.ramadan

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class FastTracker(
    val Date: String,
    val Iftaar: String,
    val Suhoor: String,
    var isFasting: Boolean,
    val islamicDate: String,
    val totalDays: Int,
    var totalTracked: Int
) : Parcelable