package com.example.timetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * 应用数据库类
 * 使用Room框架管理SQLite数据库
 */
@Database(entities = [Project::class, TimeRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // 项目数据访问对象
    abstract fun projectDao(): ProjectDao
    
    // 时间记录数据访问对象
    abstract fun timeRecordDao(): TimeRecordDao
    
    companion object {
        // 单例实例
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * 获取数据库实例
         * 使用单例模式确保整个应用只有一个数据库连接
         * 
         * @param context 应用上下文
         * @return 数据库实例
         */
        fun getDatabase(context: Context): AppDatabase {
            // 如果实例已存在，直接返回
            return INSTANCE ?: synchronized(this) {
                // 创建数据库实例
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "timetracker_database"
                )
                    // 允许在主线程执行数据库操作（仅用于开发阶段，生产环境应移除）
                    .allowMainThreadQueries()
                    // 当数据库架构变更时，销毁并重建数据库（仅用于开发阶段）
                    .fallbackToDestructiveMigration()
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}