package com.sniperoftdm.booktracker.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Preview(name = "1. Light 434dp (Default Poco X7 Pro)", widthDp = 434, showBackground = true)
@Preview(name = "2. Dark 434dp", widthDp = 434, uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "3. Light Compact 320dp", widthDp = 320, showBackground = true)
@Preview(name = "4. Light Wide 600dp", widthDp = 600, showBackground = true)
@Preview(name = "5. Font 1.3x 434dp", widthDp = 434, fontScale = 1.3f, showBackground = true)
@Preview(name = "6. Font 2.0x 434dp", widthDp = 434, fontScale = 2.0f, showBackground = true)
annotation class BookTrackerPreviews
