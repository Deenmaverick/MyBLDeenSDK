package com.deenislam.sdk.service.network.response.common.subcatcardlist

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
data class Detail(
    val ImageUrl: String,
    val Pronunciation: String,
    val Serial: Int,
    val Text: String,
    val TextInArabic: String,
    val Title: String,
    val contenturl: String,
    val reference: String
):Parcelable