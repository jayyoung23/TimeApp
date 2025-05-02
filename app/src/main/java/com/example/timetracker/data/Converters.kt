package com.example.timetracker.data

import androidx.room.TypeConverter
import java.util.*

/**
 * 数据类型转换器
 * 用于Room数据库中Java日期类型与Long类型的相互转换
 */
class Converters {
    /**
     * 将时间戳转换为Date对象
     * 
     * @param value 时间戳（毫秒）
     * @return Date对象，如果时间戳为null则返回null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * 将Date对象转换为时间戳
     * 
     * @param date Date对象
     * @return 时间戳（毫秒），如果Date为null则返回null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}