package com.novavpn.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.data.flagEmoji
import com.novavpn.app.ui.theme.Nova
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val flagCache = ConcurrentHashMap<String, ImageBitmap>()
private val flagMissing = ConcurrentHashMap.newKeySet<String>()

/** Loads assets/flags/<cc>.png (ISO 3166 alpha-2, lowercase). Returns null if that flag isn't bundled. */
private fun loadFlag(context: Context, key: String): ImageBitmap? {
    flagCache[key]?.let { return it }
    if (key in flagMissing) return null
    val bmp = runCatching {
        context.assets.open("flags/$key.png").use { BitmapFactory.decodeStream(it) }
    }.getOrNull()?.asImageBitmap()
    if (bmp == null) {
        flagMissing += key
        return null
    }
    flagCache[key] = bmp
    return bmp
}

/**
 * Round country flag for any ISO 3166 alpha-2 [code] ("GB", "de", …). Uses the bundled flag image when
 * present and falls back to the flag emoji otherwise, so a missing asset never breaks the UI.
 */
@Composable
fun FlagBadge(code: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val key = code.lowercase(Locale.ROOT)
    val bitmap by produceState(flagCache[key], key) {
        value = withContext(Dispatchers.IO) { loadFlag(context, key) }
    }
    Box(
        modifier.size(size).clip(CircleShape).background(Nova.CardRaised),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                flagEmoji(code),
                fontSize = (size.value * 1.15f).sp,
                modifier = Modifier.wrapContentSize(unbounded = true),
            )
        }
    }
}
