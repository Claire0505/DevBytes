/*
 * Copyright (C) 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.devbytes

import android.app.Application
import android.os.Build
import androidx.work.*
import com.example.devbytes.work.RefreshDataWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Override application to setup background work via WorkManager
 */
class DevByteApplication : Application() {

    private val applicationScope = CoroutineScope ( Dispatchers.Default)

    /**
     * onCreate is called before the first screen is shown to the user.
     *
     * Use it to setup any background tasks, running expensive setup operations in a background
     * thread to avoid delaying app start.
     */
    override fun onCreate() {
        super.onCreate()
        delayedInit()
    }

    private fun delayedInit(){
        applicationScope.launch {
            Timber.plant(Timber.DebugTree())
            setupRecurringWork()
        }
    }

    /**
     * 創建一個調用方法 [setupRecurringWork()]來設置重複的後台工作。
     * Setup WorkManager background job to 'fetch' new network data daily.
     *  設置 WorkManager 後台作業以每天“獲取”新的網絡數據。
     *  Recurring (再次發生的)
     *  使用該方法創建並初始化一個每天運行一次的定期工作請求 PeriodicWorkRequestBuilder()。
     *  傳入RefreshDataWorker您在上一個任務中創建的類。1以 的時間單位傳入重複間隔TimeUnit.DAYS。
     */
    private fun setupRecurringWork(){
        /**
         * 創建一個Constraints對象並在該對像上設置一個約束，即網絡類型約束。
         * 使用setRequiredNetworkType()方法向對象添加網絡類型約束constraints。
         * 使用UNMETERED枚舉以便工作請求僅在設備位於 UNMETERED(未計量 )的網絡上時運行。
         */
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)  // 更新工作請求，使其僅在設備充電時運行。
            .setRequiresBatteryNotLow(true)  // 只有在電池電量不低時才運行工作請求。
            .apply {
                // 此功能僅在 Android 6.0 (Marshmallow) 及更高版本中可用，因此請為 SDK 版本M及更高版本添加條件。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    setRequiresDeviceIdle(true)
                }
            }.build()

        val repeatingRequest1 =
            PeriodicWorkRequestBuilder<RefreshDataWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        // 打開設備或模擬器中的 Wi-Fi 並查看Logcat窗格。
        // 現在，只要滿足網絡限制，計劃的後台任務大約每 15 分鐘運行一次。
        val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        Timber.d("WorkManager: Periodic Work request for sync is scheduled 計劃同步的定期工作請求")
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            RefreshDataWorker.WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        repeatingRequest)
    }
}
