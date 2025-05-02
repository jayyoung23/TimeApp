package com.example.timetracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timetracker.R
import com.example.timetracker.data.Project
import com.example.timetracker.databinding.ItemProjectBinding
import java.util.*

/**
 * 项目列表适配器
 * 用于在RecyclerView中显示项目列表，并处理项目的点击和计时事件
 */
class ProjectAdapter(
    private val onProjectClick: (Project) -> Unit,
    private val onTrackingButtonClick: (Project) -> Unit,
    private val formatTimeFunction: (Int) -> String,
    private val calculateRemainingTimeFunction: (Int, Int?) -> Int
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    // 当前活跃的项目
    private var activeProject: Project? = null
    
    /**
     * 创建ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }
    
    /**
     * 绑定ViewHolder数据
     */
    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        val project = getItem(position)
        holder.bind(project)
    }
    
    /**
     * 设置活跃项目
     * 
     * @param project 活跃的项目，如果为null表示没有活跃项目
     */
    fun setActiveProject(project: Project?) {
        val oldActiveProject = activeProject
        activeProject = project
        
        // 更新旧的活跃项目
        oldActiveProject?.let { old ->
            val position = currentList.indexOfFirst { it.id == old.id }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
        
        // 更新新的活跃项目
        project?.let { new ->
            val position = currentList.indexOfFirst { it.id == new.id }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }
    }
    
    /**
     * 项目ViewHolder
     */
    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        init {
            // 设置项目点击事件
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProjectClick(getItem(position))
                }
            }
            
            // 设置开始/停止按钮点击事件
            binding.buttonTracking.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTrackingButtonClick(getItem(position))
                }
            }
        }
        
        /**
         * 绑定项目数据到视图
         * 
         * @param project 项目数据
         */
        fun bind(project: Project) {
            binding.textProjectName.text = project.name
            
            // 设置按钮图标
            val buttonIcon = if (project.isActive) {
                R.drawable.ic_stop
            } else {
                R.drawable.ic_play
            }
            binding.buttonTracking.setImageResource(buttonIcon)
            
            // 获取今日已完成时间
            val todayDurationSeconds = calculateTodayDuration(project)
            
            // 设置进度条
            val progress = (todayDurationSeconds * 100 / project.dailyGoalSeconds).coerceIn(0, 100)
            binding.progressBar.progress = progress
            
            // 设置时间信息文本
            val completedTimeText = formatTimeFunction(todayDurationSeconds)
            val goalTimeText = formatTimeFunction(project.dailyGoalSeconds)
            binding.textTimeInfo.text = "$completedTimeText / $goalTimeText"
        }
        
        /**
         * 计算今日已完成时间（秒）
         * 
         * @param project 项目
         * @return 今日已完成时间（秒）
         */
        private fun calculateTodayDuration(project: Project): Int {
            // 如果项目正在计时中，需要加上当前会话的时间
            return if (project.isActive && project.lastStartTime != null) {
                // 计算当前会话时长（秒）
                val currentSessionSeconds = ((Date().time - project.lastStartTime!!.time) / 1000).toInt()
                // 加上之前已记录的时间
                currentSessionSeconds
            } else {
                // 如果项目未在计时中，直接返回0（实际项目中应从数据库获取今日已记录时间）
                0
            }
        }
    }
}

/**
 * 项目差异比较回调
 * 用于优化RecyclerView的更新
 */
class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
    override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean {
        return oldItem == newItem
    }
}