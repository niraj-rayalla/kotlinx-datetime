/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalDateTimeJvmKt")
package kotlinx.datetime

import kotlinx.datetime.serializers.LocalDateTimeIso8601Serializer
import kotlinx.serialization.Serializable
import org.joda.time.LocalDateTime as jtLocalDateTime

@Serializable(with = LocalDateTimeIso8601Serializer::class)
public actual class LocalDateTime internal constructor(internal val value: jtLocalDateTime) : Comparable<LocalDateTime> {

    public actual constructor(year: Int, monthNumber: Int, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(jtLocalDateTime(year, monthNumber, dayOfMonth, hour, minute, second, nanosecond/1_000_000))

    public actual constructor(year: Int, month: Month, dayOfMonth: Int, hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(year, month.number, dayOfMonth, hour, minute, second, nanosecond)

    public actual constructor(date: LocalDate, time: LocalTime) :
            this(date.value.toLocalDateTime(time.value))

    public actual val year: Int get() = value.year
    public actual val monthNumber: Int get() = value.monthOfYear
    public actual val month: Month get() = Month.fromMonthOfYear(value.monthOfYear)
    public actual val dayOfMonth: Int get() = value.dayOfMonth
    public actual val dayOfWeek: DayOfWeek get() = DayOfWeek.fromDayOfWeek(value.dayOfWeek)
    public actual val dayOfYear: Int get() = value.dayOfYear

    public actual val hour: Int get() = value.hourOfDay
    public actual val minute: Int get() = value.minuteOfHour
    public actual val second: Int get() = value.secondOfMinute
    public actual val nanosecond: Int get() = value.millisOfSecond * 1_000_000

    public actual val date: LocalDate get() = LocalDate(value.toLocalDate()) // cache?

    public actual val time: LocalTime get() = LocalTime(value.toLocalTime())

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalDateTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override actual fun toString(): String = value.toString()

    override actual fun compareTo(other: LocalDateTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(isoString: String): LocalDateTime = jtLocalDateTime.parse(isoString).let(::LocalDateTime)

        internal actual val MIN: LocalDateTime = LocalDateTime(LocalDate.MIN, LocalTime.MIN)
        internal actual val MAX: LocalDateTime = LocalDateTime(LocalDate.MAX, LocalTime.MAX)
    }

}