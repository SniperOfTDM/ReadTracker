package com.sniperoftdm.booktracker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineFlavorGuardsTest {

    @Test
    fun offlineFlavor_doesNotRequestInternetPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        assertFalse(
            "Security guarantee violated: offline build must NOT request android.permission.INTERNET",
            requestedPermissions.contains("android.permission.INTERNET")
        )
    }

    @Test
    fun offlineFlavor_doesNotContainOkHttpOnClasspath() {
        try {
            Class.forName("okhttp3.OkHttpClient")
            fail("Security guarantee violated: OkHttp must NOT be present on classpath of offline build")
        } catch (expected: ClassNotFoundException) {
            // Expected
        }
    }
}
