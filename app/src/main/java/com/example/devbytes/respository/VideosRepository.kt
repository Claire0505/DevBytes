package com.example.devbytes.respository

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.example.devbytes.database.VideosDatabase
import com.example.devbytes.database.asDomainModel
import com.example.devbytes.domain.DevByteVideo
import com.example.devbytes.network.DevByteNetwork
import com.example.devbytes.network.asDatabaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Repository for fetching devbyte videos from the network and storing them on disk
 */
class VideosRepository (private val database: VideosDatabase){

    /**
     * 創建一個對LiveData從數據庫中讀取視頻播放列表。
     * LiveData當數據庫更新時，這個對象會自動更新。附加的片段或活動將刷新為新值。
     */
    // 第 2 步：從數據庫中檢索數據
    val videos: LiveData<List<DevByteVideo>> =
        Transformations.map(database.videoDao.getVideos()){
        it.asDomainModel()
    }

    /**
     * Refresh the videos stored in the offline cache. 刷新離線緩存中存儲的視頻。
     *
     * 該函數使用IO調度器來保證數據庫插入數據庫操作,發生在 IO 調度器上。
     * This function uses the IO dispatcher to ensure the database insert database operation
     * happens on the IO dispatcher.
     * By switching to the IO dispatcher using `withContext` this
     * function is now safe to call from any thread including the Main thread.
     *
     */
    // 第 1 步：添加存儲庫
    suspend fun refreshVideos() {
        withContext(Dispatchers.IO){
            Timber.d("refresh videos is called")

            // 使用 Retrofit 服務實例從網絡中獲取 DevByte 視頻播放列表DevByteNetwork。
            val playlist = DevByteNetwork.devbytes.getPlaylist()

            // 從網絡獲取播放列表後，將播放列表存儲在Room數據庫中。
            // 要存儲播放列表，請使用VideosDatabase對象database。
            // 調用insertAllDAO 方法，傳入playlist從網絡中檢索到的。
            // 使用asDatabaseModel()擴展函數將 映射playlist到數據庫對象。
            database.videoDao.insertAll(playlist.asDatabaseModel())
        }
    }

}