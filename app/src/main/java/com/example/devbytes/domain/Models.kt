package com.example.devbytes.domain

import com.example.devbytes.util.smartTruncate

/**
 * Domain objects are plain Kotlin data classes that represent the things in our app. These are the
 * objects that should be displayed on screen, or manipulated by the app.
 *
 * @see database for objects that are mapped to the database
 * @see network for objects that parse or prepare network calls
 */

/**
 * Videos represent a devbyte that can be played.
 */
data class DevByteVideo(val title: String,
                        val description: String,
                        val url: String,
                        val updated: String,
                        val thumbnail: String) {

    /**
     * Short description is used for displaying truncated descriptions in the UI
     * 簡短描述用於在 UI 中顯示截斷的描述
     */
    val shortDescription: String
        get() = description.smartTruncate(200)
}