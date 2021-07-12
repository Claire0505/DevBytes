package com.example.devbytes.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.devbytes.database.getDatabase
import com.example.devbytes.respository.VideosRepository
import retrofit2.HttpException
import timber.log.Timber

/**
 * RefreshDataWorker從CoroutineWorker類擴展類。
 * 將context和WorkerParameters作為構造函數參數傳入。
 * Result.success()——工作順利完成。
 * Result.failure()——工作以永久失敗告終。
 * Result.retry()— 工作遇到暫時性故障，應重試。
 */
class RefreshDataWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    // 創建並實例化一個VideosDatabase對象和一個VideosRepository對象。
    override suspend fun doWork(): Result {
        val database = getDatabase(applicationContext)
        val repository = VideosRepository(database)

        try {
            Timber.d("Work request for sync is run  運行同步工作請求")
            repository.refreshVideos()

        } catch (e: HttpException){
            return  Result.retry()
        }

        return Result.success()
    }

}