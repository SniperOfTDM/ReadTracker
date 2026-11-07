package com.sniperoftdm.booktracker.ui.clay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sniperoftdm.booktracker.ui.theme.ClayShapes
import com.sniperoftdm.booktracker.ui.theme.Dimens
import com.sniperoftdm.booktracker.ui.theme.LocalClayColors

@Composable
fun ClayBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val clayColors = LocalClayColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismissRequest
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = Dimens.contentMaxWidth)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .clay(
                    shape = ClayShapes.sheet,
                    color = clayColors.face,
                    elevation = ClayElevation.Floating
                )
                .navigationBarsPadding()
                .imePadding()
                .padding(Dimens.spaceLarge)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Grab handle
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 6.dp)
                        .clay(
                            shape = CircleShape,
                            color = clayColors.shade.copy(alpha = 0.25f),
                            elevation = ClayElevation.Inset
                        )
                )
                Spacer(modifier = Modifier.height(Dimens.spaceMedium))
                content()
            }
        }
    }
}

@Composable
fun ClayDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit
) {
    val clayColors = LocalClayColors.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.spaceLarge)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .clay(
                        shape = ClayShapes.dialog,
                        color = clayColors.face,
                        elevation = ClayElevation.Floating
                    )
                    .padding(Dimens.spaceExtraLarge)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ClaySnackbar(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val clayColors = LocalClayColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.spaceLarge, vertical = Dimens.spaceMedium)
            .clay(
                shape = ClayShapes.card,
                color = if (clayColors.isDark) Color(0xFF38302A) else Color(0xFF3A2F2A),
                elevation = ClayElevation.Floating
            )
            .padding(horizontal = Dimens.spaceLarge, vertical = Dimens.spaceMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.width(Dimens.spaceSmall))
                Box(
                    modifier = Modifier
                        .clayClickable(
                            shape = ClayShapes.button,
                            color = clayColors.terracotta,
                            elevation = ClayElevation.Raised,
                            onClick = onActionClick
                        )
                        .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceSmall)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = clayColors.textOnAccent(clayColors.terracotta)
                    )
                }
            }
        }
    }
}

@Composable
fun ClayDivider(
    modifier: Modifier = Modifier
) {
    val clayColors = LocalClayColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .clay(
                shape = CircleShape,
                color = clayColors.shade.copy(alpha = 0.12f),
                elevation = ClayElevation.Inset
            )
    )
}
