package com.ludycom.arcgis_maps.entities.agml.models


import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn
import com.ludycom.arcgis_maps.activities.OAuthUserSignInActivity


private const val ACTIVITY_RESULT_REGISTRY_KEY = "KEY_ACTIVITY_RESULT_REGISTRY"


class OAuthUserSignInViewModel(
    private val getRegistry: () -> ActivityResultRegistry
) : ViewModel(), DefaultLifecycleObserver {

    private lateinit var oAuthLauncher : ActivityResultLauncher<OAuthUserSignIn>

    private var pendingSignIn: OAuthUserSignIn? = null

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        oAuthLauncher = getRegistry().register(
            ACTIVITY_RESULT_REGISTRY_KEY,
            owner,
            OAuthUserSignInActivity.Contract()
        ) { redirectUri ->
            complete(redirectUri)
        }
    }

    fun promptForOAuthUserSignIn(oAuthUserSignIn: OAuthUserSignIn) {
        pendingSignIn = oAuthUserSignIn
        oAuthLauncher.launch(pendingSignIn)
    }

    private fun complete(redirectUri: String?) {
        pendingSignIn?.let { pendingSignIn ->
            redirectUri?.let { redirectUri ->
                pendingSignIn.complete(redirectUri)
            } ?: pendingSignIn.cancel()
        } ?: throw IllegalStateException("OAuthUserSignIn not available for completion")
    }

    companion object {
        fun getFactory(
            getRegistry: () -> ActivityResultRegistry
        ) : ViewModelProvider.Factory  = viewModelFactory {
            initializer {
                OAuthUserSignInViewModel(getRegistry)
            }
        }
    }
}