/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

import org.robovm.apple.foundation.NSDate
import org.robovm.apple.foundation.NSDateComponents
import org.robovm.apple.foundation.NSTimeZone

/**
 * Converts the [Instant] to an instance of [NSDate].
 *
 * The conversion is lossy: Darwin uses millisecond precision to represent dates, and [Instant] allows for nanosecond
 * resolution.
 */
public fun Instant.toNSDate(): NSDate {
    val secs = epochSeconds * 1.0 + nanosecondsOfSecond / 1.0e9
    if (secs < NSDate.getDistantPast().timeIntervalSince1970 || secs > NSDate.getDistantFuture().timeIntervalSince1970) {
        throw IllegalArgumentException("Boundaries of NSDate exceeded")
    }
    return NSDate.createWithTimeIntervalSince1970(secs)
}

/**
 * Converts the [NSDate] to the corresponding [Instant].
 *
 * Even though Darwin only uses millisecond precision, it is possible that [date] uses larger resolution, storing
 * microseconds or even nanoseconds. In this case, the sub-millisecond parts of [date] are rounded to the nearest
 * millisecond, given that they are likely to be conversion artifacts.
 */
public fun NSDate.toKotlinInstant(): Instant {
    val secs = timeIntervalSince1970
    val millis = secs * 1000 + if (secs > 0) 0.5 else -0.5
    return Instant.fromEpochMilliseconds(millis.toLong())
}

/**
 * Converts the [TimeZone] to [NSTimeZone].
 *
 * If the time zone is represented as a fixed number of seconds from UTC+0 (for example, if it is the result of a call
 * to [TimeZone.offset]) and the offset is not given in even minutes but also includes seconds, this method throws
 * [DateTimeException] to denote that lossy conversion would happen, as Darwin internally rounds the offsets to the
 * nearest minute.
 */
public fun TimeZone.toNSTimeZone(): NSTimeZone = if (this is FixedOffsetTimeZone) {
    require (offset.totalSeconds % 60 == 0) {
        "NSTimeZone cannot represent fixed-offset time zones with offsets not expressed in whole minutes: $this"
    }
    NSTimeZone.fromGMTSecondsOffset(offset.totalSeconds.toLong())
} else {
    NSTimeZone(id)
}

/**
 * Converts the [NSTimeZone] to the corresponding [TimeZone].
 */
public fun NSTimeZone.toKotlinTimeZone(): TimeZone = TimeZone.of(name)

/**
 * Converts the given [LocalDate] to [NSDateComponents].
 *
 * Of all the fields, only the bare minimum required for uniquely identifying the date are set.
 */
public fun LocalDate.toNSDateComponents(): NSDateComponents {
    val components = NSDateComponents()
    components.year = year.toLong()
    components.month = monthNumber.toLong()
    components.day = dayOfMonth.toLong()
    return components
}

/**
 * Converts the given [LocalDate] to [NSDateComponents].
 *
 * Of all the fields, only the bare minimum required for uniquely identifying the date and time are set.
 */
public fun LocalDateTime.toNSDateComponents(): NSDateComponents {
    val components = date.toNSDateComponents()
    components.hour = hour.toLong()
    components.minute = minute.toLong()
    components.second = second.toLong()
    components.nanosecond = nanosecond.toLong()
    return components
}
