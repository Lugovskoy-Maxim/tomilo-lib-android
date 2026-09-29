package ru.tomilo.lib.mobile.ui.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdBindingResult
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdEventListener
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import ru.tomilo.lib.mobile.R
import ru.tomilo.lib.mobile.ads.AdUnits
import ru.tomilo.lib.mobile.ads.YandexAdsSdk

private const val TAG = "TomiloNativeCatalogAd"

/** Requests a single native ad after the user reaches the in-list placement. */
@Composable
fun rememberNativeCatalogAd(
    enabled: Boolean,
    shouldRequest: Boolean,
): NativeAd? {
    val context = LocalContext.current
    val loader = remember(context) { NativeAdLoader(context) }
    val disposed = remember(loader) { java.util.concurrent.atomic.AtomicBoolean(false) }
    var nativeAd by remember(loader) { mutableStateOf<NativeAd?>(null) }
    var requested by remember(loader) { mutableStateOf(false) }
    val currentAd by rememberUpdatedState(nativeAd)

    LaunchedEffect(enabled, shouldRequest) {
        if (!enabled || !shouldRequest || requested) return@LaunchedEffect
        requested = true
        // SDK инициализируется однократно и общим single-flight (YandexAdsSdk).
        YandexAdsSdk.initialize(context.applicationContext) {
            if (disposed.get()) return@initialize
            loader.loadAd(
                AdRequest.Builder(AdUnits.nativeCatalog).build(),
                object : NativeAdLoadListener {
                    override fun onAdLoaded(ad: NativeAd) {
                        Handler(Looper.getMainLooper()).post {
                            if (disposed.get()) {
                                ad.setNativeAdEventListener(null)
                            } else {
                                nativeAd = ad
                            }
                        }
                    }

                    override fun onAdFailedToLoad(error: AdRequestError) {
                        Log.w(TAG, "Native ad failed: ${error.code} ${error.description}")
                    }
                },
            )
        }
    }

    DisposableEffect(loader, disposed) {
        disposed.set(false)
        onDispose {
            disposed.set(true)
            loader.cancelLoading()
            currentAd?.setNativeAdEventListener(null)
        }
    }

    return nativeAd.takeIf { enabled }
}

@Composable
fun NativeCatalogAdCard(
    ad: NativeAd,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        factory = { context -> createNativeAdView(context, ad) },
    )
    DisposableEffect(ad) {
        onDispose { ad.setNativeAdEventListener(null) }
    }
}

private fun createNativeAdView(context: Context, ad: NativeAd): NativeAdView {
    val root = NativeAdView(context).apply {
        background = GradientDrawable().apply {
            setColor(AndroidColor.rgb(24, 24, 29))
            cornerRadius = dp(context, 22).toFloat()
            setStroke(dp(context, 1), AndroidColor.rgb(54, 54, 62))
        }
        clipToOutline = true
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 14))
    }
    root.addView(content, ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ))

    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val sponsored = text(context, 12, AndroidColor.rgb(255, 108, 99), bold = true).apply {
        text = "Реклама"
    }
    header.addView(sponsored, LinearLayout.LayoutParams(0, dp(context, 38), 1f))
    val feedback = ImageView(context).apply {
        setImageResource(android.R.drawable.ic_menu_more)
        contentDescription = "Информация о рекламе"
        setColorFilter(AndroidColor.LTGRAY)
        scaleType = ImageView.ScaleType.CENTER
    }
    header.addView(feedback, LinearLayout.LayoutParams(dp(context, 64), dp(context, 64)))
    content.addView(header)

    val title = text(context, 18, AndroidColor.WHITE, bold = true).apply {
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    content.addView(title, marginParams(context, height = ViewGroup.LayoutParams.WRAP_CONTENT, top = 2))

    val body = text(context, 14, AndroidColor.LTGRAY).apply {
        maxLines = 3
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    content.addView(body, marginParams(context, height = ViewGroup.LayoutParams.WRAP_CONTENT, top = 5))

    val media = MediaView(context)
    content.addView(media, marginParams(context, height = dp(context, 180), top = 10))

    val details = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val favicon = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    details.addView(favicon, LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)))
    val domain = text(context, 12, AndroidColor.LTGRAY).apply {
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    details.addView(domain, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginStart = dp(context, 8)
    })
    val icon = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
    details.addView(icon, LinearLayout.LayoutParams(dp(context, 40), dp(context, 40)).apply {
        marginStart = dp(context, 8)
    })
    val price = text(context, 13, AndroidColor.WHITE, bold = true)
    details.addView(price, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        marginStart = dp(context, 8)
    })
    content.addView(details, marginParams(context, height = ViewGroup.LayoutParams.WRAP_CONTENT, top = 9))

    val callToAction = text(context, 15, AndroidColor.WHITE, bold = true).apply {
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            setColor(AndroidColor.rgb(255, 91, 83))
            cornerRadius = dp(context, 14).toFloat()
        }
    }
    content.addView(callToAction, marginParams(context, height = dp(context, 48), top = 10))

    val warning = text(context, 11, AndroidColor.LTGRAY).apply { maxLines = 2 }
    content.addView(warning, marginParams(context, height = ViewGroup.LayoutParams.WRAP_CONTENT, top = 8))
    val age = text(context, 11, AndroidColor.LTGRAY).apply { maxLines = 1 }
    content.addView(age, marginParams(context, height = ViewGroup.LayoutParams.WRAP_CONTENT, top = 4))

    bindNativeAd(
        root = root,
        ad = ad,
        title = title,
        body = body,
        callToAction = callToAction,
        domain = domain,
        favicon = favicon,
        feedback = feedback,
        icon = icon,
        media = media,
        price = price,
        sponsored = sponsored,
        warning = warning,
        age = age,
    )
    return root
}

