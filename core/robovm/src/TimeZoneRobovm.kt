/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
/* Based on the ThreeTenBp project.
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 */
@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.internal.*
import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import org.robovm.apple.foundation.NSCalendar
import org.robovm.apple.foundation.NSCalendarIdentifier
import org.robovm.apple.foundation.NSCalendarUnit
import org.robovm.apple.foundation.NSDate
import org.robovm.apple.foundation.NSTimeZone
import java.lang.ArithmeticException
import java.lang.RuntimeException

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor() {

    public actual companion object {

        public actual fun currentSystemDefault(): TimeZone =
            // TODO: probably check if currentSystemDefault name is parseable as FixedOffsetTimeZone?
            RegionTimeZone.currentSystemDefault()

        public actual val UTC: FixedOffsetTimeZone = UtcOffset.ZERO.asTimeZone()

        // org.threeten.bp.ZoneId#of(java.lang.String)
        public actual fun of(zoneId: String): TimeZone {
            // TODO: normalize aliases?
            if (zoneId == "Z") {
                return UTC
            }
            if (zoneId.length == 1) {
                throw IllegalTimeZoneException("Invalid zone ID: $zoneId")
            }
            try {
                if (zoneId.startsWith("+") || zoneId.startsWith("-")) {
                    return UtcOffset.parse(zoneId).asTimeZone()
                }
                if (zoneId == "UTC" || zoneId == "GMT" || zoneId == "UT") {
                    return FixedOffsetTimeZone(UtcOffset.ZERO, zoneId)
                }
                if (zoneId.startsWith("UTC+") || zoneId.startsWith("GMT+") ||
                    zoneId.startsWith("UTC-") || zoneId.startsWith("GMT-")
                ) {
                    val prefix = zoneId.take(3)
                    val offset = UtcOffset.parse(zoneId.substring(3))
                    return when (offset.totalSeconds) {
                        0 -> FixedOffsetTimeZone(offset, prefix)
                        else -> FixedOffsetTimeZone(offset, "$prefix$offset")
                    }
                }
                if (zoneId.startsWith("UT+") || zoneId.startsWith("UT-")) {
                    val offset = UtcOffset.parse(zoneId.substring(2))
                    return when (offset.totalSeconds) {
                        0 -> FixedOffsetTimeZone(offset, "UT")
                        else -> FixedOffsetTimeZone(offset, "UT$offset")
                    }
                }
            } catch (e: DateTimeFormatException) {
                throw IllegalTimeZoneException(e)
            }
            return RegionTimeZone.of(zoneId)
        }

        public actual val availableZoneIds: Set<String>
            get() = RegionTimeZone.availableZoneIds
    }

    public actual open val id: String
        get() = error("Should be overridden")

    public actual fun Instant.toLocalDateTime(): LocalDateTime = instantToLocalDateTime(this)
    public actual fun LocalDateTime.toInstant(): Instant = localDateTimeToInstant(this)

    internal open fun atStartOfDay(date: LocalDate): Instant = error("Should be overridden") //value.atStartOfDay(date)
    internal open fun offsetAtImpl(instant: Instant): UtcOffset = error("Should be overridden")

    internal open fun instantToLocalDateTime(instant: Instant): LocalDateTime = try {
        instant.toLocalDateTimeImpl(offsetAtImpl(instant))
    } catch (e: IllegalArgumentException) {
        throw DateTimeArithmeticException("Instant $instant is not representable as LocalDateTime.", e)
    }

    internal open fun localDateTimeToInstant(dateTime: LocalDateTime): Instant =
        atZone(dateTime).toInstant()

    internal open fun atZone(dateTime: LocalDateTime, preferred: UtcOffset? = null): ZonedDateTime =
        error("Should be overridden")

    override fun equals(other: Any?): Boolean =
        this === other || other is TimeZone && this.id == other.id

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = id
}

internal expect class RegionTimeZone : TimeZone {
    override val id: String
    override fun atStartOfDay(date: LocalDate): Instant
    override fun offsetAtImpl(instant: Instant): UtcOffset
    override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime

    companion object {
        fun of(zoneId: String): RegionTimeZone
        fun currentSystemDefault(): RegionTimeZone
        val availableZoneIds: Set<String>
    }
}

private fun dateWithTimeIntervalSince1970Saturating(epochSeconds: Long): NSDate {
    val date = NSDate.createWithTimeIntervalSince1970(epochSeconds.toDouble())
    return when {
        date.getTimeIntervalSince(NSDate.getDistantPast()) < 0 -> NSDate.getDistantPast()
        date.getTimeIntervalSince(NSDate.getDistantFuture()) > 0 -> NSDate.getDistantFuture()
        else -> date
    }
}

