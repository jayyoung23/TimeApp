package com.example.timetracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * 项目数据访问对象接口
 * 定义对项目表的所有数据库操作
 */
@Dao
interface ProjectDao {
    /**
     * 插入新项目
     * 
     * @param project 要插入的项目对象
     * @return 插入后生成的项目ID
     */
    @Insert
    suspend fun insert(project: Project): Long
    
    /**
     * 更新项目信息
     * 
     * @param project 要更新的项目对象
     */
    @Update
    suspend fun update(project: Project)
    
    /**
     * 删除项目
     * 
     * @param project 要删除的项目对象
     */
    @Delete
    suspend fun delete(project: Project)
    
    /**
     * 根据ID获取项目
     * 
     * @param projectId 项目ID
     * @return 项目对象
     */
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: Long): LiveData<Project>
    
    /**
     * 获取所有项目列表
     * 
     * @return 所有项目的LiveData列表
     */
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): LiveData<List<Project>>
    
    /**
     * 获取当前活跃的项目
     * 
     * @return 当前活跃的项目，如果没有则返回null
     */
    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    fun getActiveProject(): LiveData<Project?>
    
    /**
     * 重置所有项目的活跃状态
     */
    @Query("UPDATE projects SET isActive = 0, lastStartTime = NULL")
    suspend fun resetAllActiveProjects()
    
    /**
     * 设置项目为活跃状态
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     */
    @Query("UPDATE projects SET isActive = 1, lastStartTime = :startTime WHERE id = :projectId")
    suspend fun setProjectActive(projectId: Long, startTime: Date)
    
    /**
     * 设置项目为非活跃状态
     * 
     * @param projectId 项目ID
     */
    @Query("UPDATE projects SET isActive = 0, lastStartTime = NULL WHERE id = :projectId")
    suspend fun setProjectInactive(projectId: Long)
}