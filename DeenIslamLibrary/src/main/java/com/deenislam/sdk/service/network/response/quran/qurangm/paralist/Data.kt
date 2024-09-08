package com.deenislam.sdk.service.network.response.quran.qurangm.paralist

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Data(
    val Id: Int,
    val IsFavorite: Boolean,
    val JuzId: Int,
    val JuzName_bn: String,
    val JuzName_en: String,
    val Order: Int,
    val SurahMapping: String,
    val TotalAyat: Int
):Parcelable