private fun systemDateByLocalDate(zone: NSTimeZone, localDate: NSDate): NSDate? {
    val utc = NSTimeZone.fromGMTSecondsOffset(0)
    /* Now, we say that the date that we initially meant is `date`, only with
       the context of being in a timezone `zone`. */
    return NSDate.createWithTimeIntervalSinceDate(utc.secondsFromGMT.toDouble() - zone.secondsFromGMT.toDouble(), localDate)
}

internal actual class RegionTimeZone(private val value: NSTimeZone, actual override val id: String): TimeZone() {
    actual companion object {
        actual fun of(zoneId: String): RegionTimeZone {
            val abbreviations = NSTimeZone.getAbbreviationDictionary()
            val trueZoneId = abbreviations[zoneId] ?: zoneId
            val zone = NSTimeZone(trueZoneId)
            return RegionTimeZone(zone, zoneId)
        }

        actual fun currentSystemDefault(): RegionTimeZone {
            /* The framework has its own cache of the system timezone. Calls to
            [NSTimeZone systemTimeZone] do not reflect changes to the system timezone
            and instead just return the cached value. Thus, to acquire the current
            system timezone, first, the cache should be cleared.

            This solution is not without flaws, however. In particular, resetting the
            system timezone also resets the default timezone ([NSTimeZone default]) if
            it's the same as the cached system timezone:

                NSTimeZone.defaultTimeZone = [NSTimeZone
                    timeZoneWithName: [[NSTimeZone systemTimeZone] name]];
                NSLog(@"%@", NSTimeZone.defaultTimeZone.name);
                NSLog(@"Change the system time zone, then press Enter");
                getchar();
                [NSTimeZone resetSystemTimeZone];
                NSLog(@"%@", NSTimeZone.defaultTimeZone.name); // will also change

            This is a fairly marginal problem:
                * It is only a problem when the developer deliberately sets the default
                  timezone to the region that just happens to be the one that the user
                  is in, and then the user moves to another region, and the app also
                  uses the system timezone.
                * Since iOS 11, the significance of the default timezone has been
                  de-emphasized. In particular, it is not included in the API for
                  Swift: https://forums.swift.org/t/autoupdating-type-properties/4608/4

            Another possible solution could involve using [NSTimeZone localTimeZone].
            This is documented to reflect the current, uncached system timezone on
            iOS 11 and later:
            https://developer.apple.com/documentation/foundation/nstimezone/1387209-localtimezone
            However:
                * Before iOS 11, this was the same as the default timezone and did not
                  reflect the system timezone.
                * Worse, on a Mac (10.15.5), I failed to get it to work as documented.
                      NSLog(@"%@", NSTimeZone.localTimeZone.name);
                      NSLog(@"Change the system time zone, then press Enter");
                      getchar();
                      // [NSTimeZone resetSystemTimeZone]; // uncomment to make it work
                      NSLog(@"%@", NSTimeZone.localTimeZone.name);
                  The printed strings are the same even if I wait for good 10 minutes
                  before pressing Enter, unless the line with "reset" is uncommented--
                  then the timezone is updated, as it should be. So, for some reason,
                  NSTimeZone.localTimeZone, too, is cached.
                  With no iOS device to test this on, it doesn't seem worth the effort
                  to avoid just resetting the system timezone due to one edge case
                  that's hard to avoid.
            */
            NSTimeZone.resetSystemTimeZone()
            val zone = NSTimeZone.getSystemTimeZone()
            return RegionTimeZone(zone, zone.name)
        }

        actual val availableZoneIds: Set<String>
            get() {
                val set = mutableSetOf("UTC")
                val zones = NSTimeZone.getKnownTimeZoneNames()
                for (zone in zones) {
                    set.add(zone)
                }
                val abbrevs = NSTimeZone.getAbbreviationDictionary()
                for ((key, value) in abbrevs) {
                    if (set.contains(value)) {
                        set.add(key)
                    }
                }
                return set
            }
    }

    actual override fun atStartOfDay(date: LocalDate): Instant {
        val ldt = LocalDateTime(date, LocalTime.MIN)
        val epochSeconds = ldt.toEpochSecond(UtcOffset.ZERO)
        // timezone
        val nsDate = NSDate.createWithTimeIntervalSince1970(epochSeconds.toDouble())
        val newDate = systemDateByLocalDate(value, nsDate)
            ?: throw RuntimeException("Unable to acquire the time of start of day at $nsDate for zone $this")
        val offset = value.getSecondsFromGMTForDate(newDate).toInt()
        /* if `epoch_sec` is not in the range supported by Darwin, assume that it
           is the correct local time for the midnight and just convert it to
           the system time. */
        if (nsDate.getTimeIntervalSince(NSDate.getDistantPast()) < 0 ||
            nsDate.getTimeIntervalSince(NSDate.getDistantFuture()) > 0)
            return Instant(epochSeconds - offset, 0)
        // The ISO-8601 calendar.
        val iso8601 = NSCalendar(NSCalendarIdentifier.ISO8601)
        iso8601.timeZone = value
        // start of the day denoted by `newDate`
        val midnight = iso8601.getStartTime(NSCalendarUnit.Day, newDate)
        return Instant(midnight.timeIntervalSince1970.toLong(), 0)
    }

    actual override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime {
        val epochSeconds = dateTime.toEpochSecond(UtcOffset.ZERO)
        var offset = preferred?.totalSeconds ?: Int.MAX_VALUE
        val transitionDuration = run {
            /* a date in an unspecified timezone, defined by the number of seconds since
               the start of the epoch in *that* unspecified timezone */
            val date = dateWithTimeIntervalSince1970Saturating(epochSeconds)
            val newDate = systemDateByLocalDate(value, date)
                ?: throw RuntimeException("Unable to acquire the offset at $dateTime for zone ${this@RegionTimeZone}")
            // we now know the offset of that timezone at this time.
            offset = value.getSecondsFromGMTForDate(newDate).toInt()
            /* `dateFromComponents` automatically corrects the date to avoid gaps. We
               need to learn which adjustments it performed. */
            (newDate.timeIntervalSince1970.toLong() +
                    offset.toLong() - date.timeIntervalSince1970.toLong()).toInt()
        }
        val correctedDateTime = try {
            dateTime.plusSeconds(transitionDuration)
        } catch (e: java.lang.IllegalArgumentException) {
            throw DateTimeArithmeticException("Overflow whet correcting the date-time to not be in the transition gap", e)
        } catch (e: ArithmeticException) {
            throw RuntimeException("Anomalously long timezone transition gap reported", e)
        }
        return ZonedDateTime(correctedDateTime, this@RegionTimeZone, UtcOffset.ofSeconds(offset))
    }

    actual override fun offsetAtImpl(instant: Instant): UtcOffset {
        val date = dateWithTimeIntervalSince1970Saturating(instant.epochSeconds)
        return UtcOffset.ofSeconds(value.getSecondsFromGMTForDate(date).toInt())
    }

}


