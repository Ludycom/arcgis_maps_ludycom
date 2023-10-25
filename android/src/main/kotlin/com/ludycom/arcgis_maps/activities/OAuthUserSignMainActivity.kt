package com.ludycom.arcgis_maps.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeHandler
import com.arcgismaps.httpcore.authentication.ArcGISAuthenticationChallengeResponse
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.httpcore.authentication.OAuthUserCredential
import com.arcgismaps.portal.Portal
import com.ludycom.arcgis_maps.R
import com.ludycom.arcgis_maps.entities.agml.models.OAuthUserSignInViewModel
import com.ludycom.arcgis_maps.flutterBinaryMessenger
import kotlinx.coroutines.launch


class OAuthUserSignMainActivity: AppCompatActivity() {

    private lateinit var oAuthConfiguration: OAuthUserConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.o_auth_user_sign_main_activity)

        val portalUrl = intent.getStringExtra("portalUrl")
        val clientId = intent.getStringExtra("clientId")
        val redirectUrl = intent.getStringExtra("redirectUrl")

        oAuthConfiguration = OAuthUserConfiguration(
            portalUrl = portalUrl!!,
            clientId = clientId!!,
            redirectUrl = redirectUrl!!
        )

        val portal = Portal(portalUrl, Portal.Connection.Authenticated)

        val oAuthUserSignInViewModel = ViewModelProvider(
            owner = this,
            factory = OAuthUserSignInViewModel.getFactory { activityResultRegistry }
        )[OAuthUserSignInViewModel::class.java]

        lifecycle.addObserver(oAuthUserSignInViewModel)

        setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel)

        lifecycleScope.launch {
            portal.load().onSuccess {
                val intentResult = Intent()
                intentResult.putExtra("STATUS", "AUTHENTICATED")
                setResult(RESULT_OK, intentResult)
                AuthPigeon.AuthFlutterApi(flutterBinaryMessenger).oAuthUserState(true) {
                    println(it.toString())
                }
                finish()
            }.onFailure {
                val intentResult = Intent()
                intentResult.putExtra("STATUS", "UNAUTHENTICATED")
                setResult(RESULT_CANCELED, intentResult)
                AuthPigeon.AuthFlutterApi(flutterBinaryMessenger).oAuthUserState(true) {
                    println(it.toString())
                }
                finish()
            }
        }
    }

    private fun setUpArcGISAuthenticationChallengeHandler(oAuthUserSignInViewModel: OAuthUserSignInViewModel) {
        try {
            ArcGISEnvironment.authenticationManager.arcGISAuthenticationChallengeHandler =
                ArcGISAuthenticationChallengeHandler { challenge ->
                    if (oAuthConfiguration.canBeUsedForUrl(challenge.requestUrl)) {
                        val oAuthUserCredential =
                            OAuthUserCredential.create(oAuthConfiguration) { oAuthUserSignIn ->
                                oAuthUserSignInViewModel.promptForOAuthUserSignIn(oAuthUserSignIn)
                            }.onSuccess {
                                println(it)
                            }.onFailure {
                                println(it)
                            }.getOrThrow()
                        ArcGISAuthenticationChallengeResponse.ContinueWithCredential(oAuthUserCredential)
                    } else {
                        ArcGISAuthenticationChallengeResponse.ContinueAndFailWithError(
                            UnsupportedOperationException()
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e("setUpArcGISAuthenticationChallengeHandler", e.toString())
        }
    }

}