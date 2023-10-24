package com.ludycom.arcgis_maps.pigeons

import AuthPigeon.OAuthUserConfigurations
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.ludycom.arcgis_maps.activities.OAuthUserSignMainActivity


private const val MAIN_ACTIVITY_RESULT_REGISTRY_KEY = 20

class AuthPigeonImpl(val context: Context?) : AuthPigeon.AuthApi {

    /*
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == MAIN_ACTIVITY_RESULT_REGISTRY_KEY) {
            if(resultCode == RESULT_OK) {

            } else {

            }
        }
    }
    */

    override fun oAuthUser(
        portalConfig: OAuthUserConfigurations,
        username: String,
        password: String
    ) {
        if(context == null) {
            //Result.failure(Exception("Error in context reference, may by null"));
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

        /*
        val intent = Intent(context, OAuthUserSignMainActivity::class.java)
        intent.putExtra("portalUrl", portalConfig.portalUrl)
        intent.putExtra("clientId", portalConfig.clientId)
        intent.putExtra("redirectUrl", portalConfig.redirectUrl)

        startActivityForResult(OAuthUserSignMainActivity(), intent, MAIN_ACTIVITY_RESULT_REGISTRY_KEY, null)
         */

        /*
        lateinit var oAuthLauncher : ActivityResultLauncher<OAuthUserSignIn>

        val getRegistry: () -> ActivityResultRegistry
        var pendingSignIn: OAuthUserSignIn? = null

        val oAuthConfiguration = OAuthUserConfiguration(
            portalUrl = portalConfig.portalUrl,
            clientId = portalConfig.clientId,
            redirectUrl = portalConfig.redirectUrl
        )
/*
        oAuthLauncher =
            register
            getRegistry().register(
            ACTIVITY_RESULT_REGISTRY_KEY,
            owner,
            OAuthUserSignInActivity.Contract()
        ) { redirectUri ->
            complete(redirectUri)
        }

 */

        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            ArcGISAuthenticationChallengeHandler { challenge ->
                if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                    val oAuthUserCredential = OAuthUserCredential.create(oAuthConfiguration) { oAuthUserSignIn ->
                        println(oAuthUserSignIn.authorizeUrl)

                    }.onSuccess { message ->
                            println(message)
                            //val intentResult = Intent()
                            //intentResult.putExtra("STATUS", "AUTHENTICATED")
                            //setResult(AppCompatActivity.RESULT_OK, intentResult)
                        }.onFailure { message ->
                            println(message)
                            //val intentResult = Intent()
                            //intentResult.putExtra("STATUS", "UNAUTHENTICATED")
                            //setResult(AppCompatActivity.RESULT_CANCELED, intentResult)
                        }.getOrThrow()
                    ArcGISAuthenticationChallengeResponse.ContinueWithCredential(oAuthUserCredential).also {
                        //finish()
                    }
                } else {
                    //val intentResult = Intent()
                    //intentResult.putExtra("STATUS", "UNAUTHENTICATED")
                    //setResult(AppCompatActivity.RESULT_CANCELED, intentResult)

                    ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(
                        UnsupportedOperationException()
                    ).also {
                        //finish()
                    }
                }
            }
         */
    }

    override fun setApiKey(apiKey: String) {
        ArcGISEnvironment.apiKey = ApiKey.create(apiKey)
    }
}