@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone internal constructor(public actual val offset: UtcOffset, override val id: String) : TimeZone() {

    public actual constructor(offset: UtcOffset) : this(offset, offset.toString())

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds

    override fun atStartOfDay(date: LocalDate): Instant =
        LocalDateTime(date, LocalTime.MIN).toInstant(offset)

    override fun offsetAtImpl(instant: Instant): UtcOffset = offset

    override fun atZone(dateTime: LocalDateTime, preferred: UtcOffset?): ZonedDateTime =
        ZonedDateTime(dateTime, this, offset)

    override fun instantToLocalDateTime(instant: Instant): LocalDateTime = instant.toLocalDateTime(offset)
    override fun localDateTimeToInstant(dateTime: LocalDateTime): Instant = dateTime.toInstant(offset)
}


public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
    offsetAtImpl(instant)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    timeZone.instantToLocalDateTime(this)

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    toLocalDateTimeImpl(offset)
} catch (e: IllegalArgumentException) {
    throw DateTimeArithmeticException("Instant ${this@toLocalDateTime} is not representable as LocalDateTime", e)
}

internal fun Instant.toLocalDateTimeImpl(offset: UtcOffset): LocalDateTime {
    val localSecond: Long = epochSeconds + offset.totalSeconds // overflow caught later
    val localEpochDay = localSecond.floorDiv(SECONDS_PER_DAY.toLong()).toInt()
    val secsOfDay = localSecond.mod(SECONDS_PER_DAY.toLong()).toInt()
    val date: LocalDate = LocalDate.fromEpochDays(localEpochDay) // may throw
    val time: LocalTime = LocalTime.ofSecondOfDay(secsOfDay, nanosecondsOfSecond)
    return LocalDateTime(date, time)
}

public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    timeZone.localDateTimeToInstant(this)

public actual fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
    Instant(this.toEpochSecond(offset), this.nanosecond)

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
    timeZone.atStartOfDay(this)
