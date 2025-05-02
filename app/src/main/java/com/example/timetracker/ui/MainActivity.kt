package com.example.timetracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.timetracker.R
import com.example.timetracker.data.AppDatabase
import com.example.timetracker.databinding.ActivityMainBinding
import com.example.timetracker.repository.ProjectRepository
import com.example.timetracker.repository.TimeRecordRepository
import com.example.timetracker.viewmodel.ProjectViewModel

/**
 * 主活动
 * 应用程序的入口点，负责初始化ViewModel和导航组件
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: ProjectViewModel
    
    // TimerService相关变量
    private var timerService: TimerService? = null
    private var bound: Boolean = false
    
    // 服务连接
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // 获取服务实例
            val binder = service as TimerService.LocalBinder
            timerService = binder.getService()
            bound = true
            
            // 检查服务中是否有正在计时的项目
            checkActiveProject()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化视图绑定
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置底部导航栏
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        
        // 初始化数据库和仓库
        val database = AppDatabase.getDatabase(this)
        val projectRepository = ProjectRepository(database.projectDao())
        val timeRecordRepository = TimeRecordRepository(database.timeRecordDao())
        
        // 初始化ViewModel
        val factory = ProjectViewModel.Factory(projectRepository, timeRecordRepository)
        viewModel = ViewModelProvider(this, factory)[ProjectViewModel::class.java]
        
        // 启动并绑定TimerService
        val intent = Intent(this, TimerService::class.java)
        startService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * 检查服务中是否有正在计时的项目
     * 如果有，则更新ViewModel中的活跃项目状态
     */
    private fun checkActiveProject() {
        timerService?.let { service ->
            if (service.isTimerRunning()) {
                service.getCurrentProject()?.let { project ->
                    // 更新ViewModel中的活跃项目
                    viewModel.setActiveProject(project.id, service.getStartTime())
                    
                    // 添加计时监听器
                    service.addListener(object : TimerService.TimerListener {
                        override fun onTimerTick(elapsedSeconds: Int) {
                            // 更新UI（如果需要）
                        }
                    })
                }
            }
        }
    }
    
    /**
     * 获取计时服务实例
     * @return 计时服务实例，如果未绑定则返回null
     */
    fun getTimerService(): TimerService? {
        return if (bound) timerService else null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 解绑服务
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }
}