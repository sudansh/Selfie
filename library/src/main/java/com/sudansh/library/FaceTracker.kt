package com.sudansh.library

import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.sudansh.library.camera.GraphicOverlay

/**
 * Tracker for tracking a new face and updating the face stats like landmarks
 */
internal class FaceTracker(private val mOverlay: GraphicOverlay, private val listener: IFaceFound) : Tracker<Face>(), IFaceFound {

    private var faceOverlay: FaceOverlay = FaceOverlay(mOverlay, listener)

    override fun onNewItem(id: Int, face: Face?) {
        faceOverlay.setId(id)
    }


    override fun onUpdate(detectionResults: Detector.Detections<Face>?, face: Face) {
        synchronized(this) {
            mOverlay.add(faceOverlay)
            faceOverlay.emotionString = getEmotion(face)
            faceOverlay.updateFaceStats(face)
        }
    }


    override fun onMissing(detectionResults: Detector.Detections<Face>?) {
        mOverlay.remove(faceOverlay)
    }

    override fun onDone() {
        mOverlay.remove(faceOverlay)
    }

    override fun onFaceFound(face: Face) {
        listener.onFaceFound(face)
    }

}