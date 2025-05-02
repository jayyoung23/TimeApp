package com.example.timetracker.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.timetracker.R
import com.example.timetracker.data.Project
import com.example.timetracker.ui.MainActivity
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * 计时服务
 * 用于在后台运行计时功能，并显示通知
 */
class TimerService : Service() {

    // 绑定器，用于与Activity/Fragment通信
    private val binder = LocalBinder()
    
    // 计时器
    private var timer: Timer? = null
    
    // 当前计时的项目
    private var currentProject: Project? = null
    
    // 开始时间
    private var startTime: Date? = null
    
    // 通知ID
    private val NOTIFICATION_ID = 1001
    
    // 通知渠道ID
    private val CHANNEL_ID = "timer_channel"
    
    // 监听器列表
    private val listeners = mutableListOf<TimerListener>()
    
    /**
     * 本地绑定器类
     */
    inner class LocalBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    /**
     * 计时监听器接口
     */
    interface TimerListener {
        fun onTimerTick(elapsedSeconds: Int)
    }
    
    override fun onCreate() {
        super.onCreate()
        // 创建通知渠道（Android 8.0及以上需要）
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 使用START_STICKY，如果服务被系统杀死，会尝试重新创建服务
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.timer_notification_channel_name)
            val descriptionText = getString(R.string.timer_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // 注册通知渠道
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 开始计时
     * @param project 要计时的项目
     */
    fun startTimer(project: Project) {
        currentProject = project
        startTime = Date()
        
        // 启动前台服务并显示通知
        startForeground(NOTIFICATION_ID, createNotification(0))
        
        // 启动计时器，每秒更新一次
        timer = fixedRateTimer("timer", false, 0L, 1000) {
            val elapsedSeconds = ((System.currentTimeMillis() - startTime!!.time) / 1000).toInt()
            // 更新通知
            updateNotification(elapsedSeconds)
            // 通知监听器
            notifyListeners(elapsedSeconds)
        }
    }
    
    /**
     * 停止计时
     * @return 计时的持续时间（秒）
     */
    fun stopTimer(): Int {
        // 取消计时器
        timer?.cancel()
        timer = null
        
        // 计算持续时间
        val durationSeconds = if (startTime != null) {
            ((System.currentTimeMillis() - startTime!!.time) / 1000).toInt()
        } else {
            0
        }
        
        // 停止前台服务
        stopForeground(true)
        
        // 重置状态
        currentProject = null
        startTime = null
        
        return durationSeconds
    }
    
    /**
     * 创建通知
     * @param elapsedSeconds 已经过的时间（秒）
     * @return 通知对象
     */
    private fun createNotification(elapsedSeconds: Int): Notification {
        // 创建打开应用的PendingIntent
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        // 格式化时间
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        // 创建通知
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_time))
            .setContentText("${currentProject?.name}: $timeString")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    /**
     * 更新通知
     * @param elapsedSeconds 已经过的时间（秒）
     */
    private fun updateNotification(elapsedSeconds: Int) {
        val notification = createNotification(elapsedSeconds)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 添加计时监听器
     * @param listener 监听器
     */
    fun addListener(listener: TimerListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除计时监听器
     * @param listener 监听器
     */
    fun removeListener(listener: TimerListener) {
        listeners.remove(listener)
    }
    
    /**
     * 通知所有监听器
     * @param elapsedSeconds 已经过的时间（秒）
     */
    private fun notifyListeners(elapsedSeconds: Int) {
        for (listener in listeners) {
            listener.onTimerTick(elapsedSeconds)
        }
    }
    
    /**
     * 检查是否正在计时
     * @return 是否正在计时
     */
    fun isTimerRunning(): Boolean {
        return timer != null
    }
    
    /**
     * 获取当前计时的项目
     * @return 当前计时的项目，如果没有则返回null
     */
    fun getCurrentProject(): Project? {
        return currentProject
    }
    
    /**
     * 获取开始时间
     * @return 开始时间，如果没有则返回null
     */
    fun getStartTime(): Date? {
        return startTime
    }
}