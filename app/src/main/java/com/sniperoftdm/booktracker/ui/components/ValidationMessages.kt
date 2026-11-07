package com.sniperoftdm.booktracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sniperoftdm.booktracker.R
import com.sniperoftdm.booktracker.domain.ValidationReason

@StringRes
fun ValidationReason.toStringRes(): Int = when (this) {
    ValidationReason.END_PAGE_NOT_GREATER_THAN_PREVIOUS -> R.string.val_end_page_not_greater
    ValidationReason.END_PAGE_EXCEEDS_TOTAL -> R.string.val_end_page_exceeds_total
    ValidationReason.END_PAGE_NOT_LESS_THAN_NEXT -> R.string.val_end_page_not_less_than_next
    ValidationReason.TIMESTAMP_IN_FUTURE -> R.string.val_timestamp_future
    ValidationReason.TITLE_BLANK -> R.string.val_title_blank
    ValidationReason.TOTAL_PAGES_NOT_POSITIVE -> R.string.val_total_pages_not_positive
    ValidationReason.START_PAGE_OUT_OF_RANGE -> R.string.val_start_page_out_of_range
    ValidationReason.TOTAL_PAGES_BELOW_CURRENT -> R.string.val_total_pages_below_current
    ValidationReason.NOTE_TEXT_BLANK -> R.string.val_note_text_blank
    ValidationReason.NOTE_PAGE_OUT_OF_RANGE -> R.string.val_note_page_out_of_range
}

@Composable
fun ValidationReason.toMessage(): String = stringResource(id = toStringRes())
