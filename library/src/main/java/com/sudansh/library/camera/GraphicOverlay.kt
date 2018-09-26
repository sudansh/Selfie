package com.sudansh.library.camera

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import com.google.android.gms.vision.CameraSource
import com.sudansh.library.R
import java.util.*


/**
 * View to be render on top of the camera preview.
 */
class GraphicOverlay(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val mLock = Any()
    private var mPreviewWidth: Int = 0
    private var mWidthScaleFactor = 1.0f
    private var mPreviewHeight: Int = 0
    private var mHeightScaleFactor = 1.0f
    private var mFacing = CameraSource.CAMERA_FACING_BACK
    private val mGraphics = HashSet<Graphic>()
    private val paintOval = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
        style = Paint.Style.FILL_AND_STROKE
    }

    //Rect for the oval shape
    private var rectFaceRegion = RectF(0f, 0f, 0f, 0f)

    /**
     * Base class for a custom graphics object
     */
    abstract class Graphic(private val mOverlay: GraphicOverlay) {

        abstract fun draw(canvas: Canvas)

        /**
         * Adjusts a horizontal value of the supplied value from the preview scale to the view
         * scale.
         */
        fun scaleX(horizontal: Float): Float {
            return horizontal * mOverlay.mWidthScaleFactor
        }

        /**
         * Adjusts a vertical value of the supplied value from the preview scale to the view scale.
         */
        fun scaleY(vertical: Float): Float {
            return vertical * mOverlay.mHeightScaleFactor
        }

        /**
         * Adjusts the x coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateX(x: Float): Float {
            return if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                mOverlay.width - scaleX(x)
            } else {
                scaleX(x)
            }
        }

        /**
         * Adjusts the y coordinate from the preview's coordinate system to the view coordinate
         * system.
         */
        fun translateY(y: Float): Float {
            return scaleY(y)
        }

        fun postInvalidate() {
            mOverlay.postInvalidate()
        }

    }

    /**
     * Removes all graphics from the overlay.
     */
    fun clear() {
        synchronized(mLock) {
            mGraphics.clear()
        }
        postInvalidate()
    }

    /**
     * Adds a graphic to the overlay.
     */
    fun add(graphic: Graphic) {
        synchronized(mLock) {
            mGraphics.add(graphic)
        }
        postInvalidate()
    }

    /**
     * Removes a graphic from the overlay.
     */
    fun remove(graphic: Graphic) {
        synchronized(mLock) {
            mGraphics.remove(graphic)
        }
        postInvalidate()
    }

    /**
     * Sets the camera attributes for size and facing direction
     * Set the oval shape for the valid region to recognize a face
     */
    fun setCameraInfo(previewWidth: Int, previewHeight: Int, facing: Int) {
        synchronized(mLock) {
            mPreviewWidth = previewWidth
            mPreviewHeight = previewHeight
            mFacing = facing
            rectFaceRegion = RectF(width / 2 - 300f, height / 2 - 400f,
                    width / 2 + 300f, height / 2 + 400f)
        }
        postInvalidate()
    }

    /**
     * Draws the overlay with its associated graphic objects.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(ContextCompat.getColor(context, R.color.black_overlay))
        canvas.drawOval(rectFaceRegion, paintOval)
        synchronized(mLock) {
            if (mPreviewWidth != 0 && mPreviewHeight != 0) {
                mWidthScaleFactor = width.toFloat() / mPreviewWidth.toFloat()
                mHeightScaleFactor = height.toFloat() / mPreviewHeight.toFloat()
            }

            mGraphics.forEach { graphic -> graphic.draw(canvas) }
        }
    }

    /**
     * Return true if all the landmarks points lie inside the valid region to identify the face
     */
    fun contains(landmarks: List<PointF>) = landmarks.isNotEmpty() && landmarks.all {
        rectFaceRegion.contains(it.x, it.y)
    }
}