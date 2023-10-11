/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("InstantJvmKt")

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.Serializable
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Days
import org.joda.time.Months
import org.joda.time.field.FieldUtils
import kotlin.time.*
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.joda.time.Instant as jtInstant

@Serializable(with = InstantIso8601Serializer::class)
public actual class Instant internal constructor(internal val value: jtInstant) : Comparable<Instant> {

    public actual val epochSeconds: Long
        get() = value.millis / 1000
    public actual val nanosecondsOfSecond: Int
        get() = value.chronology.millisOfSecond().get(value.millis) * 1_000_000

    public actual fun toEpochMilliseconds(): Long = try {
        value.millis
    } catch (e: ArithmeticException) {
        if (value.isAfter(org.joda.time.Instant.EPOCH)) Long.MAX_VALUE else Long.MIN_VALUE
    }

    public actual operator fun plus(duration: Duration): Instant = duration.toComponents { seconds, nanoseconds ->
        try {
            Instant(value.plus(seconds * 1_000).plus(nanoseconds.toLong() / 1_000_000))
        } catch (e: java.lang.Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (duration.isPositive()) MAX else MIN
        }
    }

    public actual operator fun minus(duration: Duration): Instant = plus(-duration)

    public actual operator fun minus(other: Instant): Duration =
        (this.epochSeconds - other.epochSeconds).seconds + // won't overflow given the instant bounds
                (this.nanosecondsOfSecond - other.nanosecondsOfSecond).nanoseconds

    public actual override operator fun compareTo(other: Instant): Int = this.value.compareTo(other.value)

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is Instant && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    public actual companion object {
        @Deprecated("Use Clock.System.now() instead", ReplaceWith("Clock.System.now()", "kotlinx.datetime.Clock"), level = DeprecationLevel.ERROR)
        public actual fun now(): Instant =
            Instant(org.joda.time.Instant.now())

        public actual fun fromEpochMilliseconds(epochMilliseconds: Long): Instant =
            Instant(jtInstant.ofEpochMilli(epochMilliseconds))

        public actual fun parse(isoString: String): Instant = Instant(org.joda.time.Instant.parse(fixOffsetRepresentation(isoString)))

        /** A workaround for a quirk of the JDKs older than 11 where the string representations of Instant that have an
         * offset of the form "+XX" are not recognized by [jtOffsetDateTime.parse], while "+XX:XX" work fine. */
        private fun fixOffsetRepresentation(isoString: String): String {
            val time = isoString.indexOf('T', ignoreCase = true)
            if (time == -1) return isoString // the string is malformed
            val offset = isoString.indexOfLast { c -> c == '+' || c == '-' }
            if (offset < time) return isoString // the offset is 'Z' and not +/- something else
            val separator = isoString.indexOf(':', offset) // if there is a ':' in the offset, no changes needed
            return if (separator != -1) isoString else "$isoString:00"
        }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Long): Instant = try {
            Instant(jtInstant.ofEpochSecond(epochSeconds))
        } catch (e: Exception) {
            if (e !is ArithmeticException && e !is DateTimeException) throw e
            if (epochSeconds > 0) MAX else MIN
        }

        public actual fun fromEpochSeconds(epochSeconds: Long, nanosecondAdjustment: Int): Instant =
            fromEpochSeconds(epochSeconds, nanosecondAdjustment.toLong())

        public actual val DISTANT_PAST: Instant = Instant(jtInstant.ofEpochSecond(-3217862419201))
        public actual val DISTANT_FUTURE: Instant = Instant(jtInstant.ofEpochSecond(3093527980800))

        internal actual val MIN: Instant = DISTANT_PAST
        internal actual val MAX: Instant = Instant(DISTANT_FUTURE.value.plus(999L))
    }
}

