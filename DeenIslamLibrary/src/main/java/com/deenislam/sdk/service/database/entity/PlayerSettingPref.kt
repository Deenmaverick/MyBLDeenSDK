package com.deenislam.sdk.service.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playersettingpref")
internal data class PlayerSettingPref(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id", defaultValue = "1")
    var id:Int = 1,

    @ColumnInfo(name = "theme_font_size", defaultValue = "0.0")
    var theme_font_size:Float = 0F,

    @ColumnInfo(name = "arabic_font", defaultValue = "1")
    var arabic_font:Int = 1,

    @ColumnInfo(name = "auto_scroll", defaultValue = "1")
    var auto_scroll:Boolean = true,

    @ColumnInfo(name = "auto_play_next", defaultValue = "1")
    var auto_play_next:Boolean = true,

    @ColumnInfo(name = "recitation", defaultValue = "1")
    var recitation:Int = 1,

    @ColumnInfo(name = "transliteration", defaultValue = "1")
    var transliteration:Boolean = true,

    @ColumnInfo(name = "translation_font_size", defaultValue = "0.0")
    var translation_font_size:Float = 0F,

    @ColumnInfo(name = "translation_language", defaultValue = "bn")
    var translation_language:String = "bn",

    @ColumnInfo(name = "bn_translator", defaultValue = "0")
    var bn_translator:Int = 0,

    @ColumnInfo(name = "en_translator", defaultValue = "0")
    var en_translator:Int = 0,

    @ColumnInfo(name = "tafsir", defaultValue = "0")
    var tafsir:Int = 0,

)
