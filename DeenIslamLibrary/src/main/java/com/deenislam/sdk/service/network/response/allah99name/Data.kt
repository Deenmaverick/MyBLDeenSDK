package com.deenislam.sdk.service.network.response.allah99name

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Data(
    val Arabic: String,
    val ContentUrl: String,
    val Fazilat: String,
    val Id: Int,
    val ImageUrl: String,
    val Meaning: String,
    val Name: String,
    val Serial: Int
):Parcelable