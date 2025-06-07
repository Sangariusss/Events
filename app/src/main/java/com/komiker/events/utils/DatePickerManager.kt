package com.komiker.events.utils

import android.content.Context
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import com.komiker.events.R
import com.shawnlin.numberpicker.NumberPicker
import java.util.Calendar

class DatePickerManager(
    private val monthPicker: NumberPicker,
    private val dayPicker: NumberPicker,
    private val yearPicker: NumberPicker,
    private val context: Context,
    private val initialMonth: Int?,
    private val initialDay: Int?,
    private val initialYear: Int?,
    private val onDateChanged: () -> Unit,
    private val onDateSaved: (month: Int, day: Int, year: Int) -> Unit
) {
    private val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val calendar = Calendar.getInstance()
    private val currentYear = calendar.get(Calendar.YEAR)

    init {
        setupPickers()
        setListeners()
    }

    private fun setupPickers() {
        val customFont = ResourcesCompat.getFont(context, R.font.fixel_bold)
        listOf(monthPicker, dayPicker, yearPicker).forEach {
            it.typeface = customFont
            it.setSelectedTypeface(customFont)
        }

        monthPicker.apply {
            minValue = 0
            maxValue = months.size - 1
            displayedValues = months
        }
        dayPicker.apply {
            minValue = 1
            maxValue = 31
        }
        yearPicker.apply {
            minValue = currentYear
            maxValue = currentYear + 10
            formatter = NumberPicker.Formatter { value -> value.toString() }
        }
    }

    fun restoreState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val savedMonth = savedInstanceState.getInt("savedMonth", -1)
            val savedDay = savedInstanceState.getInt("savedDay", -1)
            val savedYear = savedInstanceState.getInt("savedYear", -1)
            if (savedMonth != -1 && savedDay != -1 && savedYear != -1) {
                monthPicker.value = savedMonth.coerceIn(0, 11)
                dayPicker.value = savedDay.coerceIn(1, 31)
                yearPicker.value = savedYear.coerceIn(currentYear, currentYear + 10)
                updateDayPickerMaxValue(savedMonth, savedYear)
            }
        } else if (initialMonth != null && initialDay != null && initialYear != null) {
            monthPicker.value = initialMonth.coerceIn(0, 11)
            dayPicker.value = initialDay.coerceIn(1, 31)
            yearPicker.value = initialYear.coerceIn(currentYear, currentYear + 10)
            updateDayPickerMaxValue(initialMonth, initialYear)
        } else {
            setCurrentDate()
        }
    }

    fun saveDate() {
        onDateSaved(monthPicker.value, dayPicker.value, yearPicker.value)
    }

    private fun setCurrentDate() {
        yearPicker.value = calendar.get(Calendar.YEAR)
        monthPicker.value = calendar.get(Calendar.MONTH)
        updateDayPickerMaxValue(monthPicker.value, yearPicker.value)
        dayPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
        saveDate()
    }

    private fun setListeners() {
        monthPicker.setOnValueChangedListener { _, _, newVal ->
            updateDayPickerMaxValue(newVal, yearPicker.value)
            onDateChanged()
        }
        dayPicker.setOnValueChangedListener { _, _, _ ->
            onDateChanged()
        }
        yearPicker.setOnValueChangedListener { _, _, newVal ->
            updateDayPickerMaxValue(monthPicker.value, newVal)
            onDateChanged()
        }
    }

    private fun updateDayPickerMaxValue(month: Int, year: Int) {
        val maxDay = when (month) {
            0, 2, 4, 6, 7, 9, 11 -> 31
            else -> if (month == 1 && isLeapYear(year)) 29 else if (month == 1) 28 else 30
        }
        dayPicker.maxValue = maxDay
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}