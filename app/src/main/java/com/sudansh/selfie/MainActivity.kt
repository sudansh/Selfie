package com.sudansh.selfie

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.sudansh.library.TakeSelfie
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        takeSelfie.setOnClickListener { takeSelfie() }
    }

    private fun takeSelfie() {
        TakeSelfie(this).start(REQUEST_SELFIE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELFIE && resultCode == Activity.RESULT_OK) {
            data?.getByteArrayExtra("data")?.let {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                imageSelfie.setImageBitmap(bitmap)
            }
        } else {
            //Clear the image
            imageSelfie.setImageBitmap(null)
        }
    }

    companion object {
        const val REQUEST_SELFIE = 101
    }
}