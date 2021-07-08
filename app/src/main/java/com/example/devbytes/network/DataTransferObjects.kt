package com.example.devbytes.network

import com.example.devbytes.domain.DevByteVideo
import com.squareup.moshi.JsonClass

/**
 * DataTransferObjects go in this file. These are responsible for parsing responses from the server
 * or formatting objects to send to the server. You should convert these to domain objects before
 * using them.
 *
 * 數據傳輸對像用於解析網絡結果。
 * 此文件還包含一個方便的方法 ，asDomainModel()用於將網絡結果轉換為域對象列表。
 * 數據傳輸對象與域對像不同，因為它們包含用於解析網絡結果的額外邏輯。
 *
 * @see domain package for
 */

/**
 * VideoHolder holds a list of Videos.
 *
 * This is to parse first level of our network result which looks like
 *
 * {
 *   "videos": []
 * }
 */
@JsonClass(generateAdapter = true)
data class NetworkVideoContainer(val videos: List<NetworkVideo>)

/**
 * Videos represent a devbyte that can be played.
 */
@JsonClass(generateAdapter = true)
data class NetworkVideo(
    val title: String,
    val description: String,
    val url: String,
    val updated: String,
    val thumbnail: String,
    val closedCaptions: String?)

/**
 * Convert Network results to database objects
 */
fun NetworkVideoContainer.asDomainModel(): List<DevByteVideo> {
    return videos.map {
        DevByteVideo(
            title = it.title,
            description = it.description,
            url = it.url,
            updated = it.updated,
            thumbnail = it.thumbnail)
    }
}
