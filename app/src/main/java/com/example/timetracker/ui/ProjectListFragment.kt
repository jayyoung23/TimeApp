package com.example.timetracker.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timetracker.R
import com.example.timetracker.data.Project
import com.example.timetracker.databinding.FragmentProjectListBinding
import com.example.timetracker.viewmodel.ProjectViewModel
import java.util.*

/**
 * 项目列表Fragment
 * 显示所有项目列表，并提供添加项目和开始计时的功能
 */
class ProjectListFragment : Fragment() {

    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProjectViewModel
    private lateinit var adapter: ProjectAdapter
    
    // 计时器
    private var countDownTimer: CountDownTimer? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取ViewModel实例
        viewModel = (activity as MainActivity).viewModel
        
        // 设置RecyclerView
        setupRecyclerView()
        
        // 设置添加项目按钮点击事件
        binding.fabAddProject.setOnClickListener {
            findNavController().navigate(R.id.action_projectListFragment_to_addProjectFragment)
        }
        
        // 观察项目列表数据变化
        viewModel.allProjects.observe(viewLifecycleOwner, Observer { projects ->
            adapter.submitList(projects)
            // 显示或隐藏空视图
            binding.emptyView.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        })
        
        // 观察活跃项目变化
        viewModel.activeProject.observe(viewLifecycleOwner, Observer { activeProject ->
            // 更新适配器中的活跃项目
            adapter.setActiveProject(activeProject)
            
            val timerService = (activity as MainActivity).getTimerService()
            
            // 如果有活跃项目，启动计时器
            activeProject?.let { project ->
                project.lastStartTime?.let { startTime ->
                    // 启动UI计时器
                    startCountDownTimer(project.id, startTime, project.dailyGoalSeconds)
                    
                    // 如果TimerService没有运行，则启动它
                    timerService?.let { service ->
                        if (!service.isTimerRunning()) {
                            service.startTimer(project)
                            
                            // 添加计时监听器
                            service.addListener(object : TimerService.TimerListener {
                                override fun onTimerTick(elapsedSeconds: Int) {
                                    // 通知适配器更新UI
                                    activity?.runOnUiThread {
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            })
                        }
                    }
                }
            } ?: run {
                // 如果没有活跃项目，停止计时器
                stopCountDownTimer()
                
                // 停止TimerService
                timerService?.let { service ->
                    if (service.isTimerRunning()) {
                        service.stopTimer()
                    }
                }
            }
        })
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = ProjectAdapter(
            // 项目点击事件
            onProjectClick = { project ->
                // 导航到项目详情页面
                val action = ProjectListFragmentDirections
                    .actionProjectListFragmentToProjectDetailFragment(project.id)
                findNavController().navigate(action)
            },
            // 开始/停止计时按钮点击事件
            onTrackingButtonClick = { project ->
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
                                val durationSeconds = ((Date().time - startTime.time) / 1000).toInt()
                                viewModel.stopProjectAndRecord(project.id, startTime, durationSeconds)
                            }
                        }
                    } ?: run {
                        // 如果服务未绑定，直接更新项目状态
                        project.lastStartTime?.let { startTime ->
                            val durationSeconds = ((Date().time - startTime.time) / 1000).toInt()
                            viewModel.stopProjectAndRecord(project.id, startTime, durationSeconds)
                        }
                    }
                    stopCountDownTimer()
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
                                // 通知适配器更新UI
                                adapter.notifyDataSetChanged()
                            }
                        })
                    } ?: run {
                        // 如果服务未绑定，直接更新项目状态
                        viewModel.startProject(project.id)
                    }
                }
            },
            // 格式化时间的函数
            formatTimeFunction = { seconds ->
                viewModel.formatTime(seconds)
            },
            // 计算剩余时间的函数
            calculateRemainingTimeFunction = { dailyGoalSeconds, todayDurationSeconds ->
                viewModel.calculateRemainingTime(dailyGoalSeconds, todayDurationSeconds)
            }
        )
        
        binding.recyclerViewProjects.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProjectListFragment.adapter
        }
    }
    
    /**
     * 启动倒计时器
     * 
     * @param projectId 项目ID
     * @param startTime 开始时间
     * @param dailyGoalSeconds 每日目标时间（秒）
     */
    private fun startCountDownTimer(projectId: Long, startTime: Date, dailyGoalSeconds: Int) {
        // 先停止已有的计时器
        stopCountDownTimer()
        
        // 计算已经过去的时间（毫秒）
        val elapsedTimeMillis = Date().time - startTime.time
        
        // 计算剩余时间（毫秒）
        val remainingTimeMillis = (dailyGoalSeconds * 1000 - elapsedTimeMillis).coerceAtLeast(0)
        
        // 创建新的计时器，每秒更新一次
        countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 通知适配器更新UI
                adapter.notifyDataSetChanged()
            }
            
            override fun onFinish() {
                // 计时结束，停止项目
                val durationSeconds = dailyGoalSeconds
                viewModel.stopProjectAndRecord(projectId, startTime, durationSeconds)
            }
        }.start()
    }
    
    /**
     * 停止倒计时器
     */
    private fun stopCountDownTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        stopCountDownTimer()
        _binding = null
    }
}