package com.deenislam.sdk.service.network.response.quran.qurannew.surah

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class Chapter(
    val bismillah_pre: Boolean,
    val id: Int,
    val name_arabic: String,
    val name_complex: String,
    val name_simple: String,
    val pages: List<Int>,
    val revelation_order: Int,
    val revelation_place: String,
    val translated_name: TranslatedName,
    val verses_count: Int
) : Parcelable