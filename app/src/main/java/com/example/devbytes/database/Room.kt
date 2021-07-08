package com.example.devbytes.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * 在VideoDao界面內，創建一個調用方法getVideos()以從數據庫中獲取所有視頻。
 * 將此方法的返回類型更改為LiveData，這樣每當數據庫中的數據發生變化時，UI 中顯示的數據就會刷新。
 */
@Dao
interface VideoDao {
    @Query ("select * from databasevideo")
    fun getVideos(): LiveData<List<DatabaseVideo>>

    // @Insert(onConflict = OnConflictStrategy.REPLACE)
    // 表示新增物件時和舊物件發生衝突後的處置 (REPLACE 蓋掉 (最常用))
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(videos: List<DatabaseVideo>)
}

/**
 * 使用@Database註釋將VideosDatabase類標記為Room數據庫。
 * 聲明DatabaseVideo屬於該數據庫的實體，並將版本號設置為1。
 */
@Database(entities = [DatabaseVideo::class], version = 1)
abstract class VideosDatabase: RoomDatabase(){
    abstract val videoDao: VideoDao
}

/**
 * 創建一個在類外部private lateinit調用的變量INSTANCE，以保存單例對象。
 * 該VideosDatabase應 singleton ，防止發生在同一時間打開數據庫的多個實例。
  */
private lateinit var INSTANCE: VideosDatabase

fun getDatabase(context: Context): VideosDatabase {
    synchronized(VideosDatabase::class.java){
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(context.applicationContext,
                VideosDatabase::class.java,
                "videos")
                .build()
        }
    }
    return INSTANCE
}