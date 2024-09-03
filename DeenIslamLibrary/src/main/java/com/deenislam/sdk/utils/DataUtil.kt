package com.deenislam.sdk.utils

import android.content.Context
import android.content.res.AssetManager
import com.deenislam.sdk.DeenSDKCore
import com.deenislam.sdk.R
import com.deenislam.sdk.service.models.quran.quranplayer.FontList
import com.deenislam.sdk.service.models.quran.quranplayer.PlayerCommonSelectionData
import com.deenislam.sdk.service.network.response.common.CommonCardData
import com.deenislam.sdk.service.network.response.dashboard.Item
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Qari
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.TafsirList
import com.deenislam.sdk.service.network.response.quran.qurangm.ayat.Translator
import com.deenislam.sdk.service.network.response.quran.qurangm.surahlist.Data
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

internal fun transformCommonCardListPatchModel(
    item: Item,
    itemTitle: String,
    itemSubTitle: String,
    itemMidContent:String="",
    itemBtnText: String
): Item {
    return Item(
        ArabicText = item.ArabicText,
        ContentType = item.ContentType,
        FeatureID = item.FeatureID,
        FeatureName = item.FeatureName,
        FeatureTitle = item.FeatureTitle,
        Id = item.Id,
        IsActive = item.IsActive,
        Language = item.Language,
        Reference = item.Reference,
        Sequence = item.Sequence,
        Text = item.Text,
        Title = item.Title,
        contentBaseUrl = item.contentBaseUrl,
        imageurl1 = item.imageurl1,
        imageurl2 = item.imageurl2,
        imageurl3 = item.imageurl3,
        imageurl4 = item.imageurl4,
        imageurl5 = item.imageurl5,
        SurahId = item.SurahId,
        JuzId = item.JuzId,
        VerseId = item.VerseId,
        HadithId = item.HadithId,
        CategoryId = item.CategoryId,
        SubCategoryId = item.SubCategoryId,
        DuaId = item.DuaId,
        MText = item.MText,
        Meaning = item.Meaning,
        ECount = item.ECount,
        itemTitle = itemTitle,
        itemSubTitle = itemSubTitle,
        itemBtnText = itemBtnText,
        isVideo = item.isVideo,
        isLive = item.isLive,
        FeatureSize = item.FeatureSize,
        itemMidContent = itemMidContent,
        isPremium = item.isPremium
    )
}

internal fun durationToSeconds(durationMillis: Long): Long {
    return TimeUnit.MILLISECONDS.toSeconds(durationMillis)
}

internal fun String.removeHtmlTags(): String {
    return this.replace(Regex("<.*?>"), "")
}

internal fun transformDashboardItemForKhatamQuran(
    item: Item
): CommonCardData {
    return CommonCardData(
        Id = item.Id,
        category = null,
        categoryID = item.CategoryId,
        duration = item.DuaId.toString(),
        imageurl = item.imageurl1,
        reference = item.Reference,
        referenceurl = null,
        title = item.ArabicText,
        videourl = item.imageurl2,
        buttonTxt = null,
        isPlaying = false,
        DurationInSec = item.DuaId,
        DurationWatched = 0,
        IsCompleted = false,
        customReference = null,
    )
}

fun String.monthShortNameLocale(): String =

    if(DeenSDKCore.GetDeenLanguage() == "bn") {

        val englishShortMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        val bengaliMonths = listOf(
            "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
            "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
        )

        val regex1 = "\\b(${englishShortMonths.joinToString("|")})\\b".toRegex()

        this.replace(regex1) { result ->
            val matchedMonth = result.value
            val index = englishShortMonths.indexOfFirst { it.equals(matchedMonth, ignoreCase = true) }
            if (index != -1) bengaliMonths[index] else matchedMonth
        }

    }
    else this

internal fun transformPatchToSurahData(newDataModel: Item): Data {

    return Data(
        ContentUrl= "",
        Id= newDataModel.Id,
        ImageUrl= "",
        IsFavorite = false,
        Order= newDataModel.Sequence,
        SurahId= newDataModel.SurahId,
        SurahName= newDataModel.MText,
        SurahNameInArabic= "",
        SurahNameMeaning= newDataModel.Meaning,
        SurahOrigin= newDataModel.Text,
        TotalAyat= newDataModel.ECount.toString(),
        rKey= "",
        wKey= ""
    )
}

internal fun transformPlayerReciterData(newDataModel: Qari): PlayerCommonSelectionData {

    return PlayerCommonSelectionData(
        imageurl = newDataModel.imageurl?:"",
        Id = newDataModel.title,
        title = newDataModel.text
    )
}

internal fun transformPlayerTranslatorData(newDataModel: Translator): PlayerCommonSelectionData {

    return PlayerCommonSelectionData(
        imageurl = newDataModel.imageurl,
        Id = newDataModel.title,
        title = newDataModel.text,
        language = newDataModel.language
    )
}

internal fun transformPlayerTafsirData(newDataModel: TafsirList): PlayerCommonSelectionData {

    return PlayerCommonSelectionData(
        imageurl = newDataModel.imageurl,
        Id = newDataModel.title,
        title = newDataModel.text,
        language = newDataModel.language
    )
}

internal fun Context.deleteSingleFile(fileAbsolutePath: String, fileName: String): Boolean {
    try {
        val file = File(filesDir, fileAbsolutePath)

        if (file.exists()) {
            val deleted = File(file, fileName).delete()
            // Optionally, delete the directory if it's empty after file deletion
            // file.delete()
            return deleted
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log an error message
        e.printStackTrace()
    }
    return false
}

internal fun Context.retrieveSingleFile(fileAbsolutePath: String): String? {
    try {
        val file = File(filesDir, fileAbsolutePath)

        if (file.exists()) {
            return file.readText()
        }
    } catch (e: Exception) {
        // Handle exceptions, e.g., log an error message
        e.printStackTrace()
    }
    return null
}

fun Context.loadHtmlFromAssets(filename: String): String {
    val assetManager: AssetManager = assets
    return try {
        val inputStream: InputStream = assetManager.open(filename)
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        String(buffer, StandardCharsets.UTF_8)
    } catch (e: IOException) {
        e.printStackTrace()
        ""
    }
}

internal fun Context.getArabicFontList():ArrayList<FontList> {

    return arrayListOf(
        FontList(
            fontname = this.getString(R.string.indopak),
            fontid = "1"
        ),
        FontList(
            fontname = this.getString(R.string.uthmanic_script_hafs_regular),
            fontid = "2"
        ),
        FontList(
            fontname = this.getString(R.string.al_majeed),
            fontid = "3"
        )

    )

}

internal fun transformArabicFontData(font: FontList): PlayerCommonSelectionData {

    return PlayerCommonSelectionData(
        imageurl = null,
        Id = font.fontid.toInt(),
        title = font.fontname
    )
}