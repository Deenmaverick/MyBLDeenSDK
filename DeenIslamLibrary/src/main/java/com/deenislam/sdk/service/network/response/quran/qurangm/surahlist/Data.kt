package com.deenislam.sdk.service.network.response.quran.qurangm.surahlist

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Data(
    val ContentUrl: String,
    val Id: Int,
    val ImageUrl: String,
    val IsFavorite: Boolean,
    val Order: Int,
    val SurahId: Int,
    val SurahName: String,
    val SurahNameInArabic: String,
    val SurahNameMeaning: String,
    val SurahOrigin: String,
    val TotalAyat: String,
    val rKey:String,
    val wKey:String,
    var folderLocation: String = ""
) : Parcelable