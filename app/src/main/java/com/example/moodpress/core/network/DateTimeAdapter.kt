package com.example.moodpress.core.network

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.gson.*
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Adapter tùy chỉnh để xử lý nhiều định dạng Date mà backend (Python) gửi về.
 */
class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {

    // Danh sách các định dạng mà chúng ta chấp nhận
    private val DATE_FORMATS = arrayOf(
        // 1. Định dạng 6 số lẻ (microseconds), không múi giờ (lỗi mới)
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",

        // 2. Định dạng 3 số lẻ (milliseconds), có múi giờ ISO (lỗi cũ)
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

        // 3. Định dạng 3 số lẻ, múi giờ 'Z' (UTC)
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

        // 4. Định dạng không có số lẻ, có múi giờ ISO
        "yyyy-MM-dd'T'HH:mm:ssXXX",

        // 5. Định dạng không có số lẻ, múi giờ 'Z' (UTC)
        "yyyy-MM-dd'T'HH:mm:ss'Z'",

        "yyyy-MM-dd'T'HH:mm:ss",

        "yyyy-MM-dd"
    )

    // Đây là định dạng CHUẨN chúng ta dùng để GỬI (serialize)
    private val SERIALIZE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT)

    init {
        // Luôn GỬI ở múi giờ UTC để thống nhất
        SERIALIZE_FORMAT.timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Hàm này dùng khi GỬI dữ liệu LÊN backend (Android -> Server)
     */
    override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return if (src == null) {
            JsonNull.INSTANCE
        } else {
            // Gửi đi ở định dạng ISO 8601 chuẩn (3 số lẻ + múi giờ)
            JsonPrimitive(SERIALIZE_FORMAT.format(src))
        }
    }

    /**
     * Hàm này dùng khi NHẬN dữ liệu TỪ backend (Server -> Android)
     */
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        if (json == null || json.isJsonNull) {
            return null
        }

        val dateString = json.asString

        // Thử lần lượt các định dạng trong danh sách
        for (format in DATE_FORMATS) {
            try {
                val formatter = SimpleDateFormat(format, Locale.ROOT)
                // Nếu parse thành công, trả về ngay
                return formatter.parse(dateString)
            } catch (e: ParseException) {
            }
        }

        // Nếu thử hết mà vẫn lỗi
        throw JsonParseException("Không thể parse ngày: \"$dateString\". Đã thử ${DATE_FORMATS.size} định dạng.")
    }
}