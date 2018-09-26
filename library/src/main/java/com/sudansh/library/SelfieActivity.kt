package com.sudansh.library

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.gms.vision.face.Landmark.LEFT_EYE
import com.google.android.gms.vision.face.Landmark.RIGHT_EYE
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor
import kotlinx.android.synthetic.main.activity_selfie.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.IOException

class SelfieActivity : AppCompatActivity(), IFaceFound {

    private val faceDetector by lazy {
        FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build()
    }

    private lateinit var canvas: Canvas
    private lateinit var eyePatchBitmap: Bitmap
    private lateinit var defaultBitmap: Bitmap
    private var mCameraSource: CameraSource? = null
    private var mIsFrontFacing = true

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selfie)

        //button listeners
        flipButton.setOnClickListener { flipCamera() }
        cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        done.setOnClickListener {
            //Get the bitmap from the image and pass to intent as byteArray instead of bitmap to avoid large bundle size
            val bitmap = (face.drawable as? BitmapDrawable)?.bitmap
            setResult(Activity.RESULT_OK, Intent().apply { putExtra("data", bitmap.byteArray()) })
            finish()
        }

        if (savedInstanceState != null) {
            mIsFrontFacing = savedInstanceState.getBoolean("IsFrontFacing")
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) createCameraSource() else requestCameraPermission()
    }

    private fun processImage() {

        if (!faceDetector.isOperational) {
            android.support.v7.app.AlertDialog.Builder(this)
                    .setMessage("Face Detector could not be set up on your device :(")
                    .show()
        } else {
            val bitmapOptions = BitmapFactory.Options().apply {
                inMutable = true
            }

            val temporaryBitmap = Bitmap.createBitmap(defaultBitmap.width,
                    defaultBitmap.height,
                    Bitmap.Config.RGB_565)
            eyePatchBitmap = BitmapFactory.decodeResource(resources, R.drawable.patch, bitmapOptions)

            canvas = Canvas(temporaryBitmap).apply {
                drawBitmap(defaultBitmap, 0f, 0f, null)
            }
            val frame = Frame.Builder().setBitmap(defaultBitmap).build()
            val sparseArray = faceDetector.detect(frame)

            detectFaces(sparseArray)

            face.setImageDrawable(BitmapDrawable(resources, temporaryBitmap))

            faceDetector.release()
        }
    }

    private fun detectFaces(sparseArray: SparseArray<Face>) {
        (0 until sparseArray.size()).forEach { i ->
            val face = sparseArray.valueAt(i)
            var leftEye: PointF? = null
            var righEye: PointF? = null
            face.landmarks.forEach { landmark ->
                when (landmark.type) {
                    LEFT_EYE -> leftEye = landmark.position
                    RIGHT_EYE -> righEye = landmark.position
                }
            }
            if (leftEye != null && righEye != null) {
                val distance = Math.sqrt(
                        Math.pow((righEye!!.x - leftEye!!.x).toDouble(), 2.0) + Math.pow((righEye!!.y - leftEye!!.y).toDouble(), 2.0)).toFloat()
                val patchSize = .45 * distance
                canvas.drawBitmap(eyePatchBitmap, null, Rect((leftEye!!.x - patchSize).toInt(), (leftEye!!.y - patchSize).toInt(), (leftEye!!.x + patchSize).toInt(), (leftEye!!.y + patchSize).toInt()), null)
            }
        }
    }


    private fun setBitmap(faceBitmap: Bitmap) {
        this.defaultBitmap = faceBitmap
        preview.visibility = View.GONE
        face.setImageBitmap(faceBitmap)
        processImage()
    }


    private fun flipCamera() {
        mIsFrontFacing = !mIsFrontFacing

        if (mCameraSource != null) {
            mCameraSource?.release()
            mCameraSource = null
        }

        createCameraSource()
        startCameraSource()
    }

    /**
     * Handles the requesting of the camera permission.  This includes showing a "Snackbar" message
     * of why the permission is needed then sending the request.
     */
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(faceOverlay!!, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
    }

    /**
     * Restarts the camera.
     */
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        preview?.stop()
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        mCameraSource?.release()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            // we have permission, so create the camerasource
            createCameraSource()
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.permission_dialog_title))
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    /**
     * Saves the camera facing mode, so that it can be restored after the device is rotated.
     */
    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("IsFrontFacing", mIsFrontFacing)
    }

    /**
     * Creates the face detector and associated processing pipeline to support either front facing
     * mode or rear facing mode.  Checks if the detector is ready to use, and displays a low storage
     * warning if it was not possible to download the face library.
     */
    private fun createFaceDetector(context: Context): FaceDetector {
        val detector = FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(mIsFrontFacing)
                .setMinFaceSize(if (mIsFrontFacing) 0.35f else 0.15f)
                .build()

        val processor: Detector.Processor<Face>
        val tracker = FaceTracker(faceOverlay, this)
        processor = LargestFaceFocusingProcessor.Builder(detector, tracker).build()
        detector.setProcessor(processor)

        if (!detector.isOperational) {
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowStorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowStorageFilter) != null

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w(TAG, getString(R.string.low_storage_error))
            }
        }
        return detector
    }

    /**
     * Creates the face detector and the camera.
     */
    private fun createCameraSource() {
        val context = applicationContext
        val detector = createFaceDetector(context)
        var facing = CameraSource.CAMERA_FACING_FRONT
        if (!mIsFrontFacing) facing = CameraSource.CAMERA_FACING_BACK

        //Choose 640x480 resolution for the camera.
        mCameraSource = CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(640, 480)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build()
//        lifecycle.addObserver(CameraLifecycleObserver(mCameraSource!!))
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        // check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }

        mCameraSource?.let {
            try {
                preview?.start(it, faceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource?.release()
                mCameraSource = null
            }

        }
    }

    override fun onFaceFound(face: Face) {
        launch(UI) {
            if (!face.landmarks.isEmpty()) {
                mCameraSource?.takePicture(null, { bytes ->
                    getFace(this@SelfieActivity, bytes)?.let {
                        mCameraSource?.stop()
                        this@SelfieActivity.setBitmap(it)
                    }
                })
            }
        }

    }

    companion object {
        private const val TAG = "Selfie"

        private const val RC_HANDLE_GMS = 9001

        // permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2
    }
}
