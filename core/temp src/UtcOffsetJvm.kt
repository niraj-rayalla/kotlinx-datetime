/*
 * Copyright 2019-2021 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import kotlinx.datetime.serializers.UtcOffsetSerializer
import kotlinx.serialization.Serializable
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit

@Serializable(with = UtcOffsetSerializer::class)
public actual class UtcOffset(internal val zoneOffset: DateTimeZone) {
    public actual val totalSeconds: Int get() = TimeUnit.MILLISECONDS.toHours(zoneOffset.getOffset(org.joda.time.Instant.now()).toLong()).toInt()

    override fun hashCode(): Int = zoneOffset.hashCode()
    override fun equals(other: Any?): Boolean = other is UtcOffset && this.zoneOffset == other.zoneOffset
    override fun toString(): String = zoneOffset.toString()

    public actual companion object {

        public actual val ZERO: UtcOffset = UtcOffset(DateTimeZone.UTC)

        public actual fun parse(offsetString: String): UtcOffset = DateTimeZone.forID(offsetString).let(::UtcOffset)
    }
}

@Suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
public actual fun UtcOffset(hours: Int? = null, minutes: Int? = null, seconds: Int? = null): UtcOffset =
    try {
        when {
            hours != null ->
                UtcOffset(DateTimeZone.forOffsetMillis(hours*3_600*1_000 + (minutes ?: 0)*60*1_000 + (seconds ?: 0)*1_000))
            minutes != null ->
                UtcOffset(DateTimeZone.forOffsetMillis(minutes*60*1_000 + (seconds ?: 0)*1_000))
            else -> {
                UtcOffset(DateTimeZone.forOffsetMillis((seconds ?: 0)*1_000))
            }
        }
    } catch (e: DateTimeException) {
        throw IllegalArgumentException(e)
    }