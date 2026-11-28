package com.novavpn.app.ui.screens

import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaType
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novavpn.app.GroupMode
import com.novavpn.app.MainViewModel
import com.novavpn.app.SortMode
import com.novavpn.app.core.ManualServer
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.EmptyStateCard
import com.novavpn.app.ui.components.NovaChipRow
import com.novavpn.app.ui.components.NovaDialog
import com.novavpn.app.ui.components.NovaFieldLabel
import com.novavpn.app.ui.components.NovaIconPill
import com.novavpn.app.ui.components.NovaDropdownMenu
import com.novavpn.app.ui.components.NovaMenuBack
import com.novavpn.app.ui.components.NovaMenuItem
import com.novavpn.app.ui.components.NovaSubMenuItem
import com.novavpn.app.ui.components.NovaTextField
import com.novavpn.app.ui.components.SectionHeader
import com.novavpn.app.ui.components.CheckIcon
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.CloseIcon
import com.novavpn.app.ui.components.MoreIcon
import com.novavpn.app.ui.components.OrbView
import com.novavpn.app.ui.components.PlusIcon
import com.novavpn.app.ui.components.SearchIcon
import com.novavpn.app.ui.components.SignalIcon
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.scanQrCode
import com.novavpn.app.ui.theme.Nova

private data class Confirm(val title: String, val message: String, val action: String, val run: () -> Unit)

private data class Section(val title: String?, val items: List<Region>)

