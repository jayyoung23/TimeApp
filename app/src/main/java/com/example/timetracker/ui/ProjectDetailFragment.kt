package com.example.timetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.timetracker.R
import com.example.timetracker.databinding.FragmentProjectDetailBinding
import com.example.timetracker.service.TimerService
import com.example.timetracker.viewmodel.ProjectViewModel

/**
 * 项目详情Fragment
 * 显示项目的详细信息和统计数据
 */
class ProjectDetailFragment : Fragment() {

    private var _binding: FragmentProjectDetailBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProjectViewModel
    
    // 获取导航参数（项目ID）
    private val args: ProjectDetailFragmentArgs by navArgs()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取ViewModel实例
        viewModel = (activity as MainActivity).viewModel
        
        // 设置项目ID
        viewModel.selectProject(args.projectId)
        
        // 设置返回按钮点击事件
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // 设置开始/停止按钮点击事件
        binding.buttonStartStop.setOnClickListener {
            val project = viewModel.selectedProject.value ?: return@setOnClickListener
            val timerService = (activity as MainActivity).getTimerService()
            
            if (project.isActive) {
                // 如果项目正在计时，停止计时
                timerService?.let { service ->
                    if (service.isTimerRunning()) {
                        // 停止服务中的计时器
                        val durationSeconds = service.stopTimer()
                        // 记录时间
                        project.lastStartTime?.let { startTime ->
                            viewModel.stopProjectAndRecord(project.id, startTime, durationSeconds)
                        }
                    } else {
                        // 如果服务没有运行但项目状态为活跃，直接更新项目状态
                        project.lastStartTime?.let { startTime ->
                            val durationSeconds = ((System.currentTimeMillis() - startTime.time) / 1000).toInt()
                            viewModel.stopProjectAndRecord(project.id, startTime, durationSeconds)
                        }
                    }
                } ?: run {
                    // 如果服务未绑定，直接更新项目状态
                    project.lastStartTime?.let { startTime ->
                        val durationSeconds = ((System.currentTimeMillis() - startTime.time) / 1000).toInt()
                        viewModel.stopProjectAndRecord(project.id, startTime, durationSeconds)
                    }
                }
            } else {
                // 如果项目未计时，开始计时
                timerService?.let { service ->
                    // 启动服务中的计时器
                    service.startTimer(project)
                    // 更新项目状态
                    viewModel.startProject(project.id)
                    
                    // 添加计时监听器
                    service.addListener(object : TimerService.TimerListener {
                        override fun onTimerTick(elapsedSeconds: Int) {
                            // 可以在这里更新UI，如果需要
                        }
                    })
                } ?: run {
                    // 如果服务未绑定，直接更新项目状态
                    viewModel.startProject(project.id)
                }
            }
        }
        
        // 设置编辑按钮点击事件
        binding.buttonEdit.setOnClickListener {
            // TODO: 导航到编辑项目页面
            // val action = ProjectDetailFragmentDirections.actionProjectDetailFragmentToEditProjectFragment(args.projectId)
            // findNavController().navigate(action)
        }
        
        // 设置删除按钮点击事件
        binding.buttonDelete.setOnClickListener {
            val project = viewModel.selectedProject.value ?: return@setOnClickListener
            viewModel.deleteProject(project)
            findNavController().navigateUp()
        }
        
        // 观察项目数据变化
        viewModel.selectedProject.observe(viewLifecycleOwner, Observer { project ->
            project?.let {
                // 更新项目名称
                binding.textProjectName.text = it.name
                
                // 更新每日目标时间
                val goalTimeText = viewModel.formatTime(it.dailyGoalSeconds)
                binding.textDailyGoal.text = "每日目标：$goalTimeText"
                
                // 更新开始/停止按钮状态
                updateStartStopButton(it.isActive)
            }
        })
        
        // 观察今日时长变化
        viewModel.todayDuration.observe(viewLifecycleOwner, Observer { duration ->
            val durationText = viewModel.formatTime(duration ?: 0)
            binding.textTodayDuration.text = durationText
            
            // 更新进度条
            val project = viewModel.selectedProject.value
            project?.let {
                val progress = ((duration ?: 0) * 100 / it.dailyGoalSeconds).coerceIn(0, 100)
                binding.progressBarToday.progress = progress
            }
        })
        
        // 观察总时长变化
        viewModel.totalDuration.observe(viewLifecycleOwner, Observer { duration ->
            val durationText = viewModel.formatTime(duration ?: 0)
            binding.textTotalDuration.text = durationText
        })
        
        // 观察打卡天数变化
        viewModel.checkInDays.observe(viewLifecycleOwner, Observer { days ->
            binding.textCheckInDays.text = days?.toString() ?: "0"
        })
        
        // 观察日均时长变化
        viewModel.averageDailyDuration.observe(viewLifecycleOwner, Observer { duration ->
            val durationText = viewModel.formatTime(duration ?: 0)
            binding.textAverageDuration.text = durationText
        })
        
        // TODO: 观察统计数据变化，更新图表
    }
    
    /**
     * 更新开始/停止按钮状态
     * 
     * @param isActive 项目是否处于活跃状态
     */
    private fun updateStartStopButton(isActive: Boolean) {
        if (isActive) {
            // 如果项目正在计时，显示停止按钮
            binding.buttonStartStop.text = "停止"
            binding.buttonStartStop.setBackgroundResource(R.drawable.bg_button_stop)
        } else {
            // 如果项目未计时，显示开始按钮
            binding.buttonStartStop.text = "开始"
            binding.buttonStartStop.setBackgroundResource(R.drawable.bg_button_start)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}