package com.documentpro.office.business.fileviewer.utils

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class BusinessScreenShotObserver(
    private val context: Context,
    private val onScreenShot: () -> Unit
) : DefaultLifecycleObserver {
    private var contentObserver: ContentObserver? = null
    private var isRegistered = false

    fun start() {
        if (isRegistered) return
        registerContentObserverBelow14()
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        unregisterContentObserverBelow14()
        isRegistered = false
    }

    private fun registerContentObserverBelow14() {
        val handler = Handler(Looper.getMainLooper())
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                // 简单判断：只要有图片新增就回调
                onScreenShot.invoke()
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver!!
        )
    }

    private fun unregisterContentObserverBelow14() {
        contentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            contentObserver = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
    }
} 