private fun bindNativeAd(
    root: NativeAdView,
    ad: NativeAd,
    title: TextView? = null,
    body: TextView? = null,
    callToAction: TextView? = null,
    domain: TextView? = null,
    favicon: ImageView? = null,
    feedback: ImageView? = null,
    icon: ImageView? = null,
    media: MediaView? = null,
    price: TextView? = null,
    sponsored: TextView? = null,
    warning: TextView? = null,
    age: TextView? = null,
) {
    val views = root.getTag(R.id.native_catalog_ad_views) as? NativeAdViews
    val bound = views ?: NativeAdViews(
        title = title ?: return,
        body = body ?: return,
        callToAction = callToAction ?: return,
        domain = domain ?: return,
        favicon = favicon ?: return,
        feedback = feedback ?: return,
        icon = icon ?: return,
        media = media ?: return,
        price = price ?: return,
        sponsored = sponsored ?: return,
        warning = warning ?: return,
        age = age ?: return,
    ).also { root.setTag(R.id.native_catalog_ad_views, it) }

    val binder = NativeAdViewBinder.Builder(root)
        .setTitleView(bound.title)
        .setBodyView(bound.body)
        .setAgeView(bound.age)
        .setCallToActionView(bound.callToAction)
        .setDomainView(bound.domain)
        .setFaviconView(bound.favicon)
        .setFeedbackView(bound.feedback)
        .setIconView(bound.icon)
        .setMediaView(bound.media)
        .setPriceView(bound.price)
        .setSponsoredView(bound.sponsored)
        .setWarningView(bound.warning)
        .build()

    when (val result = ad.bindNativeAd(binder)) {
        is AdBindingResult.Failure -> Log.e(TAG, "Native ad bind failed", result.exception)
        AdBindingResult.Success -> ad.setNativeAdEventListener(object : NativeAdEventListener {
            override fun onAdClicked() = Unit
            override fun onImpression(data: com.yandex.mobile.ads.common.ImpressionData?) = Unit
        })
    }
}

private data class NativeAdViews(
    val title: TextView,
    val body: TextView,
    val callToAction: TextView,
    val domain: TextView,
    val favicon: ImageView,
    val feedback: ImageView,
    val icon: ImageView,
    val media: MediaView,
    val price: TextView,
    val sponsored: TextView,
    val warning: TextView,
    val age: TextView,
)

private fun text(context: Context, sizeSp: Int, color: Int, bold: Boolean = false) =
    TextView(context).apply {
        textSize = sizeSp.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

private fun marginParams(context: Context, height: Int, top: Int) =
    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height).apply {
        topMargin = dp(context, top)
    }

private fun dp(context: Context, value: Int): Int =
    (value * context.resources.displayMetrics.density).toInt()
