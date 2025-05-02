package com.example.timetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * 项目实体类
 * 用于存储用户创建的项目信息
 */
@Entity(tableName = "projects")
data class Project(
    // 项目ID，主键，自动生成
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // 项目名称
    val name: String,
    
    // 每日计划时长（秒）
    val dailyGoalSeconds: Int,
    
    // 是否开启提醒
    val reminderEnabled: Boolean = false,
    
    // 提醒时间（小时，24小时制）
    val reminderHour: Int = 9,
    
    // 提醒时间（分钟）
    val reminderMinute: Int = 0,
    
    // 创建日期
    val createdDate: Date = Date(),
    
    // 是否正在进行中
    var isActive: Boolean = false,
    
    // 上次开始时间（用于计算当前会话时长）
    var lastStartTime: Date? = null
)