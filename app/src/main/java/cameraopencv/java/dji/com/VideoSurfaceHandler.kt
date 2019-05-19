package cameraopencv.java.dji.com
import android.app.Activity
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.TextureView
import android.widget.ImageView
import dji.sdk.codec.DJICodecManager

import cameraopencv.java.dji.com.FPVDemoApplication.detectionActive
import dji.sdk.camera.VideoFeeder

class VideoSurfaceHandler(val activity: Activity) : TextureView.SurfaceTextureListener {

    protected var mCodecManager: DJICodecManager? = null
    protected var mReceivedVideoDataListener: VideoFeeder.VideoDataListener? = null
    protected lateinit var mVideoSurface: TextureView
    protected lateinit var mImageSurface: ImageView
    private var isVideoRecording: Boolean = false



    fun init() {
        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = VideoFeeder.VideoDataListener { videoBuffer, size ->
            if (mCodecManager != null) {
                mCodecManager!!.sendDataToDecoder(videoBuffer, size)
            }
        }

        // init mVideoSurface
        mVideoSurface = activity.findViewById(R.id.flight_video_previewer_surface)
        mImageSurface = activity.findViewById(R.id.flight_image_previewer_surface)

        mVideoSurface.surfaceTextureListener = this


        val camera = FPVDemoApplication.getCameraInstance()
        if (camera != null) {
            camera.setSystemStateCallback { cameraSystemState ->
                isVideoRecording = cameraSystemState.isRecording
            }
            //calibrateCamera(camera)
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
        if (mCodecManager != null) {
            mCodecManager!!.cleanSurface()
            mCodecManager = null
        }

        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        if (detectionActive) {
            //trackHeatSignatures()
        }
    }

    companion object {

        private val TAG = FlightActivity::class.java.name
    }

}