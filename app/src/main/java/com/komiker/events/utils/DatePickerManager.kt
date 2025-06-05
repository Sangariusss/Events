package com.komiker.events.utils

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.komiker.events.R
import com.komiker.events.viewmodels.CreateEventViewModel
import com.shawnlin.numberpicker.NumberPicker
import java.util.Calendar

class DatePickerManager(
    private val monthPicker: NumberPicker,
    private val dayPicker: NumberPicker,
    private val yearPicker: NumberPicker,
    private val viewModel: CreateEventViewModel,
    private val context: Context,
    private val onDateChanged: () -> Unit
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

    fun restoreState(savedInstanceState: android.os.Bundle?) {
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
        } else {
            val vmMonth = viewModel.selectedMonth
            val vmDay = viewModel.selectedDay
            val vmYear = viewModel.selectedYear
            if (vmMonth != null && vmDay != null && vmYear != null) {
                monthPicker.value = vmMonth.coerceIn(0, 11)
                dayPicker.value = vmDay.coerceIn(1, 31)
                yearPicker.value = vmYear.coerceIn(currentYear, currentYear + 10)
                updateDayPickerMaxValue(vmMonth, vmYear)
            } else {
                setCurrentDate()
            }
        }
    }

    fun saveFilters() {
        val month = months[monthPicker.value]
        val day = dayPicker.value
        val year = yearPicker.value
        viewModel.startDate = "$month $day, $year"
        viewModel.selectedMonth = monthPicker.value
        viewModel.selectedDay = dayPicker.value
        viewModel.selectedYear = yearPicker.value
    }

    private fun setCurrentDate() {
        yearPicker.value = calendar.get(Calendar.YEAR)
        monthPicker.value = calendar.get(Calendar.MONTH)
        updateDayPickerMaxValue(monthPicker.value, yearPicker.value)
        dayPicker.value = calendar.get(Calendar.DAY_OF_MONTH)
        saveFilters()
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
            3, 5, 8, 10 -> 30
            else -> if (isLeapYear(year)) 29 else 28
        }
        dayPicker.maxValue = maxDay
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}