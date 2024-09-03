package com.deenislam.sdk.service.network.response.dashboard

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
internal data class Item(
    val ArabicText: String,
    val ContentType: String,
    val FeatureID: Int,
    val FeatureName: String,
    val FeatureTitle: String,
    val Id: Int,
    val IsActive: Boolean,
    val Language: String,
    val Reference: String,
    val Sequence: Int,
    val Text: String,
    val Title: String,
    val contentBaseUrl: String,
    val imageurl1: String,
    val imageurl2: String,
    val imageurl3: String,
    val imageurl4: String,
    val imageurl5: String,
    var SurahId: Int,
    val JuzId:Int,
    val VerseId:Int,
    val HadithId:Int,
    val CategoryId:Int,
    val SubCategoryId:Int,
    val DuaId:Int,
    val MText: String,
    val Meaning:String,
    val ECount:String,
    val itemTitle: String? = "",
    val itemMidContent: String? = "",
    val itemSubTitle: String? = "",
    val itemBtnText: String? = "",
    val FeatureLogo:String? = "",
    val FeatureButton:String ? = "",
    val isVideo:Boolean = false,
    val isLive:Boolean = false,
    val FeatureSize:String? = "",
    val Serial:Int = 0,
    val isPremium:Boolean = false
): Parcelable