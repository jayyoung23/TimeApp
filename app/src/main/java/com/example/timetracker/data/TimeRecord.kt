package com.example.timetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

/**
 * 时间记录实体类
 * 用于存储用户的打卡记录，与Project形成一对多关系
 */
@Entity(
    tableName = "time_records",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class TimeRecord(
    // 记录ID，主键，自动生成
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 关联的项目ID
    val projectId: Long,
    
    // 开始时间
    val startTime: Date,
    
    // 结束时间
    val endTime: Date,
    
    // 持续时间（秒）
    val durationSeconds: Int,
    
    // 记录日期（用于按日期分组统计）
    val recordDate: Date
)