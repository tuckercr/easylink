package com.fangjet.launcher.data.local

import androidx.room.TypeConverter
import com.fangjet.launcher.domain.model.MedicationColor
import com.fangjet.launcher.domain.model.ReminderAction
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Room TypeConverters for java.time types and domain enums.
 *
 * Room only natively stores primitives and Strings. These converters let us
 * use rich types in our entities without sacrificing type safety.
 *
 * Format choices:
 *  - [LocalTime] → "HH:mm"    (human-readable in SQLite browser)
 *  - [LocalDateTime] → ISO-8601 (unambiguous, sortable)
 *  - [Set<DayOfWeek>] → "1,2,3,4,5" (DayOfWeek.value — Monday=1, Sunday=7)
 *  - [List<LocalTime>] → "08:00,14:00,20:00" (comma-separated)
 */
class MedicationConverters {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // ── LocalTime ─────────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.format(timeFormatter)

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it, timeFormatter) }

    // ── List<LocalTime> ───────────────────────────────────────────────────────

    @TypeConverter
    fun fromLocalTimeList(times: List<LocalTime>?): String = times?.joinToString(",") { it.format(timeFormatter) } ?: ""

    @TypeConverter
    fun toLocalTimeList(value: String?): List<LocalTime> =
        value
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { LocalTime.parse(it, timeFormatter) }
            ?.sorted()
            ?: emptyList()

    // ── Set<DayOfWeek> ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String = days?.joinToString(",") { it.value.toString() } ?: ""

    @TypeConverter
    fun toDayOfWeekSet(value: String?): Set<DayOfWeek> =
        value
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { DayOfWeek.of(it.toInt()) }
            ?.toSet()
            ?: emptySet()

    // ── LocalDateTime ─────────────────────────────────────────────────────────

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString() // ISO-8601 via LocalDateTime.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }

    // ── MedicationColor ───────────────────────────────────────────────────────

    @TypeConverter
    fun fromMedicationColor(color: MedicationColor?): String? = color?.name

    @TypeConverter
    fun toMedicationColor(value: String?): MedicationColor? =
        value?.let { runCatching { MedicationColor.valueOf(it) }.getOrDefault(MedicationColor.BLUE) }

    // ── ReminderAction ────────────────────────────────────────────────────────

    @TypeConverter
    fun fromReminderAction(action: ReminderAction?): String? = action?.name

    @TypeConverter
    fun toReminderAction(value: String?): ReminderAction? =
        value?.let {
            runCatching {
                ReminderAction.valueOf(it)
            }.getOrNull()
        }
}
