package io.geeteshk.sensor.view.ui

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import io.geeteshk.sensor.BuildConfig
import io.geeteshk.sensor.R
import kotlinx.android.synthetic.main.activity_splash.*
import timber.log.Timber
import java.util.*

/*
    Every thing in this activity is for sign-in/sign-up only.
    It does not contribute to BLE functionality
 */
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                startSignIn()
            }
        }, SPLASH_DELAY)
    }

    private fun startSignIn() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                        .setAvailableProviders(Arrays.asList(
                                AuthUI.IdpConfig.GoogleBuilder().build(),
                                AuthUI.IdpConfig.EmailBuilder().build()
                        )).build(),
                RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                when {
                    response == null -> showSnackbar("Sign in was cancelled by user")
                    response.error!!.errorCode == ErrorCodes.NO_NETWORK -> showSnackbar("Please connect to the internet and try again")
                    else -> {
                        showSnackbar("An unknown error occurred")
                        Timber.e("Sign-in error: ${response.error}")
                    }
                }
            }
        }
    }

    private fun showSnackbar(text: String) {
        Snackbar.make(splashLayout, text, Snackbar.LENGTH_INDEFINITE)
                .setAction("RETRY") {
                    startSignIn()
                }.show()
    }

    companion object {
        private const val SPLASH_DELAY: Long = 2000
        private const val RC_SIGN_IN = 123
    }
}
