# DevBytes
DevBytes 入門應用程序使用Retrofit庫從網絡中獲取視頻 URL 列表， 並使用RecyclerView. 該應用程序使用ViewModel並LiveData保存數據並更新 UI。
要實現離線緩存，您可以使用 Room數據庫將獲取的數據持久保存在設備的本地存儲中。

***

## 添加離線緩存  Add an offline cache
[https://developer.android.com/codelabs/kotlin-android-training-repository#4]

### 第 1 步：添加 Room 依賴項
打開build.gradle (Module:app)文件並將Room依賴項添加到項目中。
#### Room Database dependency
`implementation "androidx.room:room-runtime:$room_version`

#### Kotlin Extensions and Coroutines support for Room
`implementation "androidx.room:room-ktx:$room_version"`

---
### 第 2 步：添加數據庫對象
1.創建 database/DatabaseEntities.kt並創建一個Room名為的實體DatabaseVideo。設置url為主鍵。DevBytes 服務器設計確保視頻 URL 始終是唯一的。

2.在 database/DatabaseEntities.kt，創建一個名為 的擴展函數asDomainModel()。使用該函數將DatabaseVideo數據庫對象轉換為域對象。

3.打開 network/DataTransferObjects.kt並創建一個名為asDatabaseModel(). 使用該函數將網絡對象轉換為DatabaseVideo數據庫對象。

---
### 第 3 步：添加 VideoDao
1.在 中database/Room.kt，定義一個VideoDao接口並用 進行註釋@Dao。

`@Dao`
`interface VideoDao { }`

2.在VideoDao interface內，創建一個調用方法getVideos()以從數據庫中獲取所有視頻。將此方法的返回類型更改為LiveData，這樣每當數據庫中的數據發生變化時，UI 中顯示的數據就會刷新。

```@Query("select * from databasevideo")
   fun getVideos(): LiveData<List<DatabaseVideo>>
```
3.在VideoDao interface內，定義另一種insertAll()方法以將從網絡獲取的視頻列表插入到數據庫中。
為簡單起見，如果視頻條目已存在於數據庫中，則覆蓋數據庫條目。為此，請使用onConflict參數將衝突策略設置為REPLACE。

`表示新增物件時和舊物件發生衝突後的處置 (REPLACE 蓋掉 (最常用))`
```
@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insertAll( videos: List<DatabaseVideo>)
```
---
### 第 4 步：實施 RoomDatabase
1.在 中database/Room.kt，在VideoDao接口之後，創建一個abstract名為的類VideosDatabase。擴展VideosDatabase的RoomDatabase。

2.使用@Database註釋將VideosDatabase類標記為Room數據庫。聲明DatabaseVideo屬於該數據庫的實體，並將版本號設置為1。

3.在裡面VideosDatabase，定義一個類型的變量VideoDao來訪問Dao方法。
```
@Database(entities = [DatabaseVideo::class], version = 1)
abstract class VideosDatabase: RoomDatabase() {
   abstract val videoDao: VideoDao
}
```
4.創建一個在類外部private lateinit調用的變量INSTANCE，以保存單例對象。該VideosDatabase應 singleton，防止發生在同一時間打開數據庫的多個實例。

`private lateinit var INSTANCE: VideosDatabase`

5.getDatabase()在類之外創建和定義一個方法。在 中getDatabase()，初始化並返回塊INSTANCE內的變量synchronized。
```
fun getDatabase(context: Context): VideosDatabase {
   synchronized(VideosDatabase::class.java) {
       if (!::INSTANCE.isInitialized) {
           INSTANCE = Room.databaseBuilder(context.applicationContext,
                   VideosDatabase::class.java,
                   "videos").build()
       }
   }
   return INSTANCE
}
```
---

創建存儲庫
[https://developer.android.com/codelabs/kotlin-android-training-repository#6]

在此任務中，您將創建一個存儲庫來管理您在上一個任務中實現的離線緩存。您的Room數據庫沒有管理離線緩存的邏輯，它只有插入和檢索數據的方法。存儲庫將具有獲取網絡結果並保持數據庫最新的邏輯。

第 1 步：添加存儲庫
創建 repository/VideosRepository.kt，創建一個VideosRepository類。傳入一個對VideosDatabase像作為類的構造函數參數來訪問Dao方法。
 /**
* Repository for fetching devbyte videos from the network and storing them on disk
*/
class VideosRepository(private val database: VideosDatabase) {
}
在VideosRepository類中，添加一個refreshVideos()沒有參數且不返回任何內容的方法。此方法將是用於刷新離線緩存的 API。
做refreshVideos()一個暫停功能。因為refreshVideos()執行數據庫操作，它必須從協程調用。
注意：Android 上的數據庫存儲在文件系統或磁盤上，為了保存它們必須執行磁盤 I/O。磁盤 I/O 或磁盤讀寫速度很慢，並且總是阻塞當前線程，直到操作完成。因此，您必須在I/O 調度程序中運行磁盤 I/O 。此調度程序旨在將阻塞 I/O 任務卸載到使用 .withContext(Dispatchers.IO) { ... }

在refreshVideos()方法內部，切換協程上下文Dispatchers.IO以執行網絡和數據庫操作。

在withContext塊內，使用 Retrofit 服務實例從網絡中獲取 DevByte 視頻播放列表DevByteNetwork。

在refreshVideos()方法內部，從網絡獲取播放列表後，將播放列表存儲在Room數據庫中。

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
    
第 2 步：從數據庫中檢索數據
在此步驟中，您將創建一個對LiveData像以從數據庫中讀取視頻播放列表。LiveData當數據庫更新時，這個對象會自動更新。附加的片段或活動將刷新為新值。

在VideosRepository類中，聲明一個LiveData對象，調用它videos來保存DevByteVideo對象列表。

使用初始化videos對象。調用DAO 方法。由於該方法返回的是數據庫對象列表，而不是對象列表，因此 Android Studio 會引發“類型不匹配”錯誤。database.videoDaogetVideos()getVideos()DevByteVideo

val videos: LiveData<List<DevByteVideo>> = database.videoDao.getVideos()
要修復錯誤，請使用Transformations.map將數據庫對象列表轉換為域對象列表。使用asDomainModel()轉換功能。
Refresher：該 Transformations.map方法使用轉換函數將一個LiveData對象轉換為另一個LiveData對象。轉換僅在活動活動或片段觀察返回的LiveData屬性時計算。

val videos: LiveData<List<DevByteVideo>> = Transformations.map(database.videoDao.getVideos()) {
  it.asDomainModel()
}
   
---
## 使用刷新策略集成存儲庫
[https://developer.android.com/codelabs/kotlin-android-training-repository#7]

在此任務中，您將ViewModel使用簡單的刷新策略將存儲庫與 集成。您從Room數據庫中顯示視頻播放列表，而不是直接從網絡中獲取。

1. 在viewmodels/DevByteViewModel.kt, 內部DevByteViewModel類中，創建一個private名為videosRepository的成員變量VideosRepository。通過傳入單例VideosDatabase對象來實例化變量。
```
/**
* The data source this ViewModel will fetch results from.
*/
private val videosRepository = VideosRepository(getDatabase(application))
```

2. 在DevByteViewModel類中，將refreshDataFromNetwork()方法替換為refreshDataFromRepository()方法。

舊方法refreshDataFromNetwork()使用 Retrofit 庫從網絡中獲取視頻播放列表。新方法從存儲庫加載視頻播放列表。
```
   /**
     * Refresh data from the repository.
       Use a coroutine launch to run in a
     * background thread.
     */
    private fun refreshDataFromRepository() {
        viewModelScope.launch {
            try {
                videosRepository.refreshVideos()
                _eventNetworkError.value = false
                _isNetworkErrorShown.value = false

            }catch (netWorkError: IOException){
                // Show a Toast error message and hide teh progress bar
                if (playlist.value.isNullOrEmpty())
                    _eventNetworkError.value = true
            }
        }
    }
```
3. 在DevByteViewModel類中的init塊內，將函數調用從 更改refreshDataFromNetwork()為refreshDataFromRepository()。此代碼從存儲庫中獲取視頻播放列表，而不是直接從網絡中獲取。
```
init {
   refreshDataFromRepository()
}

```
4. 在DevByteViewModel類中，刪除_playlist屬性及其支持屬性playlist。
```
private val _playlist = MutableLiveData<List<Video>>()
...
val playlist: LiveData<List<Video>>
   get() = _playlist

```
5. 在DevByteViewModel類中，實例化對videosRepository像後，添加一個新val調用，playlist用於保存LiveData存儲庫中的視頻列表。

```
/**
* A playlist of videos displayed on the screen.
*/
val playlist = videosRepository.videos
```
6. 運行您的應用程序。該應用程序像以前一樣運行，但現在從網絡獲取 DevBytes 播放列表並保存在Room數據庫中。播放列表從Room數據庫顯示在屏幕上，而不是直接來自網絡。

>如果運行時發生錯誤
Android room persistent: AppDatabase_Impl does not exist
則開啟 build.gradle (app) 加上：
```
    def room_version = "2.3.0"

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor 
    "androidx.room:room-compiler:$room_version"

    // To use Kotlin annotation processing tool (kapt)
    kapt("androidx.room:room-compiler:$room_version")

 ```   
 ---
 # 如何使用WorkManager調度後台任務
[https://developer.android.com/codelabs/kotlin-android-training-work-manager#0]

## 概念：WorkManager

WorkManager是Android 架構組件之一，也是Android Jetpack 的一部分。WorkManager用於可延遲且需要保證執行的後台工作：

* 可延遲意味著工作不需要立即運行。例如，向服務器發送分析數據或在後台同步數據庫是可以推遲的工作。

* 保證執行意味著即使應用程序退出或設備重新啟動，任務也會運行。

WorkManager是一個 API，它可以輕鬆安排可靠的異步任務，即使應用程序退出或設備重新啟動，這些任務也有望運行。

>筆記：
WorkManager 不適用於在應用進程被終止時可以安全終止的進程內後台工作。
WorkManager 不適用於需要立即執行的任務。
---


 # [任務：創建一個後台工作者](https://developer.android.com/codelabs/kotlin-android-training-work-manager#5)

在此 Codelab 中，您安排一項任務，每天一次從網絡中預取 DevBytes 視頻播放列表。要安排此任務，請使用WorkManager庫。
```
    // WorkManager dependency
    def work_version = "2.5.0"
    // Kotlin + coroutines
    implementation("androidx.work:work-runtime-ktx:$work_version")
```

* [Worker](https://developer.android.com/reference/androidx/work/Worker.html)
此類是您定義要在後台運行的實際工作（任務）的地方。您擴展此類並覆蓋該doWork()方法。該doWork()方法是放置在後台中執行的代碼，諸如與服務器同步數據或處理圖像。您Worker在此任務中實施。

* [WorkRequest](https://developer.android.com/reference/androidx/work/WorkRequest.html)
此類表示在後台運行工作程序的請求。使用WorkRequest配置如何以及何時運行輔助任務的幫助下Constraints為設備插上電源或Wi-Fi連接等。您將WorkRequest在稍後的任務中實現。

---
## 第 1 步： Create a worker
1. 創建一個名為work package，創建 Kotlin 類 RefreshDataWorker。
2. RefreshDataWorker從CoroutineWorker類擴展類。將context和WorkerParameters作為構造函數參數傳入。
```
class RefreshDataWorker(appContext: Context, params: WorkerParameters) :
       CoroutineWorker(appContext, params) {
}
```
3. 要解決抽像類錯誤，請覆蓋類doWork()內的方法RefreshDataWorker。
```
override suspend fun doWork(): Result {
  return Result.success()
}
```
>一個暫停的功能是可以暫停和恢復後的功能。掛起函數可以執行長時間運行的操作並等待它完成而不阻塞主線程。

## 第 2 步：實現 doWork()
The doWork() method inside the Worker class is called on a background thread. The method performs work synchronously, and should return a ListenableWorker.Result object.

Android 系統Worker最多給 10 分鐘的時間來完成它的執行並返回一個ListenableWorker.Result對象。超過此時間後，系統會強制停止Worker. 

---
要創建ListenableWorker.Result對象，請調用以下靜態方法之一來指示後台工作的完成狀態：
* Result.success()——工作順利完成。
* Result.failure()——工作以永久失敗告終。
* Result.retry()— 工作遇到暫時性故障，應重試。

在此任務中，您將實現doWork()從網絡獲取 DevBytes 視頻播放列表的方法。您可以重用類中的現有方法VideosRepository來從網絡中檢索數據。

1. 在RefreshDataWorker類裡面doWork()，創建並實例化一個VideosDatabase對象和一個VideosRepository對象。
2. 在RefreshDataWorker類中， inside doWork()，在return語句上方，調用塊refreshVideos()內的方法try。添加日誌以跟踪工作程序何時運行。

```
class RefreshDataWorker(appContext: Context, params: WorkerParameters) :
       CoroutineWorker(appContext, params) {

   override suspend fun doWork(): Result {
       val database = getDatabase(applicationContext)
       val repository = VideosRepository(database)
       try {
           repository.refreshVideos()
       } catch (e: HttpException) {
           return Result.retry()
       }
       return Result.success()
   }
}
```
---
# [任務：定義一個週期性的 WorkRequest](https://developer.android.com/codelabs/kotlin-android-training-work-manager#6)

Worker定義了一個工作單元，並且[WorkRequest](https://developer.android.com/reference/androidx/work/WorkRequest)
定義了工作應該如何以及何時運行。WorkRequest該類有兩個具體實現：

* 該[OneTimeWorkRequest](https://developer.android.com/reference/androidx/work/OneTimeWorkRequest.html)
課程適用於一次性任務。（一次性任務只發生一次。）

* 該[PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest.html)
課程適用於定期工作，即每隔一段時間重複的工作。

>注意：週期性工作的最小間隔為 15 分鐘。週期性工作不能將初始延遲作為其約束之一。

## 第 1 步：設置重複工作

在 Android 應用程序中，Application該類是包含所有其他組件（例如活動和服務）的基類。創建應用程序或包的進程時，Application類（或任何子類Application）在任何其他類之前實例化。

1. 在DevByteApplication該類中，創建一個調用方法setupRecurringWork()來設置重複的後台工作。
```
/**
* Setup WorkManager background job to 'fetch'
   new network data daily.
*/
private fun setupRecurringWork() {
}
```
2. 在該setupRecurringWork()方法中，使用該方法創建並初始化一個每天運行一次的定期工作請求PeriodicWorkRequestBuilder()。傳入[RefreshDataWorker](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest.Builder)
您在上一個任務中創建的類。以 1的時間單位傳入重複間隔TimeUnit.DAYS。

```
val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(1, TimeUnit.DAYS)
       .build()
```
## 步驟 2：使用 WorkManager 安排 WorkRequest

在定義了WorkRequest 之後，您可以WorkManager使用[enqueueUniquePeriodicWork()](https://developer.android.com/reference/androidx/work/WorkManager.html#enqueueUniquePeriodicWork(java.lang.String,%20androidx.work.ExistingPeriodicWorkPolicy,%20androidx.work.PeriodicWorkRequest))
方法來安排它。此方法允許您將唯一名稱添加[PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest.html)
到隊列中，其中一次只能有PeriodicWorkRequest一個特定名稱處於活動狀態。
1. 在RefreshDataWorker類中，在類的開頭，添加一個伴生對象。定義一個工作名稱以唯一標識此 Worker。
```
 companion object {
        const val WORK_NAME = "com.example.devbytes.work.RefreshDataWorker"
    }
```
2. 在DevByteApplication.class，在setupRecurringWork()方法結束時，使用方法安排工作enqueueUniquePeriodicWork()。傳入KEEPExistingPeriodicWorkPolicy(現有的定期工作政策)的枚舉。
傳入 repeatingRequest的PeriodicWorkRequest參數。
```
WorkManager.getInstance().enqueueUniquePeriodicWork(
       RefreshDataWorker.WORK_NAME,
       ExistingPeriodicWorkPolicy.KEEP,
       repeatingRequest)
```
>最佳實踐：該onCreate()方法在主線程中運行。執行長時間運行的操作onCreate()可能會阻塞 UI 線程並導致加載應用程序延遲。為避免此問題WorkManager，請在協程內運行諸如初始化 Timber 和從主線程調度之類的任務。

3. 在DevByteApplication類的開頭，創建一個CoroutineScope對象。通過在Dispatchers.Default作為構造函數的參數。
```
private val applicationScope = CoroutineScope(Dispatchers.Default)
```
4. 在DevByteApplication該類中，添加一個名為delayedInit()啟動協程的新方法。
5. 在delayedInit()方法內部，調用setupRecurringWork().
6. 將 Timber 初始化從onCreate()方法移動到delayedInit()方法。
```
private fun delayedInit() {
   applicationScope.launch {
       Timber.plant(Timber.DebugTree())
       setupRecurringWork()
   }
}
```
7. 在DevByteApplication類中，在onCreate()方法的末尾，添加對方法的調用delayedInit()。
```
override fun onCreate() {
   super.onCreate()
   delayedInit()
}
```
8. 打開Android Studio 窗口底部的Logcat窗格。過濾RefreshDataWorker。
9. 運行應用程序。該WorkManager時間表的經常性的工作立即。
```
在Logcat窗格中，請注意顯示工作請求已調度，然後成功運行的日誌語句。
D/RefreshDataWorker: Work request for sync is run
I/WM-WorkerWrapper: Worker result SUCCESS for Work [...]
```

## 第 3 步：（可選）將 WorkRequest 安排為最小間隔
在此步驟中，您將時間間隔從 1 天減少到 15 分鐘。這樣做是為了查看正在運行的定期工作請求的日誌。
1. 在DevByteApplication類中的setupRecurringWork()方法中，註釋掉當前repeatingRequest定義。以15分鐘為周期的重複間隔添加新的工作請求。
```
// val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(1, TimeUnit.DAYS)
//        .build()

val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(15, TimeUnit.MINUTES)
       .build()
 ```      
 2. 運行應用程序，並WorkManager立即安排您的重複工作。在Logcat窗格中，注意日誌——工作請求每 15 分鐘運行一次。等待 15 分鐘以查看另一組工作請求日誌。
 ---

# [任務：添加約束 (Constraints)](https://developer.android.com/codelabs/kotlin-android-training-work-manager#7)

定義WorkRequest時，您可以指定Worker應該運行的約束。例如，您可能希望指定工作應僅在設備空閒時運行，或僅在設備插入並連接到 Wi-Fi 時運行。您還可以為重試工作指定退避策略。在支持的限制是在set方法Constraints.Builder。

>PeriodicWorkRequest(定期工作請求) 和 Constraints (約束)
 PeriodicWorkRequest，用於重複工作會執行多次，直到被取消。第一次執行立即發生，或者在滿足給定的約束後立即執行。
下一次執行發生在下一個週期間隔內。請注意，執行可能會延遲，因為WorkManager受操作系統電池優化的影響，例如當設備處於打盹模式時。

## 第 1 步：添加一個 Constraints 對象並設置一個約束
在此步驟中，您將創建一個Constraints對象並在該對像上設置一個約束，即網絡類型約束。

1. 在DevByteApplication類中，在開頭setupRecurringWork()，定義val類型為的Constraints。使用Constraints.Builder()方法。
```
val constraints = Constraints.Builder()
```
2. 使用setRequiredNetworkType()方法向對象添加網絡類型約束constraints。使用UNMETERED枚舉以便工作請求僅在設備位於未計量的網絡上時運行。
```
.setRequiredNetworkType(NetworkType.UNMETERED)
```
3. 使用build()方法從構建器生成約束。
```
val constraints = Constraints.Builder()
       .setRequiredNetworkType(NetworkType.UNMETERED)
       .build()
```
現在您需要將新創建的Constraints對象設置為工作請求。

4. 在DevByteApplication類中的setupRecurringWork()方法中，將Constraints對象設置為定期工作請求repeatingRequest。要設置約束，請setConstraints()在build()方法調用上方添加方法。
```
val repeatingRequest = PeriodicWorkRequestBuilder<RefreshDataWorker>(15, TimeUnit.MINUTES)
               .setConstraints(constraints)
               .build()
 ```
## 第 2 步：運行應用程序並註意日誌
 在此步驟中，您運行應用程序並註意到受約束的工作請求每隔一段時間在後台運行。
1. 從設備或模擬器卸載應用程序以取消任何先前計劃的任務。

2. 在 Android Studio 中打開Logcat窗格。在Logcat窗格中，通過單擊左側的清除 logcat圖標清除以前的日誌。過濾work。

3. 關閉設備或模擬器中的 Wi-Fi，這樣您就可以看到約束是如何工作的。當前代碼只設置了一個約束，表明請求應該只在未計量的網絡上運行。由於 Wi-Fi 關閉，設備未連接到網絡，無論按流量計費還是不按流量計費。因此，將不會滿足此約束。

4. 運行應用程序並註意Logcat窗格。該WorkManager調度後台任務立即。因為不滿足網絡約束，任務沒有運行。 
5. 打開設備或模擬器中的 Wi-Fi 並查看Logcat窗格。現在，只要滿足網絡限制，計劃的後台任務大約每 15 分鐘運行一次。
```
11:31:44 D/DevByteApplication: Periodic Work request for sync is scheduled
11:31:47 D/RefreshDataWorker: Work request for sync is run
11:31:47 I/WM-WorkerWrapper: Worker result SUCCESS for Work [...]
11:46:45 D/RefreshDataWorker: Work request for sync is run
11:46:45 I/WM-WorkerWrapper: Worker result SUCCESS for Work [...] 
```
 ## 第 3 步：添加更多約束
 在此步驟中，您將以下約束添加到PeriodicWorkRequest：
* 電池電量不低。
* 設備充電。
* 設備空閒；僅在 API 級別 23 (Android M) 及更高版本中可用。

1. 在DevByteApplication類中的setupRecurringWork()方法內部，指示只有在電池電量不低時才運行工作請求。在build()方法調用之前添加約束，並使用該setRequiresBatteryNotLow()方法。

2. 更新工作請求，使其僅在設備充電時運行。在build()方法調用之前添加約束，並使用該setRequiresCharging()方法。

3. 更新工作請求，使其僅在設備空閒時運行。在build()方法調用之前添加約束，並使用setRequiresDeviceIdle()方法。此約束僅在用戶未主動使用設備時運行工作請求。此功能僅在 Android 6.0 (Marshmallow) 及更高版本中可用，因此請為 SDK 版本M及更高版本添加條件。

```
val constraints = Constraints.Builder()
       .setRequiredNetworkType(NetworkType.UNMETERED)
       .setRequiresBatteryNotLow(true)
       .setRequiresCharging(true)
       .apply {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
               setRequiresDeviceIdle(true)
           }
       }
       .build()
 ```
 4. 在setupRecurringWork()方法內部，將請求間隔改回每天一次。   
 ---
## 總結
* 該WorkManagerAPI可以很容易地安排必須可靠地運行延遲的，異步任務。

* 大多數現實世界的應用程序需要執行長時間運行的後台任務。要以優化和有效的方式安排後台任務，請使用WorkManager.

* 在主要的類WorkManager庫中Worker，WorkRequest和WorkManager。
* 本Worker類代表一個工作單元。要實現後台任務，請擴展Worker該類並覆蓋該[doWork()](https://developer.android.com/reference/androidx/work/Worker.html#doWork())方法。

* WorkRequest類表示以執行工作單元的請求。WorkRequest是用於安排的工作指定參數的基類WorkManager。

* WorkRequest該類有兩種具體實現：OneTimeWorkRequest用於一次性任務和PeriodicWorkRequest用於定期工作請求。

* 定義WorkRequest時，您可以指定Constraints指示Worker應何時運行。約束包括設備是否已插入、設備是否空閒或是否已連接 Wi-Fi。

* 要添加約束的WorkRequest，使用中列出的設置方法的Constraints.Builder文檔。例如，要指示WorkRequest設備電池電量低時不應運行，請使用setRequiresBatteryNotLow()set 方法。

* 定義之後WorkRequest，將任務交給 Android 系統。為此，請使用其中一種WorkManager enqueue方法安排任務。
* 執行的確切時間Worker取決於 中使用的約束WorkRequest，以及系統優化。WorkManager鑑於這些限制，旨在提供最佳行為。 
   




