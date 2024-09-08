package com.deenislam.sdk.service.network.response.quran.qurannew.surah

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class TranslatedName(
    val language_name: String,
    val name: String
) : Parcelable