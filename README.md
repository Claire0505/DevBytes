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





