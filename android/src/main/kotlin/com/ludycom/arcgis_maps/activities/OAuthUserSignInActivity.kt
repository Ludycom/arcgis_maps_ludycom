package com.ludycom.arcgis_maps.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Lifecycle
import com.arcgismaps.httpcore.authentication.OAuthUserSignIn

private const val AUTHORIZE_URL_KEY = "KEY_INTENT_EXTRA_AUTHORIZE_URL"
private const val CUSTOM_TABS_WAS_LAUNCHED_KEY = "KEY_INTENT_EXTRA_CUSTOM_TABS_WAS_LAUNCHED"
private const val OAUTH_RESPONSE_URI_KEY = "KEY_INTENT_EXTRA_OAUTH_RESPONSE_URI"
private const val REDIRECT_URL_KEY = "KEY_INTENT_EXTRA_REDIRECT_URL"

private const val RESULT_CODE_SUCCESS = 1
private const val RESULT_CODE_CANCELED = 2


class OAuthUserSignInActivity : AppCompatActivity() {

    private var customTabsWasLaunched = false
    private lateinit var redirectUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        redirectUrl = intent.getStringExtra(REDIRECT_URL_KEY).toString()

        if (savedInstanceState != null) {
            customTabsWasLaunched = savedInstanceState.getBoolean(
                CUSTOM_TABS_WAS_LAUNCHED_KEY
            )
        }

        if (!customTabsWasLaunched) {
            val authorizeUrl = intent.getStringExtra(AUTHORIZE_URL_KEY)
            authorizeUrl?.let {
                launchCustomTabs(it)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(CUSTOM_TABS_WAS_LAUNCHED_KEY, customTabsWasLaunched)
    }

    override fun onNewIntent(customTabsIntent: Intent) {
        super.onNewIntent(customTabsIntent)
        customTabsIntent.data?.let { uri ->
            val authorizationCode = uri.toString()
            if (authorizationCode.startsWith(redirectUrl)) {
                val intent = Intent().apply {
                    putExtra(OAUTH_RESPONSE_URI_KEY, authorizationCode)
                }
                setResult(RESULT_CODE_SUCCESS, intent)
            } else {
                setResult(RESULT_CODE_CANCELED, Intent())
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && lifecycle.currentState == Lifecycle.State.RESUMED) {
            setResult(RESULT_CODE_CANCELED, Intent())
            finish()
        }
    }

    private fun launchCustomTabs(authorizeUrl: String) {
        customTabsWasLaunched = true
        val intent = CustomTabsIntent.Builder().build().apply {
            intent.data = Uri.parse(authorizeUrl)
        }
        startActivity(intent.intent)
    }

    class Contract : ActivityResultContract<OAuthUserSignIn, String?>() {
        override fun createIntent(context: Context, input: OAuthUserSignIn) =
            run {
                Intent(context, OAuthUserSignInActivity::class.java).apply {
                    putExtra(AUTHORIZE_URL_KEY, input.authorizeUrl)
                    putExtra(REDIRECT_URL_KEY, input.oAuthUserConfiguration.redirectUrl)
                }
            }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (resultCode == RESULT_CODE_SUCCESS) {
                intent?.getStringExtra(OAUTH_RESPONSE_URI_KEY)
            } else {
                null
            }
        }
    }
}