package com.example.timetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

/**
 * 时间记录数据访问对象接口
 * 定义对时间记录表的所有数据库操作和统计查询
 */
@Dao
interface TimeRecordDao {
    /**
     * 插入新的时间记录
     * 
     * @param timeRecord 要插入的时间记录对象
     * @return 插入后生成的记录ID
     */
    @Insert
    suspend fun insert(timeRecord: TimeRecord): Long
    
    /**
     * 更新时间记录
     * 
     * @param timeRecord 要更新的时间记录对象
     */
    @Update
    suspend fun update(timeRecord: TimeRecord)
    
    /**
     * 删除时间记录
     * 
     * @param timeRecord 要删除的时间记录对象
     */
    @Delete
    suspend fun delete(timeRecord: TimeRecord)
    
    /**
     * 获取项目的所有时间记录
     * 
     * @param projectId 项目ID
     * @return 该项目的所有时间记录列表
     */
    @Query("SELECT * FROM time_records WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getRecordsByProject(projectId: Long): LiveData<List<TimeRecord>>
    
    /**
     * 获取项目在指定日期范围内的所有记录
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 符合条件的时间记录列表
     */
    @Query("SELECT * FROM time_records WHERE projectId = :projectId AND recordDate BETWEEN :startDate AND :endDate ORDER BY startTime DESC")
    fun getRecordsByProjectAndDateRange(projectId: Long, startDate: Date, endDate: Date): LiveData<List<TimeRecord>>
    
    /**
     * 获取项目的累计时长（秒）
     * 
     * @param projectId 项目ID
     * @return 累计时长
     */
    @Query("SELECT SUM(durationSeconds) FROM time_records WHERE projectId = :projectId")
    fun getTotalDurationByProject(projectId: Long): LiveData<Int>
    
    /**
     * 获取项目在指定日期的累计时长（秒）
     * 
     * @param projectId 项目ID
     * @param date 指定日期
     * @return 当日累计时长
     */
    @Query("SELECT SUM(durationSeconds) FROM time_records WHERE projectId = :projectId AND date(recordDate/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    fun getDailyDurationByProject(projectId: Long, date: Date): LiveData<Int>
    
    /**
     * 获取项目的打卡天数
     * 
     * @param projectId 项目ID
     * @return 打卡天数
     */
    @Query("SELECT COUNT(DISTINCT date(recordDate/1000, 'unixepoch')) FROM time_records WHERE projectId = :projectId")
    fun getCheckInDaysByProject(projectId: Long): LiveData<Int>
    
    /**
     * 获取项目按天统计的时长数据
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每天的时长统计数据列表
     */
    @Query("SELECT date(recordDate/1000, 'unixepoch') as dateStr, SUM(durationSeconds) as totalDuration " +
           "FROM time_records " +
           "WHERE projectId = :projectId AND recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY dateStr " +
           "ORDER BY dateStr ASC")
    fun getDailyStatsByProject(projectId: Long, startDate: Date, endDate: Date): LiveData<List<DailyStat>>
    
    /**
     * 获取项目按周统计的时长数据
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每周的时长统计数据列表
     */
    @Query("SELECT strftime('%Y-%W', recordDate/1000, 'unixepoch') as weekStr, SUM(durationSeconds) as totalDuration " +
           "FROM time_records " +
           "WHERE projectId = :projectId AND recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY weekStr " +
           "ORDER BY weekStr ASC")
    fun getWeeklyStatsByProject(projectId: Long, startDate: Date, endDate: Date): LiveData<List<WeeklyStat>>
    
    /**
     * 获取项目按月统计的时长数据
     * 
     * @param projectId 项目ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 每月的时长统计数据列表
     */
    @Query("SELECT strftime('%Y-%m', recordDate/1000, 'unixepoch') as monthStr, SUM(durationSeconds) as totalDuration " +
           "FROM time_records " +
           "WHERE projectId = :projectId AND recordDate BETWEEN :startDate AND :endDate " +
           "GROUP BY monthStr " +
           "ORDER BY monthStr ASC")
    fun getMonthlyStatsByProject(projectId: Long, startDate: Date, endDate: Date): LiveData<List<MonthlyStat>>
}

/**
 * 日统计数据类
 */
data class DailyStat(val dateStr: String, val totalDuration: Int)

/**
 * 周统计数据类
 */
data class WeeklyStat(val weekStr: String, val totalDuration: Int)

/**
 * 月统计数据类
 */
data class MonthlyStat(val monthStr: String, val totalDuration: Int)