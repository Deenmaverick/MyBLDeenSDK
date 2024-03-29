package com.deenislam.sdk.service.repository;

import com.deenislam.sdk.service.network.ApiCall
import com.deenislam.sdk.service.network.api.DeenService
import com.deenislam.sdk.utils.RequestBodyMediaType
import com.deenislam.sdk.utils.toRequestBody
import org.json.JSONObject

internal class HajjAndUmrahRepository(
    private val deenService: DeenService?
) : ApiCall {

    suspend fun getMakkahLiveVides(language:String) = makeApicall{
        val body = JSONObject()
        body.put("language", language)
        val requestBody = body.toString().toRequestBody(RequestBodyMediaType)

        deenService?.getMakkahLiveVideoes(parm = requestBody)

    }


} 