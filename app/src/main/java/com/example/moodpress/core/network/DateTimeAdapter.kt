package com.example.moodpress.core.network

import com.google.gson.*
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {

    companion object {
        private const val SERIALIZE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        private val DATE_FORMATS = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",  // Python microseconds (6 số lẻ)
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",  // ISO with timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",  // ISO UTC literal
            "yyyy-MM-dd'T'HH:mm:ssXXX",      // No millis, timezone
            "yyyy-MM-dd'T'HH:mm:ss'Z'",      // No millis, UTC literal
            "yyyy-MM-dd'T'HH:mm:ss",         // Local datetime
            "yyyy-MM-dd"                     // Date only
        )
    }

    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if (src == null) return JsonNull.INSTANCE
        val formatter = SimpleDateFormat(SERIALIZE_PATTERN, Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return JsonPrimitive(formatter.format(src))
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        if (json == null || json.isJsonNull) return null

        val dateString = json.asString.trim()

        for (format in DATE_FORMATS) {
            try {
                val formatter = SimpleDateFormat(format, Locale.US)
                if (dateString.endsWith("Z")) {
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                }

                return formatter.parse(dateString)
            } catch (e: ParseException) {
            }
        }
        System.err.println("DateTypeAdapter: Không thể parse ngày '$dateString'")
        return null
    }
}