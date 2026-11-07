package com.sniperoftdm.booktracker.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(Dimens.cornerTextField),
    large = RoundedCornerShape(Dimens.cornerCard),
    extraLarge = RoundedCornerShape(Dimens.cornerDialog)
)

object ClayShapes {
    val card = RoundedCornerShape(Dimens.cornerCard)
    val button = RoundedCornerShape(CornerSize(50))
    val chip = RoundedCornerShape(CornerSize(50))
    val textField = RoundedCornerShape(Dimens.cornerTextField)
    val sheet = RoundedCornerShape(
        topStart = Dimens.cornerSheetTop,
        topEnd = Dimens.cornerSheetTop,
        bottomEnd = 0.dp,
        bottomStart = 0.dp
    )
    val dialog = RoundedCornerShape(Dimens.cornerDialog)
    val pill = RoundedCornerShape(CornerSize(50))
    val small = RoundedCornerShape(12.dp)
}
