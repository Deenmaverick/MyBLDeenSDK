package com.deenislam.sdk.service.repository;

import com.deenislam.sdk.service.network.ApiCall
import com.deenislam.sdk.service.network.ApiResource
import com.deenislam.sdk.service.network.api.DeenService
import com.deenislam.sdk.service.network.response.common.subcatcardlist.SubCatResponse
import com.deenislam.sdk.utils.HAJJ_GUIDE
import com.deenislam.sdk.utils.HAJJ_SUB_CAT
import com.deenislam.sdk.utils.MENU_ISLAMIC_EVENT
import com.deenislam.sdk.utils.MENU_PRAYER_LEARNING
import com.deenislam.sdk.utils.RequestBodyMediaType
import com.deenislam.sdk.utils.toRequestBody
import org.json.JSONObject

internal class SubCatCardListRepository(
    private val deenService: DeenService?
) : ApiCall {

    suspend fun getSubCatList(categoryID: Int, language: String, tag: String): ApiResource<SubCatResponse?>? {
        return when(tag)
        {
            HAJJ_SUB_CAT ->  getHajjAndUmrahSubCat(categoryID,language)
            MENU_PRAYER_LEARNING -> getPrayerLeareningSubCat(language,categoryID)
            MENU_ISLAMIC_EVENT -> getIslamicEeventSubCat(language,categoryID)
            HAJJ_GUIDE ->  getHajjGuide(language,categoryID)
            else -> null
        }
    }

    private suspend fun getHajjAndUmrahSubCat(categoryID:Int, language:String) = makeApicall{
        val body = JSONObject()
        body.put("Category", categoryID)
        body.put("language", language)
        val requestBody = body.toString().toRequestBody(RequestBodyMediaType)

        deenService?.getHajjAndUmrahSubCat(parm = requestBody)

    }

    private suspend fun getPrayerLeareningSubCat(language:String, cat:Int) = makeApicall {
        val body = JSONObject()
        body.put("language", language)
        body.put("category", cat)
        val requestBody = body.toString().toRequestBody(RequestBodyMediaType)
        deenService?.getPrayerLearningSubCat(parm = requestBody)

    }
    private suspend fun getIslamicEeventSubCat(language:String,cat:Int) = makeApicall {
        val body = JSONObject()
        body.put("Language", language)
        body.put("Category", cat)
        val requestBody = body.toString().toRequestBody(RequestBodyMediaType)
        deenService?.getIslamicEventSubCat(parm = requestBody)

    }

    private suspend fun getHajjGuide(language:String,cat:Int) = makeApicall {
        val body = JSONObject()
        body.put("Language", language)
        body.put("Category", cat)
        val requestBody = body.toString().toRequestBody(RequestBodyMediaType)
        deenService?.getHajjGuide(parm = requestBody)

    }

} 