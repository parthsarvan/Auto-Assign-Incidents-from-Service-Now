package com.inciteam.app.session

import android.content.Context
import java.io.File

class ProfileImageStorage(context: Context) {
    private val directory = File(context.filesDir, "profile-images").apply {
        mkdirs()
    }

    fun loadImage(userId: Long): ByteArray? {
        val file = fileFor(userId)
        return if (file.exists()) file.readBytes() else null
    }

    fun saveImage(userId: Long, data: ByteArray): ByteArray {
        fileFor(userId).writeBytes(data)
        return data
    }

    fun deleteImage(userId: Long) {
        fileFor(userId).delete()
    }

    private fun fileFor(userId: Long): File {
        return File(directory, "user-$userId")
    }
}
