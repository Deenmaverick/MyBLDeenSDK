package com.deenislam.sdk.service.network.response.common.subcatcardlist

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Data(
    val Category: String?,
    val CategoryId: Int = 0,
    val Id: Int = 0,
    val ImageUrl: String?,
    val Language: String?,
    val Pronunciation: String?,
    val Serial: Int= 0,
    val Text: String?,
    val TextInArabic: String?,
    val reference: String?,
    val Title: String?,
    val details: List<Detail> ? = arrayListOf(),
    var IsTracked:Boolean,
    var isExpanded:Boolean = false
):Parcelable