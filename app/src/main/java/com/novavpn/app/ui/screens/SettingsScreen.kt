package com.novavpn.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.theme.Nova

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenRegions: () -> Unit,
    onRefreshPings: () -> Unit,
) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Nova.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header: back pill + title
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pill = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .chipSurface(pill)
                    .clickable(onClickLabel = "Back", role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                ChevronIcon(Nova.White, Modifier.size(22.dp), left = true)
            }
            Text(
                "Settings",
                color = Nova.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Text(
            "Connection",
            color = Nova.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, top = 14.dp),
        )
        SettingsRow("Regions", "Choose a server location", onClick = onOpenRegions)
        SettingsRow("Test server speed", "Re-check ping for every region", onClick = onRefreshPings)

        Text(
            "About",
            color = Nova.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp, top = 14.dp),
        )
        SettingsRow("Nova VPN", "Version $version", onClick = null)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Nova.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Nova.TextMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) ChevronIcon(Nova.TextMuted, Modifier.size(20.dp))
    }
}
