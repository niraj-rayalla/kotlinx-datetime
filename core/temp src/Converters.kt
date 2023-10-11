/*
 * Copyright 2019-2022 JetBrains s.r.o. and contributors.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

/**
 * Converts this [kotlinx.datetime.Instant][Instant] value to a [org.joda.time.Instant][org.joda.time.Instant] value.
 */
public fun Instant.toJavaInstant(): org.joda.time.Instant = this.value

/**
 * Converts this [org.joda.time.Instant][org.joda.time.Instant] value to a [kotlinx.datetime.Instant][Instant] value.
 */
public fun org.joda.time.Instant.toKotlinInstant(): Instant = Instant(this)


/**
 * Converts this [kotlinx.datetime.LocalDateTime][LocalDateTime] value to a [org.joda.time.LocalDateTime][org.joda.time.LocalDateTime] value.
 */
public fun LocalDateTime.toJavaLocalDateTime(): org.joda.time.LocalDateTime = this.value

/**
 * Converts this [org.joda.time.LocalDateTime][org.joda.time.LocalDateTime] value to a [kotlinx.datetime.LocalDateTime][LocalDateTime] value.
 */
public fun org.joda.time.LocalDateTime.toKotlinLocalDateTime(): LocalDateTime = LocalDateTime(this)

/**
 * Converts this [kotlinx.datetime.LocalDateTime][LocalTime] value to a [org.joda.time.LocalTime][org.joda.time.LocalTime] value.
 */
public fun LocalTime.toJavaLocalTime(): org.joda.time.LocalTime = this.value

/**
 * Converts this [org.joda.time.LocalTime][org.joda.time.LocalTime] value to a [kotlinx.datetime.LocalTime][LocalTime] value.
 */
public fun org.joda.time.LocalTime.toKotlinLocalTime(): LocalTime = LocalTime(this)


/**
 * Converts this [kotlinx.datetime.LocalDate][LocalDate] value to a [org.joda.time.LocalDate][org.joda.time.LocalDate] value.
 */
public fun LocalDate.toJavaLocalDate(): org.joda.time.LocalDate = this.value

/**
 * Converts this [org.joda.time.LocalDate][org.joda.time.LocalDate] value to a [kotlinx.datetime.LocalDate][LocalDate] value.
 */
public fun org.joda.time.LocalDate.toKotlinLocalDate(): LocalDate = LocalDate(this)


/**
 * Converts this [kotlinx.datetime.DatePeriod][DatePeriod] value to a [org.joda.time.Period][org.joda.time.Period] value.
 */
public fun DatePeriod.toJavaPeriod(): org.joda.time.Period = org.joda.time.Period(this.years, this.months, 0, this.days, 0, 0, 0, 0)

/**
 * Converts this [org.joda.time.Period][org.joda.time.Period] value to a [kotlinx.datetime.DatePeriod][DatePeriod] value.
 */
public fun org.joda.time.Period.toKotlinDatePeriod(): DatePeriod = DatePeriod(this.years, this.months, this.days)


/**
 * Converts this [kotlinx.datetime.TimeZone][TimeZone] value to a [org.joda.time.ZoneId][org.joda.time.ZoneId] value.
 */
public fun TimeZone.toJavaZoneId(): org.joda.time.DateTimeZone = this.zoneId

/**
 * Converts this [org.joda.time.ZoneId][org.joda.time.ZoneId] value to a [kotlinx.datetime.TimeZone][TimeZone] value.
 */
public fun org.joda.time.DateTimeZone.toKotlinTimeZone(): TimeZone = TimeZone.ofZone(this)


/**
 * Converts this [kotlinx.datetime.FixedOffsetTimeZone][FixedOffsetTimeZone] value to a [org.joda.time.ZoneOffset][org.joda.time.ZoneOffset] value.
 */
public fun FixedOffsetTimeZone.toJavaZoneOffset(): org.joda.time.DateTimeZone = this.offset.zoneOffset

/**
 * Converts this [org.joda.time.ZoneOffset][org.joda.time.ZoneOffset] value to a [kotlinx.datetime.FixedOffsetTimeZone][FixedOffsetTimeZone] value.
 */
public fun org.joda.time.DateTimeZone.toKotlinFixedOffsetTimeZone(): FixedOffsetTimeZone = FixedOffsetTimeZone(UtcOffset(this))

@Deprecated("Use toKotlinFixedOffsetTimeZone() instead.", ReplaceWith("this.toKotlinFixedOffsetTimeZone()"))
public fun org.joda.time.DateTimeZone.toKotlinZoneOffset(): FixedOffsetTimeZone = toKotlinFixedOffsetTimeZone()

/**
 * Converts this [kotlinx.datetime.UtcOffset][UtcOffset] value to a [org.joda.time.ZoneOffset][org.joda.time.ZoneOffset] value.
 */
public fun UtcOffset.toJavaZoneOffset(): org.joda.time.DateTimeZone = this.zoneOffset

/**
 * Converts this [org.joda.time.ZoneOffset][org.joda.time.ZoneOffset] value to a [kotlinx.datetime.UtcOffset][UtcOffset] value.
 */
public fun org.joda.time.DateTimeZone.toKotlinUtcOffset(): UtcOffset = UtcOffset(this)