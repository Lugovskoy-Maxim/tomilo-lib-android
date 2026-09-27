package ru.tomilo.lib.mobile.ui.screens.premium

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent

/** Opens the bank-hosted payment page in a browser Custom Tab. */
class PaymentCheckoutActivity : ComponentActivity() {

    private var paymentUrl: Uri? = null
    private var invoiceId: String = ""
    private var customTabOpened = false
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        invoiceId = intent.getStringExtra(EXTRA_INV_ID).orEmpty()
        val rawUrl = intent.getStringExtra(EXTRA_PAYMENT_URL).orEmpty()
        paymentUrl = runCatching {
            val base = Uri.parse(rawUrl)
            if (base.scheme != "https" || base.host.isNullOrBlank()) null
            else base
        }.getOrNull()

        if (paymentUrl == null) {
            finishWith(RESULT_OPEN_ERROR)
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        if (finished) return

        if (customTabOpened) {
            // The user closed the bank page (or returned from a banking app).
            // The caller verifies the invoice state with the server.
            finishWith(RESULT_RETURNED)
            return
        }

        val url = paymentUrl ?: return
        customTabOpened = true
        runCatching {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(this, url)
        }.onFailure {
            finishWith(RESULT_OPEN_ERROR)
        }
    }

    private fun finishWith(status: String) {
        if (finished) return
        finished = true
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_INV_ID, invoiceId),
        )
        finish()
    }

    companion object {
        const val EXTRA_PAYMENT_URL = "payment_url"
        const val EXTRA_INV_ID = "inv_id"
        const val EXTRA_STATUS = "status"
        const val RESULT_SUCCESS = "success"
        const val RESULT_FAILED = "failed"
        const val RESULT_OPEN_ERROR = "open_error"
        const val RESULT_RETURNED = "returned"

        fun intent(context: Context, paymentUrl: String, invId: String): Intent =
            Intent(context, PaymentCheckoutActivity::class.java)
                .putExtra(EXTRA_PAYMENT_URL, paymentUrl)
                .putExtra(EXTRA_INV_ID, invId)
    }
}
