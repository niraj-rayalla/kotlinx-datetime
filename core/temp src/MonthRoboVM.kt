/*
 * Copyright 2019-2020 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.datetime

public actual enum class Month {
    JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER;

    public companion object {
        /**
         * @return The [Month] for the given month of year number (in range [1, 12]).
         */
        public fun fromMonthOfYear(monthOfYear: Int): Month {
            return if (monthOfYear in 1..12) {
                Month.values()[monthOfYear - 1]
            }
            else throw IllegalArgumentException("Month of year must be in range [1, 12], but was $monthOfYear.")
        }
    }
}

// From threetenbp
internal fun Month.firstDayOfYear(leapYear: Boolean): Int {
    val leap = if (leapYear) 1 else 0
    return when (this) {
        Month.JANUARY -> 1
        Month.FEBRUARY -> 32
        Month.MARCH -> 60 + leap
        Month.APRIL -> 91 + leap
        Month.MAY -> 121 + leap
        Month.JUNE -> 152 + leap
        Month.JULY -> 182 + leap
        Month.AUGUST -> 213 + leap
        Month.SEPTEMBER -> 244 + leap
        Month.OCTOBER -> 274 + leap
        Month.NOVEMBER -> 305 + leap
        Month.DECEMBER -> 335 + leap
    }
}
