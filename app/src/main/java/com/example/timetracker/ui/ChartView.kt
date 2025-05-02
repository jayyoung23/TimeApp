package com.example.timetracker.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.timetracker.R
import com.example.timetracker.data.TimeRecord
import com.example.timetracker.databinding.ViewChartBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 自定义图表视图
 * 用于显示项目的时间记录统计图表
 */
class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // 视图绑定
    private val binding: ViewChartBinding
    
    // 图表类型常量
    companion object {
        const val TYPE_DAY = 0
        const val TYPE_WEEK = 1
        const val TYPE_MONTH = 2
        const val TYPE_QUARTER = 3
        const val TYPE_YEAR = 4
    }
    
    // 当前选择的图表类型
    private var currentType = TYPE_DAY
    
    // 时间记录数据
    private var timeRecords: List<TimeRecord> = emptyList()
    
    init {
        // 设置方向为垂直
        orientation = VERTICAL
        
        // 加载布局
        binding = ViewChartBinding.inflate(LayoutInflater.from(context), this)
        
        // 初始化图表
        setupChart()
        
        // 设置芯片组选择监听器
        setupChipGroupListeners()
    }
    
    /**
     * 初始化图表设置
     */
    private fun setupChart() {
        with(binding.barChart) {
            // 禁用描述文本
            description.isEnabled = false
            
            // 设置X轴位置和样式
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            
            // 设置左侧Y轴
            axisLeft.setDrawGridLines(true)
            axisLeft.axisMinimum = 0f
            
            // 禁用右侧Y轴
            axisRight.isEnabled = false
            
            // 设置图例
            legend.isEnabled = true
            
            // 启用缩放和拖动
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // 设置动画
            animateY(1000)
        }
    }
    
    /**
     * 设置芯片组选择监听器
     */
    private fun setupChipGroupListeners() {
        binding.chipGroupChartTypes.setOnCheckedChangeListener { _, checkedId ->
            currentType = when (checkedId) {
                R.id.chip_day -> TYPE_DAY
                R.id.chip_week -> TYPE_WEEK
                R.id.chip_month -> TYPE_MONTH
                R.id.chip_quarter -> TYPE_QUARTER
                R.id.chip_year -> TYPE_YEAR
                else -> TYPE_DAY
            }
            updateChart()
        }
    }
    
    /**
     * 设置时间记录数据
     * @param records 时间记录列表
     */
    fun setTimeRecords(records: List<TimeRecord>) {
        timeRecords = records
        updateChart()
    }
    
    /**
     * 更新图表数据
     */
    private fun updateChart() {
        if (timeRecords.isEmpty()) {
            binding.barChart.clear()
            binding.barChart.invalidate()
            return
        }
        
        // 根据当前选择的类型生成图表数据
        val entries = when (currentType) {
            TYPE_DAY -> generateDailyEntries()
            TYPE_WEEK -> generateWeeklyEntries()
            TYPE_MONTH -> generateMonthlyEntries()
            TYPE_QUARTER -> generateQuarterlyEntries()
            TYPE_YEAR -> generateYearlyEntries()
            else -> generateDailyEntries()
        }
        
        // 创建数据集
        val dataSet = BarDataSet(entries, context.getString(R.string.hours))
        dataSet.color = resources.getColor(R.color.colorPrimary, null)
        
        // 创建图表数据
        val barData = BarData(dataSet)
        barData.barWidth = 0.7f
        
        // 设置数据
        binding.barChart.data = barData
        
        // 刷新图表
        binding.barChart.invalidate()
    }
    
    /**
     * 生成每日数据条目
     */
    private fun generateDailyEntries(): List<BarEntry> {
        // 按日期分组并计算每天的总时长
        val dailyMap = mutableMapOf<String, Float>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 获取最近7天的日期
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, -6) // 往前推6天，总共7天
        val startDate = calendar.time
        
        // 初始化日期映射（确保所有日期都有条目，即使没有数据）
        calendar.time = startDate
        while (!calendar.time.after(endDate)) {
            val dateStr = dateFormat.format(calendar.time)
            dailyMap[dateStr] = 0f
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // 填充实际数据
        for (record in timeRecords) {
            val date = Date(record.startTime.time)
            val dateStr = dateFormat.format(date)
            if (dailyMap.containsKey(dateStr)) {
                // 将秒转换为小时
                dailyMap[dateStr] = dailyMap[dateStr]!! + (record.durationSeconds / 3600f)
            }
        }
        
        // 创建条目列表
        val entries = mutableListOf<BarEntry>()
        val sortedDates = dailyMap.keys.sorted()
        
        // 设置X轴标签
        val xLabels = sortedDates.map { it.substring(5) } // 只显示MM-dd部分
        binding.barChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < xLabels.size) xLabels[index] else ""
            }
        }
        
        // 添加条目
        sortedDates.forEachIndexed { index, date ->
            entries.add(BarEntry(index.toFloat(), dailyMap[date] ?: 0f))
        }
        
        return entries
    }
    
    /**
     * 生成每周数据条目
     */
    private fun generateWeeklyEntries(): List<BarEntry> {
        // 简化实现，仅返回一些示例数据
        // 实际应用中应该按周分组计算数据
        return listOf(
            BarEntry(0f, 5f),
            BarEntry(1f, 8f),
            BarEntry(2f, 6f),
            BarEntry(3f, 10f)
        )
    }
    
    /**
     * 生成每月数据条目
     */
    private fun generateMonthlyEntries(): List<BarEntry> {
        // 简化实现，仅返回一些示例数据
        // 实际应用中应该按月分组计算数据
        return listOf(
            BarEntry(0f, 20f),
            BarEntry(1f, 25f),
            BarEntry(2f, 30f),
            BarEntry(3f, 15f),
            BarEntry(4f, 22f),
            BarEntry(5f, 28f)
        )
    }
    
    /**
     * 生成季度数据条目
     */
    private fun generateQuarterlyEntries(): List<BarEntry> {
        // 简化实现，仅返回一些示例数据
        // 实际应用中应该按季度分组计算数据
        return listOf(
            BarEntry(0f, 80f),
            BarEntry(1f, 95f),
            BarEntry(2f, 65f),
            BarEntry(3f, 75f)
        )
    }
    
    /**
     * 生成年度数据条目
     */
    private fun generateYearlyEntries(): List<BarEntry> {
        // 简化实现，仅返回一些示例数据
        // 实际应用中应该按年分组计算数据
        return listOf(
            BarEntry(0f, 300f),
            BarEntry(1f, 350f),
            BarEntry(2f, 280f)
        )
    }
}