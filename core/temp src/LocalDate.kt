/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateJvmKt")
package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.LocalDateIso8601Serializer
import kotlinx.serialization.Serializable
import org.joda.time.Days
import org.joda.time.LocalTime
import org.joda.time.Months
import org.joda.time.Years
import org.joda.time.LocalDate as jtLocalDate

@Serializable(with = LocalDateIso8601Serializer::class)
public actual class LocalDate internal constructor(internal val value: jtLocalDate) : Comparable<LocalDate> {
    public actual companion object {
        public actual fun parse(isoString: String): LocalDate = jtLocalDate.parse(isoString).let(::LocalDate)

        internal actual val MIN: LocalDate = LocalDate(-292275055, 1, 1)
        internal actual val MAX: LocalDate = LocalDate(292278994, 12, 31)

        private val epochDate = jtLocalDate(1970, 1, 1)
        private val epochDateTime = epochDate.toDateTime(LocalTime.MIDNIGHT)

        public actual fun fromEpochDays(epochDays: Int): LocalDate =
            LocalDate(epochDate.plusDays(epochDays))
    }

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int) :
            this(jtLocalDate(year, monthNumber, dayOfMonth))

    public actual constructor(year: Int, month: Month, dayOfMonth: Int) : this(year, month.number, dayOfMonth)

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthOfYear
    public actual val month: Month get() = Month.fromMonthOfYear(value.monthOfYear)
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = DayOfWeek.fromDayOfWeek(value.dayOfWeek)
    public actual val dayOfYear: Int get() = value.dayOfYear

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalDate && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalDate): Int = this.value.compareTo(other.value)

    public actual fun toEpochDays(): Int = Days.daysBetween(epochDateTime, value.toDateTime(LocalTime.MIDNIGHT)).days
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit)"))
public actual fun LocalDate.plus(unit: DateTimeUnit.DateBased): LocalDate =
    plus(1L, unit)

public actual fun LocalDate.plus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
    plus(value.toLong(), unit)

public actual fun LocalDate.minus(value: Int, unit: DateTimeUnit.DateBased): LocalDate =
    plus(-value.toLong(), unit)

public actual fun LocalDate.plus(value: Long, unit: DateTimeUnit.DateBased): LocalDate =
    try {
        when (unit) {
            is DateTimeUnit.DayBased -> {
                val addDays: Long = safeMultiply(value, unit.days.toLong())
                ofEpochDayChecked(safeAdd(this.toEpochDays().toLong(), addDays))
            }
            is DateTimeUnit.MonthBased ->
                this.value.plusMonths(safeMultiply(value, unit.months.toLong()).toInt())
        }.let(::LocalDate)
    } catch (e: Exception) {
        if (e !is DateTimeException && e !is ArithmeticException) throw e
        throw DateTimeArithmeticException("The result of adding $value of $unit to $this is out of LocalDate range.", e)
    }

private val minEpochDay = LocalDate.MIN.toEpochDays()
private val maxEpochDay = LocalDate.MAX.toEpochDays()
private fun ofEpochDayChecked(epochDay: Long): jtLocalDate {
    // LocalDate.ofEpochDay doesn't actually check that the argument doesn't overflow year calculation
    if (epochDay !in minEpochDay..maxEpochDay)
        throw DateTimeException("The resulting day $epochDay is out of supported LocalDate range.")
    return jtLocalDate(epochDay).plusDays(epochDay.toInt())
}

public actual operator fun LocalDate.plus(period: DatePeriod): LocalDate = try {
    with(period) {
        return@with value
            .run { if (totalMonths != 0) plusMonths(totalMonths) else this }
            .run { if (days != 0) plusDays(days) else this }

    }.let(::LocalDate)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException("The result of adding $value to $this is out of LocalDate range.")
}


public actual fun LocalDate.periodUntil(other: LocalDate): DatePeriod {
    var startD = this
    val endD = other
    val months = startD.until(endD, DateTimeUnit.MONTH); startD = startD.value.plusMonths(months).toKotlinLocalDate()
    val days = startD.until(endD, DateTimeUnit.DAY)

    if (months > Int.MAX_VALUE || months < Int.MIN_VALUE) {
        throw DateTimeArithmeticException("The number of months between $this and $other does not fit in an Int")
    }
    return DatePeriod(0, months.toInt(), days.toInt())
}

public actual fun LocalDate.until(other: LocalDate, unit: DateTimeUnit.DateBased): Int = when(unit) {
    is DateTimeUnit.MonthBased -> (Months.monthsBetween(this.value.toDateTime(LocalTime.MIDNIGHT), other.value.toDateTime(LocalTime.MIDNIGHT)).months / unit.months)
    is DateTimeUnit.DayBased -> (Days.daysBetween(this.value.toDateTime(LocalTime.MIDNIGHT), other.value.toDateTime(LocalTime.MIDNIGHT)).days / unit.days)
}

public actual fun LocalDate.daysUntil(other: LocalDate): Int =
    Days.daysBetween(this.value.toDateTime(LocalTime.MIDNIGHT), other.value.toDateTime(LocalTime.MIDNIGHT)).days

public actual fun LocalDate.monthsUntil(other: LocalDate): Int =
    Months.monthsBetween(this.value.toDateTime(LocalTime.MIDNIGHT), other.value.toDateTime(LocalTime.MIDNIGHT)).months

public actual fun LocalDate.yearsUntil(other: LocalDate): Int =
    Years.yearsBetween(this.value.toDateTime(LocalTime.MIDNIGHT), other.value.toDateTime(LocalTime.MIDNIGHT)).years