package com.novavpn.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.novavpn.app.ui.Motion
import com.novavpn.app.ui.Press
import com.novavpn.app.ui.menuPageTransition
import com.novavpn.app.ui.pressable
import com.novavpn.app.ui.theme.Nova
import com.novavpn.app.ui.theme.NovaShapes
import com.novavpn.app.ui.theme.NovaSpace
import com.novavpn.app.ui.theme.NovaType

// Shared building blocks. New screens should use these instead of rebuilding them, so every screen
// looks and behaves the same. Colours come from Nova, text from NovaType, corners from NovaShapes.

/** Full-width lime button. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressable(NovaShapes.Pill, Press.Pill, onClick = onClick)
            .background(Nova.Lime),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Nova.Black, style = NovaType.Button) }
}

/** Card with a title, a short explanation and one action. [overlay] makes it slightly see-through (over the map). */
@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    overlay: Boolean = false,
) {
    Column(
        modifier
            .fillMaxWidth()
            .chipSurface(NovaShapes.Card, fillAlpha = if (overlay) PILL_OVERLAY_ALPHA else 1f)
            .padding(NovaSpace.Xl),
    ) {
        Text(title, color = Nova.White, style = NovaType.Title)
        Text(
            message,
            color = Nova.TextMuted,
            style = NovaType.Secondary,
            modifier = Modifier.padding(top = 6.dp, bottom = NovaSpace.L),
        )
        PrimaryButton(actionLabel, onAction)
    }
}

/** Rounded pill with a single icon (search, add, more, back). */
@Composable
fun NovaIconPill(onClick: () -> Unit, label: String, width: Dp = 52.dp, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .size(width = width, height = 48.dp)
            .pressable(NovaShapes.Pill, Press.Pill, label = label, onClick = onClick)
            .chipSurface(NovaShapes.Pill),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
    }
}

/** Heading for a group of rows, with an optional item count. */
@Composable
fun SectionHeader(title: String, count: Int? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp, bottom = NovaSpace.Xs, start = NovaSpace.Xs, end = NovaSpace.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            color = Nova.White,
            style = NovaType.Heading,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (count != null) {
            Text("$count", color = Nova.TextMuted, style = NovaType.Secondary, modifier = Modifier.padding(start = NovaSpace.S))
        }
    }
}

/** Small muted label above a block of rows (Settings: "Connection", "About"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Nova.TextMuted,
        style = NovaType.Caption,
        modifier = modifier.padding(start = NovaSpace.Xs, top = 14.dp),
    )
}

// --- dropdown menus -------------------------------------------------------------------------

/**
 * The app's popup menu (three-dot, + and per-server menus). Same card surface as the rest of the app:
 * Nova.CardRaised fill, 1dp Nova.CardBorder outline, Card-style rounded corners, no tonal tint.
 */
@Composable
fun NovaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    content: @Composable () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.defaultMinSize(minWidth = 220.dp),
        offset = offset,
        shape = NovaShapes.Menu,
        containerColor = Nova.CardRaised,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Nova.CardBorder),
    ) {
        // Inner padding so the rounded corners never clip the first / last row.
        Column(Modifier.padding(vertical = NovaSpace.S)) { content() }
    }
}

/**
 * Pages of one popup menu. Switching [page] slides/fades the content and animates the menu's size
 * instead of jumping. [order] decides the direction: moving to a later page = forward.
 */
@Composable
fun <T> NovaMenuPages(page: T, order: (T) -> Int, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = page,
        transitionSpec = { menuPageTransition(forward = order(targetState) > order(initialState)) },
        label = "menuPage",
    ) { p -> Column { content(p) } }
}

private val MenuItemPadding = PaddingValues(horizontal = NovaSpace.Xl, vertical = NovaSpace.M)

@Composable
private fun menuItemColors(color: Color) = MenuDefaults.itemColors(
    textColor = color,
    leadingIconColor = Nova.TextMuted,
    trailingIconColor = Nova.TextMuted,
    disabledTextColor = Nova.Gray,
)

