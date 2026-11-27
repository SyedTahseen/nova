package com.novavpn.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.novavpn.app.ui.theme.Nova
import java.util.Locale

/**
 * Round country flag for any ISO 3166 alpha-2 [code] ("GB", "de", …). Artwork lives in
 * res/drawable-nodpi/flag_<cc>.webp and is mapped in FlagRes.kt; unknown codes show an empty circle.
 */
@Composable
fun FlagBadge(code: String, size: Dp, modifier: Modifier = Modifier) {
    val res = FLAG_RES[code.uppercase(Locale.ROOT)]
    Box(modifier.size(size).clip(CircleShape).background(Nova.CardRaised)) {
        if (res != null) {
            Image(
                painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
