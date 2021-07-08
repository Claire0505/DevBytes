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







