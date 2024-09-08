package com.deenislam.sdk.service.network.response.common

import android.os.Parcelable
import androidx.annotation.Keep
import com.deenislam.sdk.utils.BASE_CONTENT_URL_SGP
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import kotlinx.parcelize.Parcelize

@Keep
@Parcelize
data class CommonCardData(
    val Id: Int = 0,
    val category: String? = null,
    val categoryID: Int = 0,
    val duration: String? = null,
    val imageurl: String? = null,
    val reference: String? = null,
    val referenceurl: String? = null,
    val title: String? = null,
    val videourl: String? = null,
    var buttonTxt:String ? = null,
    var isPlaying:Boolean = false,
    val isLive:Boolean = false,
    val DurationInSec: Int = 0,
    val DurationWatched: Int = 0,
    var IsCompleted: Boolean = false,
    var customReference: String ? = null
): Parcelable {
    fun toMediaItem() = MediaItem.Builder()
        .setUri(BASE_CONTENT_URL_SGP +videourl)
        .setMediaId(Id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setDescription(title)
                .build()
        )
        .build()
}

internal fun List<CommonCardData>.toMediaItems() = this.map { it.toMediaItem() }