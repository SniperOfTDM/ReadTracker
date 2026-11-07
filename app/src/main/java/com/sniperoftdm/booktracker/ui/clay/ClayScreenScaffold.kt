package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sniperoftdm.booktracker.ui.theme.Dimens

val LocalClayBottomInset = compositionLocalOf<Dp> { 0.dp }

@Composable
fun ClayScreenScaffold(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null,
    floatingActionButton: @Composable (() -> Unit)? = null,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isTopLevel: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barHeightWithMargin = Dimens.navBarHeight + Dimens.navBarBottomMargin // 68dp + 16dp = 84dp
    val bottomInset = if (isTopLevel) {
        barHeightWithMargin + navBarsBottom
    } else {
        navBarsBottom
    }

    // Scroll padding: bottom inset + 16dp (plus extra room for FAB if present so last item scrolls above FAB)
    val fabClearance = if (floatingActionButton != null) 72.dp else 16.dp
    val contentBottomPadding = bottomInset + fabClearance

    CompositionLocalProvider(
        LocalClayBottomInset provides bottomInset
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("clay_screen_scaffold")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Slot with status-bar insets and 16dp gutter
                if (title != null || navigationIcon != null || actions != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = Dimens.spaceLarge,
                                vertical = Dimens.spaceMedium
                            )
                            .testTag("clay_screen_header"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (navigationIcon != null) {
                                navigationIcon()
                                Spacer(modifier = Modifier.width(Dimens.spaceMedium))
                            }
                            Column(modifier = Modifier.testTag("clay_screen_title")) {
                                if (title != null) {
                                    ProvideTextStyle(value = MaterialTheme.typography.displaySmall) {
                                        title()
                                    }
                                }
                                if (subtitle != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
                                        subtitle()
                                    }
                                }
                            }
                        }

                        if (actions != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSmall),
                                verticalAlignment = Alignment.CenterVertically,
                                content = actions
                            )
                        }
                    }
                }

                // Content Slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content(PaddingValues(bottom = contentBottomPadding))
                }
            }

            // Floating Action Button
            if (floatingActionButton != null) {
                val fabAlignment = if (floatingActionButtonPosition == FabPosition.Center) {
                    Alignment.BottomCenter
                } else {
                    Alignment.BottomEnd
                }
                Box(
                    modifier = Modifier
                        .align(fabAlignment)
                        .padding(
                            end = if (floatingActionButtonPosition == FabPosition.End) Dimens.spaceLarge else 0.dp,
                            start = if (floatingActionButtonPosition == FabPosition.Start) Dimens.spaceLarge else 0.dp,
                            bottom = bottomInset + 16.dp
                        )
                        .testTag("clay_fab_container")
                ) {
                    floatingActionButton()
                }
            }
        }
    }
}