private fun Instant.atZone(zone: DateTimeZone): org.joda.time.DateTime = try {
    value.toDateTime(zone)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

public actual fun Instant.plus(period: DateTimePeriod, timeZone: TimeZone): Instant {
    try {
        val thisZdt = atZone(timeZone.toJavaZoneId())
        return with(period) {
            thisZdt
                .run { if (totalMonths != 0) plusMonths(totalMonths) else this }
                .run { if (days != 0) plusDays(days) else this }
                .run { if (totalNanoseconds != 0L) plus(totalNanoseconds / 1_000_000) else this }
        }.toInstant().let(::Instant)
    } catch (e: DateTimeException) {
        throw DateTimeArithmeticException(e)
    }
}

@Deprecated("Use the plus overload with an explicit number of units", ReplaceWith("this.plus(1, unit, timeZone)"))
public actual fun Instant.plus(unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(1L, unit, timeZone)

public actual fun Instant.plus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(value.toLong(), unit, timeZone)

public actual fun Instant.minus(value: Int, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    plus(-value.toLong(), unit, timeZone)

public actual fun Instant.plus(value: Long, unit: DateTimeUnit, timeZone: TimeZone): Instant =
    try {
        val thisZdt = atZone(timeZone.toJavaZoneId())
        when (unit) {
            is DateTimeUnit.TimeBased ->
                plus(value, unit).value
            is DateTimeUnit.DayBased ->
                thisZdt.plusDays(FieldUtils.safeMultiply(value, unit.days.toLong()).toInt()).toInstant()
            is DateTimeUnit.MonthBased ->
                thisZdt.plusMonths(FieldUtils.safeMultiply(value, unit.months.toLong()).toInt()).toInstant()
        }.let(::Instant)
    } catch (e: Exception) {
        if (e !is DateTimeException && e !is ArithmeticException) throw e
        throw DateTimeArithmeticException("Instant $this cannot be represented as local date when adding $value $unit to it", e)
    }

public actual fun Instant.plus(value: Long, unit: DateTimeUnit.TimeBased): Instant =
    try {
        multiplyAndDivide(value, unit.nanoseconds, NANOS_PER_ONE.toLong()).let { (d, r) ->
            Instant(this.value.plus(d * 1000).plus(r / 1_000_000))
        }
    } catch (e: Exception) {
        if (e !is DateTimeException && e !is ArithmeticException) throw e
        if (value > 0) Instant.MAX else Instant.MIN
    }

public actual fun Instant.periodUntil(other: Instant, timeZone: TimeZone): DateTimePeriod {
    var thisZdt = this.atZone(timeZone.toJavaZoneId())
    val otherZdt = other.atZone(timeZone.toJavaZoneId())

    val months = Months.monthsBetween(thisZdt, otherZdt).months; thisZdt = thisZdt.plusMonths(months)
    val days = Days.daysBetween(thisZdt, otherZdt).days; thisZdt = thisZdt.plusDays(days)
    val nanoseconds = org.joda.time.Duration(thisZdt, otherZdt).millis * 1_000_000L

    if (months > Int.MAX_VALUE || months < Int.MIN_VALUE) {
        throw DateTimeArithmeticException("The number of months between $this and $other does not fit in an Int")
    }
    return buildDateTimePeriod(months.toInt(), days.toInt(), nanoseconds)
}

public actual fun Instant.until(other: Instant, unit: DateTimeUnit, timeZone: TimeZone): Long = try {
    val thisZdt = this.atZone(timeZone.toJavaZoneId())
    val otherZdt = other.atZone(timeZone.toJavaZoneId())
    when(unit) {
        is DateTimeUnit.TimeBased -> until(other, unit)
        is DateTimeUnit.DayBased -> (Days.daysBetween(thisZdt, otherZdt).days / unit.days).toLong()
        is DateTimeUnit.MonthBased -> (Months.monthsBetween(thisZdt, otherZdt).months / unit.months).toLong()
    }
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
} catch (e: ArithmeticException) {
    if (this.value < other.value) Long.MAX_VALUE else Long.MIN_VALUE
}

internal actual fun Instant.toStringWithOffset(offset: UtcOffset): String =
    DateTime(this.value, offset.zoneOffset).toString()