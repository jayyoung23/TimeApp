package com.example.timetracker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.timetracker.R
import com.example.timetracker.databinding.FragmentAddProjectBinding
import com.example.timetracker.viewmodel.ProjectViewModel

/**
 * 添加项目Fragment
 * 用于创建新的项目，设置项目名称和每日目标时间
 */
class AddProjectFragment : Fragment() {

    private var _binding: FragmentAddProjectBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProjectViewModel
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProjectBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取ViewModel实例
        viewModel = (activity as MainActivity).viewModel
        
        // 设置时间选择器的最小值和最大值
        setupTimePicker()
        
        // 设置保存按钮点击事件
        binding.buttonSave.setOnClickListener {
            saveProject()
        }
        
        // 设置取消按钮点击事件
        binding.buttonCancel.setOnClickListener {
            // 返回上一页
            findNavController().navigateUp()
        }
    }
    
    /**
     * 设置时间选择器
     */
    private fun setupTimePicker() {
        // 设置小时选择器
        binding.pickerHours.apply {
            minValue = 0
            maxValue = 23
            value = 1 // 默认1小时
        }
        
        // 设置分钟选择器
        binding.pickerMinutes.apply {
            minValue = 0
            maxValue = 59
            value = 0 // 默认0分钟
        }
    }
    
    /**
     * 保存项目
     */
    private fun saveProject() {
        // 获取项目名称
        val name = binding.editTextProjectName.text.toString().trim()
        
        // 验证项目名称不能为空
        if (name.isEmpty()) {
            binding.editTextProjectName.error = getString(R.string.error_empty_name)
            return
        }
        
        // 获取每日目标时间（小时和分钟）
        val hours = binding.pickerHours.value
        val minutes = binding.pickerMinutes.value
        
        // 验证时间不能为0
        if (hours == 0 && minutes == 0) {
            Toast.makeText(requireContext(), R.string.error_zero_time, Toast.LENGTH_SHORT).show()
            return
        }
        
        // 转换为秒
        val dailyGoalSeconds = hours * 3600 + minutes * 60
        
        // 获取提醒设置
        val reminderEnabled = binding.switchReminder.isChecked
        val reminderHour = binding.pickerReminderHour.value
        val reminderMinute = binding.pickerReminderMinute.value
        
        // 创建项目
        viewModel.createProject(
            name = name,
            dailyGoalSeconds = dailyGoalSeconds,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        ).observe(viewLifecycleOwner) { projectId ->
            // 创建成功，返回上一页
            Toast.makeText(requireContext(), R.string.project_created, Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}