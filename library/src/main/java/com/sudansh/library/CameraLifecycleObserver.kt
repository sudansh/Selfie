package com.sudansh.library

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import com.google.android.gms.vision.CameraSource

class CameraLifecycleObserver(private val cameraView: CameraSource) : LifecycleObserver {

    @SuppressLint("MissingPermission")
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun startCamera() {
        cameraView.start()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pauseCamera() {
        cameraView.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destroyCamera() {
        cameraView.release()
    }
}