/** Thin divider between groups of menu rows. */
@Composable
fun NovaMenuDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = NovaSpace.Xs)
            .height(1.dp)
            .background(Nova.CardBorder),
    )
}

/** Menu row; shows a lime check mark when [checked]. */
@Composable
fun NovaMenuItem(text: String, checked: Boolean = false, color: Color = Nova.White, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text,
                color = if (checked) Nova.Lime else color,
                style = if (checked) NovaType.BodyStrong else NovaType.Body,
            )
        },
        trailingIcon = if (checked) {
            { CheckIcon(Nova.Lime, Modifier.size(18.dp)) }
        } else null,
        colors = menuItemColors(color),
        contentPadding = MenuItemPadding,
        onClick = onClick,
    )
}

/** Menu row that opens a sub page; shows a chevron on the right. */
@Composable
fun NovaSubMenuItem(text: String, color: Color = Nova.White, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, color = color, style = NovaType.Body) },
        trailingIcon = { ChevronIcon(Nova.TextMuted, Modifier.size(18.dp)) },
        colors = menuItemColors(color),
        contentPadding = MenuItemPadding,
        onClick = onClick,
    )
}

/** First row of a sub page: tap to go back to the main menu. */
@Composable
fun NovaMenuBack(title: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(title, color = Nova.TextMuted, style = NovaType.SecondaryStrong) },
        leadingIcon = { ChevronIcon(Nova.TextMuted, Modifier.size(18.dp), left = true) },
        colors = menuItemColors(Nova.TextMuted),
        contentPadding = MenuItemPadding,
        onClick = onClick,
    )
    NovaMenuDivider()
}

/** Small upper-case label between groups of menu rows. */
@Composable
fun NovaMenuLabel(text: String) {
    Text(
        text.uppercase(),
        color = Nova.TextMuted,
        style = NovaType.Label,
        modifier = Modifier.padding(start = NovaSpace.Xl, end = NovaSpace.Xl, top = 10.dp, bottom = 2.dp),
    )
}

// --- forms and dialogs ----------------------------------------------------------------------

/** Row of selectable chips (single choice). */
@Composable
fun NovaChipRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { o ->
            val sel = o == selected
            Box(
                Modifier
                    .height(36.dp)
                    .pressable(NovaShapes.Control, Press.Small, onClick = { onSelect(o) })
                    .chipSurface(NovaShapes.Control, sel)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                val textColor by animateColorAsState(if (sel) Nova.Lime else Nova.White, tween(Motion.Short), label = "chipText")
                Text(o, color = textColor, style = NovaType.Caption)
            }
        }
    }
}

/** Small muted caption above a form control. */
@Composable
fun NovaFieldLabel(text: String) {
    Text(text, color = Nova.TextMuted, style = NovaType.Small, modifier = Modifier.padding(top = 2.dp))
}

/** Text field with a placeholder, in the app's card style. */
@Composable
fun NovaTextField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    keyboardType: KeyboardType,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(NovaShapes.Field)
            .background(Nova.Card)
            .border(1.dp, Nova.CardBorder, NovaShapes.Field)
            .padding(horizontal = 14.dp, vertical = NovaSpace.M),
    ) {
        if (value.isEmpty()) Text(placeholder, color = Nova.TextMuted, style = NovaType.Secondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else 4,
            textStyle = NovaType.Secondary.copy(color = Nova.White),
            cursorBrush = SolidColor(Nova.Lime),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Dialog in the app's colours. Buttons are usually TextButtons. */
@Composable
fun NovaDialog(
    title: String,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.border(1.dp, Nova.CardBorder, NovaShapes.Card),
        shape = NovaShapes.Card,
        containerColor = Nova.CardRaised,
        tonalElevation = 0.dp,
        titleContentColor = Nova.White,
        textContentColor = Nova.TextMuted,
        title = { Text(title, color = Nova.White, style = NovaType.Title) },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}
