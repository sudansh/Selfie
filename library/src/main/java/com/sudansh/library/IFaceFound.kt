package com.sudansh.library

import com.google.android.gms.vision.face.Face

/**
 * Interface on face recognized in the given region
 */
interface IFaceFound {
    fun onFaceFound(face: Face)
}