private const val UNKNOWN_COUNTRY = "Unknown location"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegionsScreen(
    vm: MainViewModel,
    onSelect: (Region) -> Unit,
    onMoreRegions: () -> Unit,
    startWithAdd: Boolean = false,
    /** True while the screen is still sliding in: the list is left empty and fades in afterwards. */
    deferList: Boolean = false,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val regions by vm.regions.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val pings by vm.pings.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val groupMode by vm.groupMode.collectAsStateWithLifecycle()
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var showSub by rememberSaveable { mutableStateOf(false) }
    var subUrl by rememberSaveable { mutableStateOf("") }
    var subError by remember { mutableStateOf<String?>(null) }
    var showManual by rememberSaveable { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Confirm?>(null) }

    // Closes the search field first; a second Back closes the screen (handled in NovaApp).
    BackHandler(enabled = searching) { searching = false; query = "" }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun handleImport(text: String) {
        val r = vm.importText(text)
        when {
            r.subscriptionUrl != null -> { subUrl = r.subscriptionUrl; subError = null; showSub = true }
            r.added > 0 -> toast("Added ${r.added} server${if (r.added > 1) "s" else ""}")
            r.duplicates > 0 -> toast("Already in your list")
            else -> toast(r.error ?: "Nothing to import")
        }
    }

    val q = query.trim().lowercase()
    val source: List<Region> = if (deferList) emptyList<Region>() else regions
    val filtered = if (q.isEmpty()) source else source.filter {
        it.label.lowercase().contains(q) || it.code.lowercase() == q ||
            it.id.lowercase().contains(q) || it.group.lowercase().contains(q) ||
            it.protocolName.lowercase().contains(q)
    }

    val sections = remember(filtered, groupMode, sort, pings, subs) {
        buildSections(filtered, groupMode, sort, pings, subs.map { it.id })
    }

    val unreachable = remember(regions, pings) { vm.unreachableIds() }
    val duplicates = remember(regions) { vm.duplicateIds() }
    val manualCount = regions.count { it.custom && it.subId == null }
    val subServerCount = regions.count { it.subId != null }
    val customCount = regions.count { it.custom }

    @Composable
    fun ServerRow(r: Region) {
        RegionRow(
            r, r.id == selectedId, pings[r.id],
            onClick = { onSelect(r) },
            onCopy = { clipboard.setText(AnnotatedString(r.link)); toast("Link copied") },
            onDelete = if (r.custom) ({ vm.deleteServer(r.id); toast("Server removed") }) else null,
        )
    }

    fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

    val menu = MoreMenu(
        sort = sort,
        groupMode = groupMode,
        hasSubs = subs.isNotEmpty(),
        unreachable = unreachable.size,
        duplicates = duplicates.size,
        manual = manualCount,
        subscriptions = subs.size,
        custom = customCount,
        onTestAll = { vm.refreshPings(); toast("Testing all servers…") },
        onUpdateSubs = { toast("Updating subscriptions…"); vm.refreshSubscriptions { toast(it) } },
        onSort = vm::setSort,
        onGroup = vm::setGroupMode,
        onDeleteUnreachable = {
            confirm = Confirm(
                "Delete unreachable servers?",
                "${plural(unreachable.size, "server")} that failed the last test will be removed.",
                "Delete",
            ) { vm.deleteIds(unreachable) }
        },
        onDeleteDuplicates = {
            confirm = Confirm(
                "Delete duplicates?",
                "${plural(duplicates.size, "extra copy")} of servers with the same address and credentials will be removed.",
                "Delete",
            ) { vm.deleteIds(duplicates) }
        },
        onDeleteManual = {
            confirm = Confirm(
                "Delete manual servers?",
                "This removes the ${plural(manualCount, "server")} you added by hand. Subscriptions are not affected.",
                "Delete",
            ) { vm.deleteManual() }
        },
        onDeleteSubs = {
            confirm = Confirm(
                "Delete subscriptions?",
                "This removes ${plural(subs.size, "subscription")} and their ${plural(subServerCount, "server")}.",
                "Delete",
            ) { vm.deleteSubscriptions() }
        },
        onDeleteAll = {
            confirm = Confirm(
                "Delete all servers?",
                "This removes all ${plural(customCount, "server")} and subscriptions you added.",
                "Delete all",
            ) { vm.deleteAllCustom() }
        },
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Nova.Black)
            .statusBarsPadding()
            .imePadding(),
    ) {
        RegionsHeader(
            searching = searching,
            query = query,
            menu = menu,
            openAddMenuInitially = startWithAdd,
            onQueryChange = { query = it },
            onOpenSearch = { searching = true },
            onCloseSearch = { searching = false; query = "" },
            onSubscription = { subUrl = ""; subError = null; showSub = true },
            onClipboard = { handleImport(clipboard.getText()?.text.orEmpty()) },
            onQr = { scanQrCode(context, onResult = ::handleImport, onError = ::toast) },
            onManual = { showManual = true },
        )

        val listAlpha by animateFloatAsState(if (deferList) 0f else 1f, tween(220), label = "listAlpha")
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = listAlpha },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!searching && q.isEmpty()) {
                item { Banner(regions, onMoreRegions) }
                item { Spacer(Modifier.height(4.dp)) }
            }

            if (deferList) {
                // nothing yet
            } else if (regions.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No servers yet",
                        message = "Add a vless, vmess, trojan, ss, socks or wireguard link. The flag and location are taken from the proxy name, e.g. Germany.",
                        actionLabel = "Add server",
                        onAction = { showManual = true },
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "No servers match \"${query.trim()}\"",
                        color = Nova.TextMuted,
                        style = NovaType.Body,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    )
                }
            }

            sections.forEach { section ->
                if (section.title != null) {
                    item(key = "header-${section.title}") { SectionHeader(section.title, section.items.size) }
                }
                items(section.items, key = { it.id }) { r -> ServerRow(r) }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (showSub) {
        SubscriptionDialog(
            url = subUrl,
            busy = busy,
            error = subError,
            onUrlChange = { subUrl = it; subError = null },
            onPaste = { clipboard.getText()?.text.orEmpty().trim() },
            onSubmit = { name, url ->
                subError = null
                vm.addSubscription(name, url) { err ->
                    if (err == null) { showSub = false; toast("Subscription added") } else subError = err
                }
            },
            onDismiss = { if (!busy) showSub = false },
        )
    }

    if (showManual) {
        ManualDialog(
            onDismiss = { showManual = false },
            onPaste = { clipboard.getText()?.text.orEmpty().trim() },
            onAddLink = { link, name ->
                val err = vm.addServer(link, name)
                if (err == null) { showManual = false; toast("Server added") }
                err
            },
        )
    }

    confirm?.let { c ->
        NovaDialog(
            onDismissRequest = { confirm = null },
            title = c.title,
            text = { Text(c.message, color = Nova.TextMuted) },
            confirmButton = {
                TextButton(onClick = { c.run(); confirm = null }) { Text(c.action, color = Nova.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) { Text("Cancel", color = Nova.White) }
            },
        )
    }
}

/** Splits [list] into sections for the chosen grouping, sorting inside each section. */
private fun buildSections(
    list: List<Region>,
    mode: GroupMode,
    sort: SortMode,
    pings: Map<String, Int?>,
    subOrder: List<String>,
): List<Section> {
    fun sorted(l: List<Region>): List<Region> = when (sort) {
        SortMode.Default -> l
        SortMode.Name -> l.sortedBy { it.label.lowercase() }
        // Untested / unreachable servers go last.
        SortMode.Ping -> l.sortedBy { pings[it.id] ?: Int.MAX_VALUE }
    }

    if (list.isEmpty()) return emptyList()

    val sections: List<Section> = when (mode) {
        GroupMode.None -> listOf(Section(null, sorted(list)))

        GroupMode.Country -> list.groupBy { it.country.ifBlank { UNKNOWN_COUNTRY } }
            .entries.sortedWith(compareBy({ it.key == UNKNOWN_COUNTRY }, { it.key.lowercase() }))
            .map { Section(it.key, sorted(it.value)) }

        GroupMode.Protocol -> list.groupBy { it.protocolName }
            .entries.sortedBy { it.key.lowercase() }
            .map { Section(it.key, sorted(it.value)) }

        GroupMode.Source -> {
            fun title(r: Region) = when {
                r.subId != null -> r.group
                r.custom -> r.group.ifBlank { "My servers" }
                r.premium -> "Premium regions"
                else -> "Free regions"
            }
            fun rank(r: Region) = when {
                r.subId != null -> 1 + subOrder.indexOf(r.subId).coerceAtLeast(0)
                r.custom -> 0
                r.premium -> 100_001
                else -> 100_000
            }
            list.groupBy { title(it) }
                .entries.sortedBy { e -> rank(e.value.first()) }
                .map { Section(it.key, sorted(it.value)) }
        }
    }
    // A lone section doesn't need a heading.
    return if (sections.size == 1) listOf(sections[0].copy(title = null)) else sections
}

/** Everything the three-dot menu needs. */
private class MoreMenu(
    val sort: SortMode,
    val groupMode: GroupMode,
    val hasSubs: Boolean,
    val unreachable: Int,
    val duplicates: Int,
    val manual: Int,
    val subscriptions: Int,
    val custom: Int,
    val onTestAll: () -> Unit,
    val onUpdateSubs: () -> Unit,
    val onSort: (SortMode) -> Unit,
    val onGroup: (GroupMode) -> Unit,
    val onDeleteUnreachable: () -> Unit,
    val onDeleteDuplicates: () -> Unit,
    val onDeleteManual: () -> Unit,
    val onDeleteSubs: () -> Unit,
    val onDeleteAll: () -> Unit,
)

/** Title + search / add / overflow pills. The search pill expands into a text field. */
@Composable
private fun RegionsHeader(
    searching: Boolean,
    query: String,
    menu: MoreMenu,
    openAddMenuInitially: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSubscription: () -> Unit,
    onClipboard: () -> Unit,
    onQr: () -> Unit,
    onManual: () -> Unit,
) {
    val pill = NovaShapes.Pill
    var moreOpen by remember { mutableStateOf(false) }
    // Which page of the three-dot menu is showing: "root", "sort", "group" or "delete".
    var page by remember { mutableStateOf("root") }
    var addOpen by remember { mutableStateOf(openAddMenuInitially) }

    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (searching) {
            val focus = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(Unit) { focus.requestFocus() }

            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .chipSurface(pill)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchIcon(Nova.TextMuted, Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Search regions", color = Nova.TextMuted, style = NovaType.Body)
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = NovaType.Body.copy(color = Nova.White),
                        cursorBrush = SolidColor(Nova.Lime),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Close search", role = Role.Button, onClick = onCloseSearch),
                    contentAlignment = Alignment.Center,
                ) { CloseIcon(Nova.White, Modifier.size(18.dp)) }
            }
        } else {
            Text(
                "Regions",
                color = Nova.White,
                style = NovaType.ScreenTitle,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            NovaIconPill(onClick = onOpenSearch, label = "Search") { SearchIcon(Nova.White) }

            Box {
                NovaIconPill(onClick = { addOpen = true }, label = "Add server") { PlusIcon(Nova.White) }
                NovaDropdownMenu(
                    expanded = addOpen,
                    onDismissRequest = { addOpen = false },
                ) {
                    NovaMenuItem("Subscription URL") { addOpen = false; onSubscription() }
                    NovaMenuItem("Import from clipboard") { addOpen = false; onClipboard() }
                    NovaMenuItem("Scan QR code") { addOpen = false; onQr() }
                    NovaMenuItem("Manual input") { addOpen = false; onManual() }
                }
            }
        }

        Box {
            NovaIconPill(onClick = { page = "root"; moreOpen = true }, label = "More options") { MoreIcon(Nova.White) }
            NovaDropdownMenu(
                expanded = moreOpen,
                onDismissRequest = { moreOpen = false },
            ) {
                // The page is reset when the menu is next opened, so it does not flip back while fading out.
                fun close(block: () -> Unit): () -> Unit = { moreOpen = false; block() }

                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        (fadeIn(tween(180, delayMillis = 70)))
                            .togetherWith(fadeOut(tween(80)))
                            .using(SizeTransform(clip = true) { _, _ -> tween(240, easing = FastOutSlowInEasing) })
                    },
                    label = "menuPage",
                ) { p ->
                Column(Modifier.fillMaxWidth()) {
                when (p) {
                    "sort" -> {
                        NovaMenuBack("Sort") { page = "root" }
                        NovaMenuItem("Default order", checked = menu.sort == SortMode.Default, onClick = close { menu.onSort(SortMode.Default) })
                        NovaMenuItem("Fastest first", checked = menu.sort == SortMode.Ping, onClick = close { menu.onSort(SortMode.Ping) })
                        NovaMenuItem("Name A–Z", checked = menu.sort == SortMode.Name, onClick = close { menu.onSort(SortMode.Name) })
                    }

                    "group" -> {
                        NovaMenuBack("Group by") { page = "root" }
                        NovaMenuItem("Source", checked = menu.groupMode == GroupMode.Source, onClick = close { menu.onGroup(GroupMode.Source) })
                        NovaMenuItem("Country", checked = menu.groupMode == GroupMode.Country, onClick = close { menu.onGroup(GroupMode.Country) })
                        NovaMenuItem("Protocol", checked = menu.groupMode == GroupMode.Protocol, onClick = close { menu.onGroup(GroupMode.Protocol) })
                        NovaMenuItem("No grouping", checked = menu.groupMode == GroupMode.None, onClick = close { menu.onGroup(GroupMode.None) })
                    }

                    "delete" -> {
                        NovaMenuBack("Delete") { page = "root" }
                        if (menu.unreachable > 0) {
                            NovaMenuItem("Unreachable (${menu.unreachable})", color = Nova.Red, onClick = close(menu.onDeleteUnreachable))
                        }
                        if (menu.duplicates > 0) {
                            NovaMenuItem("Duplicates (${menu.duplicates})", color = Nova.Red, onClick = close(menu.onDeleteDuplicates))
                        }
                        if (menu.manual > 0) {
                            NovaMenuItem("Manual servers (${menu.manual})", color = Nova.Red, onClick = close(menu.onDeleteManual))
                        }
                        if (menu.subscriptions > 0) {
                            NovaMenuItem("Subscriptions (${menu.subscriptions})", color = Nova.Red, onClick = close(menu.onDeleteSubs))
                        }
                        NovaMenuItem("All my servers", color = Nova.Red, onClick = close(menu.onDeleteAll))
                    }

                    else -> {
                        NovaMenuItem("Test all servers", onClick = close(menu.onTestAll))
                        if (menu.hasSubs) NovaMenuItem("Update subscriptions", onClick = close(menu.onUpdateSubs))
                        NovaSubMenuItem("Sort") { page = "sort" }
                        NovaSubMenuItem("Group by") { page = "group" }
                        if (menu.custom > 0) NovaSubMenuItem("Delete", color = Nova.Red) { page = "delete" }
                    }
                }
                }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDialog(
    url: String,
    busy: Boolean,
    error: String?,
    onUrlChange: (String) -> Unit,
    onPaste: () -> String,
    onSubmit: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    NovaDialog(
        onDismissRequest = onDismiss,
        title = "Add subscription",
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Paste the subscription URL from your provider. Its servers are added as a group and can be updated later.",
                    color = Nova.TextMuted,
                    style = NovaType.Secondary,
                )
                NovaTextField(url, onUrlChange, "https://…", singleLine = false, keyboardType = KeyboardType.Uri)
                NovaTextField(name, { name = it }, "Name (optional)", singleLine = true, keyboardType = KeyboardType.Text)
                TextButton(onClick = { onUrlChange(onPaste()) }, enabled = !busy) {
                    Text("Paste from clipboard", color = Nova.Lime)
                }
                if (busy) Text("Downloading…", color = Nova.TextMuted, style = NovaType.Caption)
                error?.let { Text(it, color = Nova.Red, style = NovaType.Caption) }
            }
        },
        confirmButton = {
            val ok = url.isNotBlank() && !busy
            TextButton(onClick = { onSubmit(name, url) }, enabled = ok) {
                Text("Add", color = if (ok) Nova.Lime else Nova.Gray)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel", color = Nova.White) } },
    )
}

@Composable
private fun ManualDialog(
    onDismiss: () -> Unit,
    onPaste: () -> String,
    onAddLink: (link: String, name: String) -> String?,
) {
    var tab by rememberSaveable { mutableStateOf("Link") }
    var link by rememberSaveable { mutableStateOf("") }
    var linkName by rememberSaveable { mutableStateOf("") }
    var f by remember { mutableStateOf(ManualServer()) }
    var error by remember { mutableStateOf<String?>(null) }

    val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks", "SOCKS")
    val transportProtocol = f.protocol == "VLESS" || f.protocol == "VMess" || f.protocol == "Trojan"
    val securities = if (f.protocol == "VMess") listOf("none", "tls") else listOf("none", "tls", "reality")

    NovaDialog(
        onDismissRequest = onDismiss,
        title = "Manual input",
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NovaChipRow(listOf("Link", "Fields"), tab) { tab = it; error = null }

                if (tab == "Link") {
                    Text(
                        "Paste a vless, vmess, trojan, ss, socks or wireguard link.",
                        color = Nova.TextMuted,
                        style = NovaType.Secondary,
                    )
                    NovaTextField(link, { link = it; error = null }, "vless://, vmess://, trojan://, ss://…", false, KeyboardType.Uri)
                    NovaTextField(linkName, { linkName = it }, "Name (optional)", true, KeyboardType.Text)
                    TextButton(onClick = { link = onPaste(); error = null }) {
                        Text("Paste from clipboard", color = Nova.Lime)
                    }
                } else {
                    NovaChipRow(protocols, f.protocol) { p ->
                        f = f.copy(
                            protocol = p,
                            network = "tcp",
                            security = if (p == "Trojan") "tls" else "none",
                            port = if (p == "SOCKS") "1080" else if (p == "Shadowsocks") "8388" else "443",
                        )
                        error = null
                    }
                    NovaTextField(f.name, { f = f.copy(name = it) }, "Name, e.g. Germany", true, KeyboardType.Text)
                    NovaTextField(f.host, { f = f.copy(host = it); error = null }, "Address", true, KeyboardType.Uri)
                    NovaTextField(f.port, { f = f.copy(port = it); error = null }, "Port", true, KeyboardType.Number)

                    val secretHint = when (f.protocol) {
                        "VLESS", "VMess" -> "User ID (UUID)"
                        else -> "Password"
                    }
                    NovaTextField(f.secret, { f = f.copy(secret = it); error = null }, secretHint, true, KeyboardType.Text)

                    when {
                        f.protocol == "Shadowsocks" -> {
                            NovaFieldLabel("Cipher")
                            NovaChipRow(
                                listOf("aes-256-gcm", "aes-128-gcm", "chacha20-ietf-poly1305"),
                                f.cipher,
                            ) { f = f.copy(cipher = it) }
                        }

                        f.protocol == "SOCKS" -> {
                            NovaTextField(f.username, { f = f.copy(username = it) }, "Username (optional)", true, KeyboardType.Text)
                        }

                        transportProtocol -> {
                            NovaFieldLabel("Transport")
                            NovaChipRow(listOf("tcp", "ws", "grpc", "xhttp"), f.network) { f = f.copy(network = it) }
                            NovaFieldLabel("Security")
                            NovaChipRow(securities, f.security) { f = f.copy(security = it); error = null }
                            if (f.security != "none") {
                                NovaTextField(f.sni, { f = f.copy(sni = it) }, "SNI / host", true, KeyboardType.Uri)
                            }
                            if (f.network != "tcp") {
                                NovaTextField(
                                    f.path,
                                    { f = f.copy(path = it) },
                                    if (f.network == "grpc") "Service name" else "Path, e.g. /ws",
                                    true,
                                    KeyboardType.Uri,
                                )
                            }
                            if (f.security == "reality") {
                                NovaTextField(f.publicKey, { f = f.copy(publicKey = it); error = null }, "Reality public key", true, KeyboardType.Text)
                                NovaTextField(f.shortId, { f = f.copy(shortId = it) }, "Short ID (optional)", true, KeyboardType.Text)
                            }
                            if (f.protocol == "VLESS" && f.network == "tcp" && f.security != "none") {
                                NovaFieldLabel("Flow")
                                NovaChipRow(listOf("none", "xtls-rprx-vision"), if (f.vision) "xtls-rprx-vision" else "none") {
                                    f = f.copy(vision = it != "none")
                                }
                            }
                        }
                    }
                }
                error?.let { Text(it, color = Nova.Red, style = NovaType.Caption) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = if (tab == "Link") {
                    onAddLink(link, linkName)
                } else {
                    try {
                        onAddLink(f.toLink(), "")
                    } catch (e: IllegalArgumentException) {
                        e.message ?: "Check the fields"
                    }
                }
            }) { Text("Add", color = Nova.Lime) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nova.White) } },
    )
}

