package com.novavpn.app.ui.screens

import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.novavpn.app.ui.Press
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.NovaIconPill
import com.novavpn.app.ui.components.SectionLabel
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.pressable
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
            NovaIconPill(onClick = onBack, label = "Back", width = 64.dp) {
                ChevronIcon(Nova.White, Modifier.size(22.dp), left = true)
            }
            Text(
                "Settings",
                color = Nova.White,
                style = NovaType.ScreenTitle,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        SectionLabel("Connection")
        SettingsRow("Regions", "Choose a server location", onClick = onOpenRegions)
        SettingsRow("Test server speed", "Re-check ping for every region", onClick = onRefreshPings)

        SectionLabel("About")
        SettingsRow("Nova VPN", "Version $version", onClick = null)
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: (() -> Unit)?) {
    val shape = NovaShapes.Pill
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(shape, Press.Pill, onClick = onClick) else Modifier.clip(shape))
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, shape)
            .padding(start = 20.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Nova.White, style = NovaType.BodyStrong)
            Text(subtitle, color = Nova.TextMuted, style = NovaType.Caption, modifier = Modifier.padding(top = 2.dp))
        }
        if (onClick != null) ChevronIcon(Nova.TextMuted, Modifier.size(20.dp))
    }
}
