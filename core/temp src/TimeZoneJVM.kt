/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:JvmMultifileClass
@file:JvmName("TimeZoneKt")

package kotlinx.datetime

import kotlinx.datetime.serializers.*
import kotlinx.serialization.Serializable
import org.joda.time.LocalTime
import org.joda.time.DateTimeZone as jtZoneOffset

@Serializable(with = TimeZoneSerializer::class)
public actual open class TimeZone internal constructor(internal val zoneId: jtZoneOffset) {
    public actual val id: String get() = zoneId.id


    // experimental member-extensions
    public actual fun Instant.toLocalDateTime(): LocalDateTime = toLocalDateTime(this@TimeZone)
    public actual fun LocalDateTime.toInstant(): Instant = toInstant(this@TimeZone)

    override fun equals(other: Any?): Boolean =
        (this === other) || (other is TimeZone && this.zoneId == other.zoneId)

    override fun hashCode(): Int = zoneId.hashCode()

    override fun toString(): String = zoneId.toString()

    public actual companion object {
        public actual fun currentSystemDefault(): TimeZone = ofZone(jtZoneOffset.getDefault())
        public actual val UTC: FixedOffsetTimeZone = UtcOffset(jtZoneOffset.UTC).asTimeZone()

        public actual fun of(zoneId: String): TimeZone = try {
            ofZone(jtZoneOffset.forID(zoneId))
        } catch (e: Exception) {
            if (e is DateTimeException) throw IllegalTimeZoneException(e)
            throw e
        }

        internal fun ofZone(zoneId: jtZoneOffset): TimeZone = when {
            zoneId is jtZoneOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId))
            zoneId.isFixedOffset ->
                FixedOffsetTimeZone(UtcOffset(zoneId.getOffset(0)), zoneId)
            else ->
                TimeZone(zoneId)
        }

        public actual val availableZoneIds: Set<String> get() = jtZoneOffset.getAvailableIDs()
    }
}

// Workaround for https://issuetracker.google.com/issues/203956057
private val jtZoneOffset.isFixedOffset: Boolean
    get() = this.isFixed

@Serializable(with = FixedOffsetTimeZoneSerializer::class)
public actual class FixedOffsetTimeZone
internal constructor(public actual val offset: UtcOffset, zoneId: jtZoneOffset): TimeZone(zoneId) {

    public actual constructor(offset: UtcOffset) : this(offset, offset.zoneOffset)

    @Deprecated("Use offset.totalSeconds", ReplaceWith("offset.totalSeconds"))
    public actual val totalSeconds: Int get() = offset.totalSeconds
}

public actual fun TimeZone.offsetAt(instant: Instant): UtcOffset =
    zoneId.getOffset(instant.value).let(::UtcOffset)

public actual fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime = try {
    org.joda.time.LocalDateTime(this.value, timeZone.zoneId).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}

internal actual fun Instant.toLocalDateTime(offset: UtcOffset): LocalDateTime = try {
    org.joda.time.LocalDateTime(this.value, offset.zoneOffset).let(::LocalDateTime)
} catch (e: DateTimeException) {
    throw DateTimeArithmeticException(e)
}


public actual fun LocalDateTime.toInstant(timeZone: TimeZone): Instant =
    this.value.toDateTime(timeZone.zoneId).toInstant().toKotlinInstant()

public actual fun LocalDateTime.toInstant(offset: UtcOffset): Instant =
    this.value.toDateTime(offset.zoneOffset).toInstant().toKotlinInstant()

public actual fun LocalDate.atStartOfDayIn(timeZone: TimeZone): Instant =
    this.value.toDateTime(LocalTime.MIDNIGHT, timeZone.zoneId).toInstant().toKotlinInstant()