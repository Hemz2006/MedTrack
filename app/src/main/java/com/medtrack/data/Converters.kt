package com.medtrack.data

import androidx.room.TypeConverter
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val ingredientList: KSerializer<List<Ingredient>> = ListSerializer(Ingredient.serializer())

    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun ingredientsToJson(value: List<Ingredient>): String = json.encodeToString(ingredientList, value)
    @TypeConverter fun jsonToIngredients(value: String): List<Ingredient> = json.decodeFromString(ingredientList, value)
    @TypeConverter fun scheduleToJson(value: Schedule): String = ScheduleJson.encode(value)
    @TypeConverter fun jsonToSchedule(value: String): Schedule = ScheduleJson.decode(value)
}
