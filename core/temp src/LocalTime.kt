/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
@file:JvmName("LocalTimeJvmKt")

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.LocalTimeIso8601Serializer
import kotlinx.serialization.Serializable
import org.joda.time.LocalTime as jtLocalTime

@Serializable(with = LocalTimeIso8601Serializer::class)
public actual class LocalTime internal constructor(internal val value: jtLocalTime) :
    Comparable<LocalTime> {

    public actual constructor(hour: Int, minute: Int, second: Int, nanosecond: Int) :
            this(
                jtLocalTime(hour, minute, second, nanosecond/1_000_000)
            )

    public actual val hour: Int get() = value.hourOfDay
    public actual val minute: Int get() = value.minuteOfHour
    public actual val second: Int get() = value.secondOfMinute
    public actual val nanosecond: Int get() = value.millisOfSecond * 1_000_000
    public actual fun toSecondOfDay(): Int = value.millisOfDay / 1_000
    public actual fun toMillisecondOfDay(): Int = value.millisOfDay
    public actual fun toNanosecondOfDay(): Long = toMillisecondOfDay() * 1_000L

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is LocalTime && this.value == other.value)

    override fun hashCode(): Int = value.hashCode()

    actual override fun toString(): String = value.toString()

    actual override fun compareTo(other: LocalTime): Int = this.value.compareTo(other.value)

    public actual companion object {
        public actual fun parse(isoString: String): LocalTime = jtLocalTime.parse(isoString).let(::LocalTime)

        public actual fun fromSecondOfDay(secondOfDay: Int): LocalTime = jtLocalTime.fromMillisOfDay(secondOfDay.toLong() * 1_000L).let(::LocalTime)

        public actual fun fromMillisecondOfDay(millisecondOfDay: Int): LocalTime = jtLocalTime.fromMillisOfDay(millisecondOfDay.toLong()).let(::LocalTime)

        public actual fun fromNanosecondOfDay(nanosecondOfDay: Long): LocalTime = jtLocalTime.fromMillisOfDay(nanosecondOfDay / 1_000_000L).let(::LocalTime)

        internal actual val MIN: LocalTime = LocalTime(0, 0, 0, 0)
        internal actual val MAX: LocalTime = LocalTime(23, 59, 59, 999_999_999)
    }
}