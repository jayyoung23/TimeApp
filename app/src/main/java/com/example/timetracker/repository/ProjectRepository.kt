package com.example.timetracker.repository

import androidx.lifecycle.LiveData
import com.example.timetracker.data.Project
import com.example.timetracker.data.ProjectDao
import java.util.*

/**
 * 项目数据仓库
 * 作为ViewModel和数据源之间的中介，处理与项目相关的所有数据操作
 */
class ProjectRepository(private val projectDao: ProjectDao) {
    
    /**
     * 获取所有项目列表
     * 
     * @return 所有项目的LiveData列表
     */
    val allProjects: LiveData<List<Project>> = projectDao.getAllProjects()
    
    /**
     * 获取当前活跃的项目
     * 
     * @return 当前活跃的项目，如果没有则返回null
     */
    val activeProject: LiveData<Project?> = projectDao.getActiveProject()
    
    /**
     * 根据ID获取项目
     * 
     * @param projectId 项目ID
     * @return 项目对象
     */
    fun getProjectById(projectId: Long): LiveData<Project> {
        return projectDao.getProjectById(projectId)
    }
    
    /**
     * 插入新项目
     * 
     * @param project 要插入的项目对象
     * @return 插入后生成的项目ID
     */
    suspend fun insert(project: Project): Long {
        return projectDao.insert(project)
    }
    
    /**
     * 更新项目信息
     * 
     * @param project 要更新的项目对象
     */
    suspend fun update(project: Project) {
        projectDao.update(project)
    }
    
    /**
     * 删除项目
     * 
     * @param project 要删除的项目对象
     */
    suspend fun delete(project: Project) {
        projectDao.delete(project)
    }
    
    /**
     * 开始项目计时
     * 将所有项目设为非活跃状态，然后将指定项目设为活跃状态
     * 
     * @param projectId 项目ID
     */
    suspend fun startProject(projectId: Long) {
        // 先重置所有项目的活跃状态
        projectDao.resetAllActiveProjects()
        // 设置指定项目为活跃状态，并记录开始时间
        projectDao.setProjectActive(projectId, Date())
    }
    
    /**
     * 停止项目计时
     * 
     * @param projectId 项目ID
     */
    suspend fun stopProject(projectId: Long) {
        projectDao.setProjectInactive(projectId)
    }
    
    /**
     * 设置活跃项目
     * 用于从TimerService中同步活跃项目状态
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     */
    suspend fun setActiveProject(projectId: Long, startTime: Date?) {
        // 先重置所有项目的活跃状态
        projectDao.resetAllActiveProjects()
        // 设置指定项目为活跃状态，并记录开始时间
        if (startTime != null) {
            projectDao.setProjectActive(projectId, startTime)
        }
    }
}