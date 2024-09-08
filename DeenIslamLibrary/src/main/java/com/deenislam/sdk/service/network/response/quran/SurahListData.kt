package com.deenislam.service.network.response.quran

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class SurahListData(
    val ContentBaseUrl: String,
    val ContentUrl: String,
    val Duration: String,
    val IsFavorite: Boolean,
    val Language: String,
    val Name: String,
    val NameInArabic: String,
    val NameMeaning: String,
    val Origin: String,
    val SurahNo: Int,
    val TotalAyat: Int
):Parcelable