@Composable
private fun Banner(regions: List<Region>, onClick: () -> Unit) {
    val shape = NovaShapes.Card
    Box(
        Modifier
            .fillMaxWidth()
            .height(213.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    0f to Nova.BannerBase,
                    1f to Nova.BannerTint,
                ),
            )
            .clickable(onClick = onClick),
    ) {
        // Orb peeking in from the right edge
        OrbView(
            Modifier
                .requiredSize(338.dp) // sphere radius ≈ 128dp, centred on the card's right edge
                .align(Alignment.CenterEnd)
                .offset(x = 166.dp, y = (-4).dp),
        )

        Text(
            "Need more\nregions?",
            color = Nova.White,
            style = NovaType.Headline,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 13.dp, top = 12.dp),
        )

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(65.dp)
                .clip(NovaShapes.Pill)
                .background(Nova.White),
            contentAlignment = Alignment.Center,
        ) {
            ChevronIcon(Nova.Black, Modifier.size(22.dp))
        }

        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val flags = regions.take(2).map { it.code }
            Box(Modifier.width(72.dp).height(28.dp)) {
                flags.forEachIndexed { i, code ->
                    FlagBadge(
                        code, 28,
                        Modifier.offset(x = (i * 22).dp).border(1.5.dp, Nova.BannerBase, CircleShape),
                    )
                }
                Box(
                    Modifier
                        .offset(x = (flags.size * 22).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Nova.White)
                        .border(1.5.dp, Nova.BannerBase, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Nova.Black, style = NovaType.ButtonBold) }
            }
            Text(
                "Europe, USA, Canada",
                color = Nova.TextMuted,
                style = NovaType.Body,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RegionRow(
    region: Region,
    selected: Boolean,
    pingMs: Int?,
    onClick: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val shape = NovaShapes.Pill
    val bg = if (selected) Nova.Lime else Nova.Card
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onCopy != null || onDelete != null
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Row sinks slightly while pressed and while its menu is open, so the long press feels acknowledged.
    val pressScale by animateFloatAsState(
        if (pressed || menuOpen) 0.97f else 1f,
        tween(160, easing = FastOutSlowInEasing),
        label = "rowPress",
    )

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clip(shape)
                .background(bg)
                .then(if (selected) Modifier else Modifier.border(1.dp, Nova.CardBorder, shape))
                .combinedClickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = if (hasMenu) ({
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    }) else null,
                    onLongClickLabel = if (hasMenu) "Server options" else null,
                )
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlagBadge(region.code, 30, Modifier.padding(end = 12.dp))
            Text(
                region.label,
                color = if (selected) Nova.Black else Nova.White.copy(alpha = 0.78f),
                style = NovaType.Secondary,
                modifier = Modifier.weight(1f),
            )
            if (pingMs != null) {
                Text(
                    "$pingMs ms",
                    color = if (selected) Nova.Black.copy(alpha = 0.7f) else Nova.TextMuted,
                    style = NovaType.Small,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            val btnShape = NovaShapes.Control
            Box(
                Modifier
                    .size(54.dp)
                    .clip(btnShape)
                    .background(if (selected) Color.Transparent else Nova.CardRaised)
                    .border(1.dp, if (selected) Nova.Black.copy(alpha = 0.85f) else Nova.CardBorder, btnShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    CheckIcon(Nova.Black, Modifier.size(22.dp))
                } else {
                    SignalIcon(pingColor(pingMs), Modifier.size(20.dp))
                }
            }
        }

        if (hasMenu) {
            NovaDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                offset = androidx.compose.ui.unit.DpOffset(16.dp, 0.dp),
            ) {
                onCopy?.let { NovaMenuItem("Copy link") { menuOpen = false; it() } }
                onDelete?.let { NovaMenuItem("Delete", color = Nova.Red) { menuOpen = false; it() } }
            }
        }
    }
}

private fun pingColor(ms: Int?): Color = when {
    ms == null -> Nova.Gray
    ms < 120 -> Nova.Green
    ms < 250 -> Nova.Orange
    else -> Nova.Red
}
