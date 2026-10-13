package com.medtrack.schedule

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** java.time types as ISO strings (DayOfWeek as its enum name). */
abstract class StringSerializer<T : Any>(
    name: String,
    private val format: (T) -> String,
    private val parse: (String) -> T,
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor("com.medtrack.$name", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(format(value))
    override fun deserialize(decoder: Decoder): T = parse(decoder.decodeString())
}

object LocalTimeSerializer : StringSerializer<LocalTime>("LocalTime", LocalTime::toString, LocalTime::parse)
object LocalDateSerializer : StringSerializer<LocalDate>("LocalDate", LocalDate::toString, LocalDate::parse)
object LocalDateTimeSerializer : StringSerializer<LocalDateTime>("LocalDateTime", LocalDateTime::toString, LocalDateTime::parse)
object DayOfWeekSerializer : StringSerializer<DayOfWeek>("DayOfWeek", DayOfWeek::name, DayOfWeek::valueOf)
