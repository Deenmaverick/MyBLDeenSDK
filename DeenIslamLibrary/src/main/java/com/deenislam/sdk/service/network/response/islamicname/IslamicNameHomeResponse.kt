package com.deenislam.sdk.service.network.response.islamicname


import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
@Keep
data class IslamicNameHomeResponse(
    @SerializedName("Data")
    var `data`: List<Data>,
    @SerializedName("Message")
    var message: String? = null,
    @SerializedName("Success")
    var success: Boolean? = null,
    @SerializedName("TotalData")
    var totalData: Int? = null
) {
    @Keep
    data class Data(
        @SerializedName("Design")
        var design: String? = null,
        @SerializedName("Id")
        var id: Int? = null,
        @SerializedName("Items")
        var items: List<Item>,
        @SerializedName("logo")
        var logo: Any? = null,
        @SerializedName("Sequence")
        var sequence: Int? = null,
        @SerializedName("Title")
        var title: String? = null
    ) {
        @Parcelize
        @Keep
        data class Item(
            @SerializedName("ArabicText")
            var arabicText: String,
            @SerializedName("CategoryId")
            var categoryId: Int? = null,
            @SerializedName("contentBaseUrl")
            var contentBaseUrl: String? = null,
            @SerializedName("ContentType")
            var contentType: String? = null,
            @SerializedName("DuaId")
            var duaId: Int? = null,
            @SerializedName("ECount")
            var eCount: String? = null,
            @SerializedName("FeatureID")
            var featureID: Int? = null,
            @SerializedName("FeatureName")
            var featureName: String? = null,
            @SerializedName("FeatureTitle")
            var featureTitle: String? = null,
            @SerializedName("HadithId")
            var hadithId: Int? = null,
            @SerializedName("Id")
            var id: Int? = null,
            @SerializedName("imageurl1")
            var imageurl1: String? = null,
            @SerializedName("imageurl2")
            var imageurl2: String? = null,
            @SerializedName("imageurl3")
            var imageurl3: String? = null,
            @SerializedName("imageurl4")
            var imageurl4: String? = null,
            @SerializedName("imageurl5")
            var imageurl5: String? = null,
            @SerializedName("IsActive")
            var isActive: String? = null,
            @SerializedName("JuzId")
            var juzId: Int? = null,
            @SerializedName("Language")
            var language: String? = null,
            @SerializedName("Logo")
            var logo: String? = null,
            @SerializedName("MText")
            var mText: String? = null,
            @SerializedName("Meaning")
            var meaning: String? = null,
            @SerializedName("Reference")
            var reference: String? = null,
            @SerializedName("Sequence")
            var sequence: Int? = null,
            @SerializedName("SubCategoryId")
            var subCategoryId: Int? = null,
            @SerializedName("SurahId")
            var surahId: Int? = null,
            @SerializedName("Text")
            var text: String? = null,
            @SerializedName("Title")
            var title: String? = null,
            @SerializedName("VerseId")
            var verseId: Int? = null
        ) : Parcelable
    }
}