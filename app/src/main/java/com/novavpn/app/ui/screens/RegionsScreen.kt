package com.novavpn.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.novavpn.app.SortMode
import com.novavpn.app.data.Region
import com.novavpn.app.ui.components.CheckIcon
import com.novavpn.app.ui.components.ChevronIcon
import com.novavpn.app.ui.components.CloseIcon
import com.novavpn.app.ui.components.MoreIcon
import com.novavpn.app.ui.components.OrbView
import com.novavpn.app.ui.components.PlusIcon
import com.novavpn.app.ui.components.SearchIcon
import com.novavpn.app.ui.components.SignalIcon
import com.novavpn.app.ui.components.chipSurface
import com.novavpn.app.ui.theme.Nova

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RegionsScreen(
    regions: List<Region>,
    selectedId: String?,
    pings: Map<String, Int?>,
    sort: SortMode,
    onSelect: (Region) -> Unit,
    startWithAdd: Boolean = false,
    onAddServer: (link: String, name: String) -> String?,
    onDeleteServer: (String) -> Unit,
    onDeleteAllCustom: () -> Unit,
    onTestAll: () -> Unit,
    onSortChange: (SortMode) -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var showAdd by rememberSaveable { mutableStateOf(startWithAdd) }
    var confirmDeleteAll by rememberSaveable { mutableStateOf(false) }

    // Closes the search field first; a second Back closes the screen (handled in NovaApp).
    BackHandler(enabled = searching) { searching = false; query = "" }

    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) regions else regions.filter {
        it.label.lowercase().contains(q) || it.code.lowercase() == q || it.id.lowercase().contains(q)
    }

    fun sorted(list: List<Region>): List<Region> = when (sort) {
        SortMode.Default -> list
        SortMode.Name -> list.sortedBy { it.label.lowercase() }
        // Unreachable / not-yet-tested servers go last.
        SortMode.Ping -> list.sortedBy { pings[it.id] ?: Int.MAX_VALUE }
    }

    val shown = sorted(filtered)
    val customCount = regions.count { it.custom }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

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
            sort = sort,
            hasCustom = customCount > 0,
            onQueryChange = { query = it },
            onOpenSearch = { searching = true },
            onCloseSearch = { searching = false; query = "" },
            onAdd = { showAdd = true },
            onTestAll = { onTestAll(); toast("Testing all servers…") },
            onSortChange = onSortChange,
            onImportClipboard = {
                val text = clipboard.getText()?.text.orEmpty()
                if (text.isBlank()) {
                    toast("Clipboard is empty")
                } else {
                    // A clipboard can hold several links, one per line.
                    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    var added = 0
                    var lastError: String? = null
                    lines.forEach { line -> onAddServer(line, "")?.let { lastError = it } ?: run { added++ } }
                    toast(if (added > 0) "Added $added server${if (added > 1) "s" else ""}" else lastError ?: "Nothing to import")
                }
            },
            onDeleteAll = { confirmDeleteAll = true },
        )

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (regions.isEmpty()) {
                item { EmptyState(onAdd = { showAdd = true }) }
            } else if (filtered.isEmpty()) {
                item {
                    Text(
                        "No servers match \"${query.trim()}\"",
                        color = Nova.TextMuted,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    )
                }
            }

            items(shown, key = { it.id }) { r ->
                RegionRow(
                    r, r.id == selectedId, pings[r.id],
                    onClick = { onSelect(r) },
                    onCopy = { clipboard.setText(AnnotatedString(r.link)); toast("Link copied") },
                    onDelete = if (r.custom) ({ onDeleteServer(r.id); toast("Server removed") }) else null,
                )
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onPaste = { clipboard.getText()?.text.orEmpty() },
            onAdd = { link, name ->
                val err = onAddServer(link, name)
                if (err == null) { showAdd = false; toast("Server added") }
                err
            },
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = Nova.CardRaised,
            title = { Text("Delete my servers?", color = Nova.White) },
            text = {
                Text(
                    "This removes the $customCount server${if (customCount > 1) "s" else ""} you added. Built-in regions are not affected.",
                    color = Nova.TextMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteAllCustom(); confirmDeleteAll = false }) {
                    Text("Delete", color = Nova.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel", color = Nova.White) }
            },
        )
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp).chipSurface(shape).padding(20.dp),
    ) {
        Text("No servers yet", color = Nova.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Add a vless, vmess, trojan, ss, socks or wireguard link. The flag and location are taken from the proxy name, e.g. Germany.",
            color = Nova.TextMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Nova.Lime)
                .clickable(role = Role.Button, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) { Text("Add server", color = Nova.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/** Title + search / add / overflow pills. The search pill expands into a text field. */
@Composable
private fun RegionsHeader(
    searching: Boolean,
    query: String,
    sort: SortMode,
    hasCustom: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onAdd: () -> Unit,
    onTestAll: () -> Unit,
    onSortChange: (SortMode) -> Unit,
    onImportClipboard: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    val pill = RoundedCornerShape(24.dp)
    var menuOpen by remember { mutableStateOf(false) }

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
                    if (query.isEmpty()) Text("Search regions", color = Nova.TextMuted, fontSize = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(color = Nova.White, fontSize = 15.sp),
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
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            HeaderPill(onClick = onOpenSearch, label = "Search") { SearchIcon(Nova.White) }
            HeaderPill(onClick = onAdd, label = "Add server") { PlusIcon(Nova.White) }
        }

        Box {
            HeaderPill(onClick = { menuOpen = true }, label = "More options") { MoreIcon(Nova.White) }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Nova.CardRaised,
            ) {
                MenuItem("Test all servers") { menuOpen = false; onTestAll() }
                MenuItem("Sort: Default", checked = sort == SortMode.Default) { menuOpen = false; onSortChange(SortMode.Default) }
                MenuItem("Sort: Fastest first", checked = sort == SortMode.Ping) { menuOpen = false; onSortChange(SortMode.Ping) }
                MenuItem("Sort: Name A–Z", checked = sort == SortMode.Name) { menuOpen = false; onSortChange(SortMode.Name) }
                MenuItem("Import from clipboard") { menuOpen = false; onImportClipboard() }
                if (hasCustom) MenuItem("Delete my servers", color = Nova.Red) { menuOpen = false; onDeleteAll() }
            }
        }
    }
}

@Composable
private fun HeaderPill(onClick: () -> Unit, label: String, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .size(width = 52.dp, height = 48.dp)
            .chipSurface(RoundedCornerShape(24.dp))
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

@Composable
private fun MenuItem(text: String, checked: Boolean = false, color: Color = Nova.White, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = color, fontSize = 15.sp) },
        trailingIcon = if (checked) {
            { CheckIcon(Nova.Lime, Modifier.size(18.dp)) }
        } else null,
        onClick = onClick,
    )
}

