package com.ludycom.arcgis_maps.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultRegistry
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.portal.Portal
import com.ludycom.arcgis_maps.entities.agml.models.OAuthUserSignInViewModel
import kotlinx.coroutines.launch


class OAuthUserSignMainActivity: AppCompatActivity() {

    private lateinit var oAuthConfiguration: OAuthUserConfiguration

    private val portal by lazy {
        Portal("https://ludycom.maps.arcgis.com/home/item.html?id=1a1c8686db25409b8f17da6beb163032", Portal.Connection.Authenticated)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val portalUrl = intent.getStringExtra("portalUrl")
        val clientId = intent.getStringExtra("clientId")
        val redirectUrl = intent.getStringExtra("redirectUrl")

        oAuthConfiguration = OAuthUserConfiguration(
            portalUrl = portalUrl!!,
            clientId = clientId!!,
            redirectUrl = redirectUrl!!
        )

        val oAuthUserSignInViewModel = ViewModelProvider(
            owner = this,
            factory = OAuthUserSignInViewModel.getFactory { activityResultRegistry }
        )[OAuthUserSignInViewModel::class.java]

        lifecycle.addObserver(oAuthUserSignInViewModel)

        setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel)

        lifecycleScope.launch {
            portal.load().onSuccess {
               Log.d("Success", it.toString())

            }.onFailure { throwable ->
                Log.e("Success", throwable.toString())

            }
        }
    }

    private fun setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel: OAuthUserSignInViewModel) {
        ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
            ArcGISAuthenticationChallengeHandler { challenge ->
                if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                    val oAuthUserCredential =
                        OAuthUserCredential.create(oAuthConfiguration) { oAuthUserSignIn ->
                            oAuthUserSignInViewModel.promptForOAuthUserSignIn(oAuthUserSignIn)
                        }.onSuccess {
                            val intentResult = Intent()
                            intentResult.putExtra("STATUS", "AUTHENTICATED")
                            setResult(RESULT_OK, intentResult)
                        }.onFailure {
                            val intentResult = Intent()
                            intentResult.putExtra("STATUS", "UNAUTHENTICATED")
                            setResult(RESULT_CANCELED, intentResult)
                        }.getOrThrow()
                    ArcGISAuthenticationChallengeResponse.ContinueWithCredential(oAuthUserCredential).also {
                        finish()
                    }
                } else {
                    val intentResult = Intent()
                    intentResult.putExtra("STATUS", "UNAUTHENTICATED")
                    setResult(RESULT_CANCELED, intentResult)

                    ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(
                        UnsupportedOperationException()
                    ).also {
                        finish()
                    }
                }
            }
        Log.d("INFO", "TEST Fabian")
    }

}