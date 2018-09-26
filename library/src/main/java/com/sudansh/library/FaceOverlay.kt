package com.sudansh.library

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.google.android.gms.vision.face.Face
import com.sudansh.library.camera.GraphicOverlay

/**
 * Graphics class for rendering valid face area to identify.
 */
internal class FaceOverlay(private val overlay: GraphicOverlay,
                           private val listener: IFaceFound) : GraphicOverlay.Graphic(overlay) {

    //Paint for emotions to be displayed.
    private val paintText = Paint().apply {
        textSize = 80f
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var emotion: String? = null
    private var faceId: Int = 0
    private var face: Face? = null
    private var landmarkPositions: List<PointF> = mutableListOf()

    var emotionString: String = ""

    /**
     * Update the landmarks positions
     */
    private fun updateLandmarks(list: List<PointF>) {
        landmarkPositions = list.map {
            PointF(translateX(it.x), translateY(it.y))
        }
        overlay.contains(landmarkPositions).also { isInsideOval ->
            if (isInsideOval) listener.onFaceFound(face!!)
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.save().also {
            face?.also {
                canvas.drawText(emotionString, (canvas.width / 5).toFloat(), (canvas.height / 5).toFloat(), paintText)
            }
            canvas.restoreToCount(it)
        }
        emotion?.let {
            canvas.drawText(it, 16f, 16f, paintText)
        }
    }

    /**
     * update the landmarks and invalidate to draw the
     */
    fun updateFaceStats(face: Face) {
        this.face = face
        updateLandmarks(face.landmarks.orEmpty().map { it.position })
        postInvalidate()
    }

    /**
     * set face id
     */
    fun setId(id: Int) {
        faceId = id
    }
}
