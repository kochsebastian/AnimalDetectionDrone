package cameraopencv.java.dji.com

import android.app.Activity
import android.app.Application
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.widget.ImageView
import dji.sdk.codec.DJICodecManager

import cameraopencv.java.dji.com.objectdetection.ObjectDetection
import cameraopencv.java.dji.com.utils.ToastUtils
import dji.common.camera.SystemState
import dji.common.product.Model
import dji.sdk.camera.Camera
import dji.sdk.camera.VideoFeeder
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

class VideoSurfaceHandler(val activity: Activity) : TextureView.SurfaceTextureListener {

    protected lateinit var mCodecManager: DJICodecManager
    protected lateinit var mReceivedVideoDataListener: VideoFeeder.VideoDataListener
    protected lateinit var mVideoSurface: TextureView
    protected lateinit var mImageSurface: ImageView
    private var isVideoRecording: Boolean = false


    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                super.onManagerConnected(status)

            }
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                }
                else -> {
                }
            }
        }
    }

    private lateinit var objectDetection: ObjectDetection


    fun init() {
        val product = FPVDemoApplication.getProductInstance()

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            if (mCodecManager != null) {
                mCodecManager!!.sendDataToDecoder(videoBuffer, size)
            }
        }

        // init mVideoSurface
        mVideoSurface = activity.findViewById(R.id.flight_video_previewer_surface)
        mImageSurface = activity.findViewById(R.id.flight_image_previewer_surface)

        initPreviewer()

        val camera = FPVDemoApplication.getCameraInstance()
        if (camera != null) {
            camera.setSystemStateCallback { cameraSystemState ->
                isVideoRecording = cameraSystemState.isRecording
            }
            //calibrateCamera(camera)
        }

        objectDetection = ObjectDetection(activity, mVideoSurface, mImageSurface)

    }


    private fun initPreviewer() {
        val product = FPVDemoApplication.getProductInstance()
        if (product == null || !product.isConnected) {
        } else {

            mVideoSurface.surfaceTextureListener = this
            if (product.model != Model.UNKNOWN_AIRCRAFT) {
                VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(mReceivedVideoDataListener)
            }
        }
    }

    private fun uninitPreviewer() {
        val camera: Camera? = FPVDemoApplication.getCameraInstance()
        if (camera != null) {
            // Reset the callback
            VideoFeeder.getInstance().primaryVideoFeed.addVideoDataListener(null!!)
        }
    }


    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e(TAG, "onSurfaceTextureAvailable")
        if (mCodecManager == null) {
            mCodecManager = DJICodecManager(activity, surface, width, height)
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e(TAG, "onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.e(TAG, "onSurfaceTextureDestroyed")
        mCodecManager!!.cleanSurface()

        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (FPVDemoApplication.detectionActive) {
            ToastUtils.showToast("track heat signatures")
           objectDetection.trackHeatSignatures()
        }
    }


    fun pause() {

    }
    fun resume() {
        initPreviewer()

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, activity, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    fun destroy() {

    }


    companion object {

        private val TAG = FlightActivity::class.java.name
    }

}
