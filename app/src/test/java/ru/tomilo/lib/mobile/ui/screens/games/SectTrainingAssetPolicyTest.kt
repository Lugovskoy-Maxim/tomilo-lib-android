package ru.tomilo.lib.mobile.ui.screens.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SectTrainingAssetPolicyTest {
    @Test fun localBundleAllowsOnlyItsOwnAssets() {
        assertEquals("sect-training/index.html", SectTrainingAssetPolicy.assetPath(SectTrainingAssetPolicy.ENTRY_URL))
        assertEquals("sect-training/assets/runtime/arena-a.js", SectTrainingAssetPolicy.assetPath("https://appassets.androidplatform.net/assets/sect-training/assets/runtime/arena-a.js"))
        listOf(
            "https://example.com/assets/sect-training/index.html",
            "http://appassets.androidplatform.net/assets/sect-training/index.html",
            "https://appassets.androidplatform.net/assets/sect-training/../secret",
            "https://appassets.androidplatform.net/assets/sect-training/%2e%2e/secret",
            "https://appassets.androidplatform.net/assets/other/index.html",
            "https://appassets.androidplatform.net:443/assets/sect-training/index.html",
            "https://appassets.androidplatform.net/assets/sect-training/index.html?host=web",
            "file:///android_asset/sect-training/index.html",
        ).forEach { assertNull(it, SectTrainingAssetPolicy.assetPath(it)) }
    }
}
