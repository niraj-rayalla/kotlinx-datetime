package kotlinx.datetime

public actual enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public companion object {
        /**
         * @return The [DayOfWeek] for the given day of week number (in range [1, 7]).
         */
        public fun fromDayOfWeek(dayOfWeek: Int): DayOfWeek {
            return if (dayOfWeek in 1..7) {
                DayOfWeek.values()[dayOfWeek - 1]
            }
            else throw IllegalArgumentException("Day of year must be in range [1, 7], but was $dayOfWeek.")
        }
    }
}