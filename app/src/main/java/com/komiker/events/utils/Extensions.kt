package com.komiker.events.utils

import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.komiker.events.R

fun ChipGroup.addChip(
    text: String,
    onCloseClick: (Chip) -> Unit
) {
    val chip = Chip(context, null, com.google.android.material.R.style.Widget_Material3_Chip_Filter).apply {
        this.text = text
        isCloseIconVisible = true
        chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.neutral_98)
        setTextColor(ContextCompat.getColorStateList(context, R.color.neutral_0))
        setCloseIconResource(R.drawable.ic_close)
        closeIconTint = ContextCompat.getColorStateList(context, R.color.neutral_0)
        chipStrokeColor = ContextCompat.getColorStateList(context, R.color.neutral_95)
        chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
        typeface = ResourcesCompat.getFont(context, R.font.fixel_semibold)
        val contentPadding = resources.getDimension(R.dimen.chip_content_padding)
        chipStartPadding = contentPadding
        chipEndPadding = contentPadding
        textStartPadding = contentPadding
        textEndPadding = contentPadding
        iconStartPadding = contentPadding
        setOnCloseIconClickListener {
            onCloseClick(this)
        }
    }
    addView(chip)
}