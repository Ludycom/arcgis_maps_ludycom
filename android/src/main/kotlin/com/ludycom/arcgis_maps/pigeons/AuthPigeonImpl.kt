package com.ludycom.arcgis_maps.pigeons

import AuthPigeon.OAuthUserConfigurations
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.ludycom.arcgis_maps.activities.OAuthUserSignMainActivity



class AuthPigeonImpl(private val context: Context?) : AuthPigeon.AGMLAuthApi {

    override fun oAuthUser(
        portalConfig: OAuthUserConfigurations,
        username: String,
        password: String
    ) {
        if(context == null) {
            Log.e("oAuthUser", ("Error in context reference, may by null"))
            return
        }

        val intent = Intent(
            context,
            OAuthUserSignMainActivity::class.java
        ).apply {
            putExtra("portalUrl", portalConfig.portalUrl)
            putExtra("clientId", portalConfig.clientId)
            putExtra("redirectUrl", portalConfig.redirectUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    override fun setApiKey(apiKey: String) {
        ArcGISEnvironment.apiKey = ApiKey.create(apiKey)
    }
}