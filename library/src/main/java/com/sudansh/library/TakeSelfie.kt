package com.sudansh.library

import android.content.Intent
import android.support.v7.app.AppCompatActivity

class TakeSelfie(private val activity: AppCompatActivity) {

    fun start(requestCode: Int) {
        activity.startActivityForResult(Intent(activity, SelfieActivity::class.java), requestCode)
    }

}