@Composable
private fun AddServerDialog(
    onDismiss: () -> Unit,
    onPaste: () -> String,
    onAdd: (link: String, name: String) -> String?,
) {
    var link by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nova.CardRaised,
        title = { Text("Add server", color = Nova.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paste a share link: vless, vmess, trojan, ss, socks or wireguard.", color = Nova.TextMuted, fontSize = 14.sp)
                DialogField(
                    value = link,
                    onChange = { link = it; error = null },
                    placeholder = "vless://, vmess://, trojan://, ss://…",
                    singleLine = false,
                    keyboardType = KeyboardType.Uri,
                )
                DialogField(
                    value = name,
                    onChange = { name = it },
                    placeholder = "Name (optional)",
                    singleLine = true,
                    keyboardType = KeyboardType.Text,
                )
                TextButton(onClick = { link = onPaste().trim(); error = null }) {
                    Text("Paste from clipboard", color = Nova.Lime)
                }
                error?.let { Text(it, color = Nova.Red, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = { error = onAdd(link, name) }, enabled = link.isNotBlank()) {
                Text("Add", color = if (link.isNotBlank()) Nova.Lime else Nova.Gray)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Nova.White) } },
    )
}

@Composable
private fun DialogField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    keyboardType: KeyboardType,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = Nova.TextMuted, fontSize = 14.sp)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            textStyle = TextStyle(color = Nova.White, fontSize = 14.sp),
            cursorBrush = SolidColor(Nova.Lime),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
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
    val shape = RoundedCornerShape(24.dp)
    val bg = if (selected) Nova.Lime else Nova.Card
    var menuOpen by remember { mutableStateOf(false) }
    val hasMenu = onCopy != null || onDelete != null

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bg)
                .then(if (selected) Modifier else Modifier.border(1.dp, Nova.CardBorder, shape))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (hasMenu) ({ menuOpen = true }) else null,
                    onLongClickLabel = if (hasMenu) "Server options" else null,
                )
                .padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FlagBadge(region.code, 30, Modifier.padding(end = 12.dp))
            Text(
                region.label,
                color = if (selected) Nova.Black else Nova.White.copy(alpha = 0.78f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            if (pingMs != null) {
                Text(
                    "$pingMs ms",
                    color = if (selected) Nova.Black.copy(alpha = 0.7f) else Nova.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 10.dp),
                )
            }
            val btnShape = RoundedCornerShape(18.dp)
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
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Nova.CardRaised,
                offset = androidx.compose.ui.unit.DpOffset(16.dp, 0.dp),
            ) {
                onCopy?.let { MenuItem("Copy link") { menuOpen = false; it() } }
                onDelete?.let { MenuItem("Delete", color = Nova.Red) { menuOpen = false; it() } }
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
