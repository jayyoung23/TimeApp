package com.example.timetracker.repository

import androidx.lifecycle.LiveData
import com.example.timetracker.data.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 时间记录数据仓库
 * 作为ViewModel和数据源之间的中介，处理与时间记录相关的所有数据操作和统计功能
 */
class TimeRecordRepository(private val timeRecordDao: TimeRecordDao) {
    
    /**
     * 获取项目的所有时间记录
     * 
     * @param projectId 项目ID
     * @return 该项目的所有时间记录列表
     */
    fun getRecordsByProject(projectId: Long): LiveData<List<TimeRecord>> {
        return timeRecordDao.getRecordsByProject(projectId)
    }
    
    /**
     * 添加新的时间记录
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param durationSeconds 持续时间（秒）
     * @return 插入后生成的记录ID
     */
    suspend fun addTimeRecord(projectId: Long, startTime: Date, endTime: Date, durationSeconds: Int): Long {
        val timeRecord = TimeRecord(
            projectId = projectId,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            recordDate = startTime // 使用开始时间作为记录日期
        )
        return timeRecordDao.insert(timeRecord)
    }
    
    /**
     * 获取项目的累计时长（秒）
     * 
     * @param projectId 项目ID
     * @return 累计时长
     */
    fun getTotalDuration(projectId: Long): LiveData<Int> {
        return timeRecordDao.getTotalDurationByProject(projectId)
    }
    
    /**
     * 获取项目在当天的累计时长（秒）
     * 
     * @param projectId 项目ID
     * @return 当日累计时长
     */
    fun getTodayDuration(projectId: Long): LiveData<Int> {
        return timeRecordDao.getDailyDurationByProject(projectId, Date())
    }
    
    /**
     * 获取项目的打卡天数
     * 
     * @param projectId 项目ID
     * @return 打卡天数
     */
    fun getCheckInDays(projectId: Long): LiveData<Int> {
        return timeRecordDao.getCheckInDaysByProject(projectId)
    }
    
    /**
     * 获取项目在指定日期范围内的记录
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 符合条件的时间记录列表
     */
    fun getRecordsByDateRange(projectId: Long, startDate: Date, endDate: Date): LiveData<List<TimeRecord>> {
        return timeRecordDao.getRecordsByProjectAndDateRange(projectId, startDate, endDate)
    }
    
    /**
     * 获取项目按天统计的数据
     * 
     * @param projectId 项目ID
     * @param days 天数，默认为7天
     * @return 每天的时长统计数据列表
     */
    fun getDailyStats(projectId: Long, days: Int = 7): LiveData<List<DailyStat>> {
        val endDate = Date()
        val startDate = Date(endDate.time - TimeUnit.DAYS.toMillis(days.toLong()))
        return timeRecordDao.getDailyStatsByProject(projectId, startDate, endDate)
    }
    
    /**
     * 获取项目按周统计的数据
     * 
     * @param projectId 项目ID
     * @param weeks 周数，默认为4周
     * @return 每周的时长统计数据列表
     */
    fun getWeeklyStats(projectId: Long, weeks: Int = 4): LiveData<List<WeeklyStat>> {
        val endDate = Date()
        val startDate = Date(endDate.time - TimeUnit.DAYS.toMillis((weeks * 7).toLong()))
        return timeRecordDao.getWeeklyStatsByProject(projectId, startDate, endDate)
    }
    
    /**
     * 获取项目按月统计的数据
     * 
     * @param projectId 项目ID
     * @param months 月数，默认为6个月
     * @return 每月的时长统计数据列表
     */
    fun getMonthlyStats(projectId: Long, months: Int = 6): LiveData<List<MonthlyStat>> {
        val endDate = Date()
        val calendar = Calendar.getInstance()
        calendar.time = endDate
        calendar.add(Calendar.MONTH, -months)
        val startDate = calendar.time
        return timeRecordDao.getMonthlyStatsByProject(projectId, startDate, endDate)
    }
    
    /**
     * 计算项目的日均时长（秒）
     * 
     * @param totalDuration 总时长（秒）
     * @param checkInDays 打卡天数
     * @return 日均时长（秒）
     */
    fun calculateAverageDailyDuration(totalDuration: Int, checkInDays: Int): Int {
        return if (checkInDays > 0) totalDuration / checkInDays else 0
    }
}