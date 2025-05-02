package com.example.timetracker.viewmodel

import androidx.lifecycle.*
import com.example.timetracker.data.Project
import com.example.timetracker.repository.ProjectRepository
import com.example.timetracker.repository.TimeRecordRepository
import kotlinx.coroutines.launch
import java.util.*

/**
 * 项目ViewModel
 * 负责处理与项目相关的UI逻辑，连接UI和数据仓库
 */
class ProjectViewModel(private val projectRepository: ProjectRepository, 
                       private val timeRecordRepository: TimeRecordRepository) : ViewModel() {
    
    // 所有项目列表
    val allProjects: LiveData<List<Project>> = projectRepository.allProjects
    
    // 当前活跃的项目
    val activeProject: LiveData<Project?> = projectRepository.activeProject
    
    // 选中的项目ID
    private val _selectedProjectId = MutableLiveData<Long>()
    val selectedProjectId: LiveData<Long> = _selectedProjectId
    
    // 选中的项目
    val selectedProject: LiveData<Project> = Transformations.switchMap(selectedProjectId) { id ->
        projectRepository.getProjectById(id)
    }
    
    // 选中项目的今日时长
    val todayDuration: LiveData<Int> = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getTodayDuration(id)
    }
    
    // 选中项目的总时长
    val totalDuration: LiveData<Int> = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getTotalDuration(id)
    }
    
    // 选中项目的打卡天数
    val checkInDays: LiveData<Int> = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getCheckInDays(id)
    }
    
    // 计算日均时长
    val averageDailyDuration: LiveData<Int> = MediatorLiveData<Int>().apply {
        // 添加总时长数据源
        addSource(totalDuration) { total ->
            val days = checkInDays.value ?: 0
            value = timeRecordRepository.calculateAverageDailyDuration(total ?: 0, days)
        }
        // 添加打卡天数数据源
        addSource(checkInDays) { days ->
            val total = totalDuration.value ?: 0
            value = timeRecordRepository.calculateAverageDailyDuration(total, days ?: 0)
        }
    }
    
    // 每日统计数据
    val dailyStats = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getDailyStats(id)
    }
    
    // 每周统计数据
    val weeklyStats = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getWeeklyStats(id)
    }
    
    // 每月统计数据
    val monthlyStats = Transformations.switchMap(selectedProjectId) { id ->
        timeRecordRepository.getMonthlyStats(id)
    }
    
    /**
     * 设置选中的项目ID
     * 
     * @param projectId 项目ID
     */
    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
    }
    
    /**
     * 创建新项目
     * 
     * @param name 项目名称
     * @param dailyGoalSeconds 每日计划时长（秒）
     * @param reminderEnabled 是否开启提醒
     * @param reminderHour 提醒时间（小时）
     * @param reminderMinute 提醒时间（分钟）
     * @return 新创建的项目ID
     */
    fun createProject(name: String, dailyGoalSeconds: Int, 
                     reminderEnabled: Boolean = false, 
                     reminderHour: Int = 9, 
                     reminderMinute: Int = 0): LiveData<Long> {
        val resultId = MutableLiveData<Long>()
        viewModelScope.launch {
            val project = Project(
                name = name,
                dailyGoalSeconds = dailyGoalSeconds,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
            val id = projectRepository.insert(project)
            resultId.postValue(id)
        }
        return resultId
    }
    
    /**
     * 更新项目信息
     * 
     * @param project 要更新的项目对象
     */
    fun updateProject(project: Project) {
        viewModelScope.launch {
            projectRepository.update(project)
        }
    }
    
    /**
     * 删除项目
     * 
     * @param project 要删除的项目对象
     */
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            projectRepository.delete(project)
        }
    }
    
    /**
     * 开始项目计时
     * 
     * @param projectId 项目ID
     */
    fun startProject(projectId: Long) {
        viewModelScope.launch {
            projectRepository.startProject(projectId)
        }
    }
    
    /**
     * 设置活跃项目
     * 用于从TimerService中同步活跃项目状态
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     */
    fun setActiveProject(projectId: Long, startTime: Date?) {
        viewModelScope.launch {
            projectRepository.setActiveProject(projectId, startTime)
        }
    }
    
    /**
     * 停止项目计时并记录时间
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param durationSeconds 持续时间（秒）
     */
    fun stopProjectAndRecord(projectId: Long, startTime: Date, durationSeconds: Int) {
        viewModelScope.launch {
            // 停止项目计时
            projectRepository.stopProject(projectId)
            // 记录时间
            val endTime = Date()
            timeRecordRepository.addTimeRecord(projectId, startTime, endTime, durationSeconds)
        }
    }
    
    /**
     * 计算剩余时间（秒）
     * 每日目标时间减去今日已完成时间
     * 
     * @param dailyGoalSeconds 每日目标时间（秒）
     * @param todayDurationSeconds 今日已完成时间（秒）
     * @return 剩余时间（秒），最小为0
     */
    fun calculateRemainingTime(dailyGoalSeconds: Int, todayDurationSeconds: Int?): Int {
        val completed = todayDurationSeconds ?: 0
        return (dailyGoalSeconds - completed).coerceAtLeast(0)
    }
    
    /**
     * 格式化时间（秒）为可读字符串
     * 格式：HH:MM:SS
     * 
     * @param seconds 时间（秒）
     * @return 格式化后的时间字符串
     */
    fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
    
    /**
     * ViewModel工厂类
     * 用于创建带参数的ViewModel实例
     */
    class Factory(private val projectRepository: ProjectRepository,
                 private val timeRecordRepository: TimeRecordRepository) : ViewModelProvider.Factory {
        
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
                return ProjectViewModel(projectRepository, timeRecordRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}