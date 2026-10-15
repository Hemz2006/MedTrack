package com.medtrack.data

import androidx.room.TypeConverter
import com.medtrack.schedule.Schedule
import com.medtrack.schedule.ScheduleJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val ingredientList: KSerializer<List<Ingredient>> = ListSerializer(Ingredient.serializer())
    private val stringList: KSerializer<List<String>> = ListSerializer(String.serializer())

    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun dateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
    @TypeConverter fun ingredientsToJson(value: List<Ingredient>): String = json.encodeToString(ingredientList, value)
    @TypeConverter fun jsonToIngredients(value: String): List<Ingredient> = json.decodeFromString(ingredientList, value)
    @TypeConverter fun stringsToJson(value: List<String>): String = json.encodeToString(stringList, value)
    @TypeConverter fun jsonToStrings(value: String): List<String> = json.decodeFromString(stringList, value)
    @TypeConverter fun stepsToJson(value: List<Schedule>): String = ScheduleJson.encodeSteps(value)
    @TypeConverter fun jsonToSteps(value: String): List<Schedule> = ScheduleJson.decodeSteps(value)
}
