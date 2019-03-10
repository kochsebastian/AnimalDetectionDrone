package cameraopencv.java.dji.com;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.*;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.useraccount.UserAccountManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = MainActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    protected ImageView mImageSurface;
    private TextView recordingTime;



    private Handler handler;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        initUI();

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {

            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {

                        int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
                        int minutes = (recordTime % 3600) / 60;
                        int seconds = recordTime % 60;

                        final String timeString = String.format("%02d:%02d", minutes, seconds);
                        final boolean isVideoRecording = cameraSystemState.isRecording();

                        MainActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {

                                recordingTime.setText(timeString);

                                /*
                                 * Update recordingTime TextView visibility and mRecordBtn's check state
                                 */
                                if (isVideoRecording){
                                    recordingTime.setVisibility(View.VISIBLE);
                                }else
                                {
                                    recordingTime.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }
            });
            calibrateCamera(camera);

        }

    }

    protected void onProductChange() {
        initPreviewer();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        showToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
        initPreviewer();
        onProductChange();
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        if(mVideoSurface == null) {
            Log.e(TAG, "mVideoSurface is null");
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        uninitPreviewer();
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view){
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        uninitPreviewer();
        super.onDestroy();
    }

    private void initUI() {
        // init mVideoSurface
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        mImageSurface = (ImageView)findViewById(R.id.image_previewer_surface);
     //   mImageSurface.bringToFront();
        recordingTime = (TextView) findViewById(R.id.timer);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }


        recordingTime.setVisibility(View.INVISIBLE);

    }

    private void initPreviewer() {

        BaseProduct product = FPVDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            showToast(getString(R.string.disconnected));
        } else {
            if (null != mVideoSurface) {
                mVideoSurface.setSurfaceTextureListener(this);
            }
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
            }
        }
    }

    private void uninitPreviewer() {
        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null){
            // Reset the callback
            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureAvailable");
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.e(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.e(TAG,"onSurfaceTextureDestroyed");
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        trackHeatSignatures();
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            default:
                break;
        }
    }


    private void trackHeatSignatures(){
        /*
        ToDo identify same object
         */
        Mat droneImage = new Mat();
        Utils.bitmapToMat(mVideoSurface.getBitmap(), droneImage);
        Mat copy = null;
        copy = droneImage.clone();
        //Mat histimage = createHist(frame2);
        Imgproc.cvtColor(droneImage, droneImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(droneImage, droneImage,new Size(5,5), 3);

        Imgproc.threshold(droneImage,droneImage, 170, 255,Imgproc.THRESH_BINARY);
        //frame2 = houghcircles(frame,frame2);
        //	Imgproc.adaptiveThreshold(frame, frame, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C,Imgproc.THRESH_BINARY_INV,15,20);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


        Imgproc.findContours(droneImage, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(copy, contours, -1, new Scalar(0,255,255),2);
        for(MatOfPoint cnt : contours) {
            if(Imgproc.contourArea(cnt) > 10) {
                double x = Imgproc.boundingRect(cnt).x;
                double y = Imgproc.boundingRect(cnt).y;
                Point p = new Point(x,y);
                Imgproc.circle(copy, p, 40, new Scalar( 255, 0, 0 ),2);
            }
        }
        displayAlteredImage(copy);

    }

    private void displayAlteredImage(Mat img){
        Bitmap bmpImageSurface =  Bitmap.createBitmap(img.cols(),
                img.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img,bmpImageSurface);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(bmpImageSurface);

    }

    private Point calculatePosition(int px_x, int px_y){
        Point image_size = calculateFrameSize();
        double droneLongitude = FPVDemoApplication.getAircraftInstance()
                                                    .getFlightController()
                                                    .getState()
                                                    .getAircraftLocation()
                                                    .getLongitude();
        double droneLatitude = FPVDemoApplication.getAircraftInstance()
                                                    .getFlightController()
                                                    .getState()
                                                    .getAircraftLocation()
                                                    .getLatitude();

        double x_scalingfactor = image_size.x/720;
        double y_scalingfactor = image_size.y/480;

        double dist_y = px_y * y_scalingfactor;
        double dist_x = px_x * x_scalingfactor;

        double geo_new_latitude  = droneLatitude  + (dist_y / 6371) * (180 / Math.PI);
        double geo_new_longitude = droneLongitude + (dist_x / 6371) * (180 / Math.PI) / Math.cos((droneLatitude * Math.PI/180));

        return new Point(geo_new_latitude,geo_new_longitude);

    }

    private Point calculateFrameSize(){
        // calculating by using trigonometry
        // 2*tan(theta) = d/h
        double fov_angle_y = 27; // degree
        double fov_angle_x = 35; // degree

        double droneAltitude = FPVDemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getAltitude();
        double dx = 2* Math.tan(fov_angle_x/2) * droneAltitude;
        double dy = 2* Math.tan(fov_angle_y/2) * droneAltitude;
        return new Point(dx,dy);

    }


    private void calibrateCamera(Camera camera){
        if(camera.isThermalCamera()){
            camera.setThermalPalette(SettingsDefinitions.ThermalPalette.WHITE_HOT,null);
            camera.setThermalIsothermEnabled(false,null);
           // camera.setThermalGainMode(SettingsDefinitions.ThermalGainMode.AUTO,null);
            camera.setThermalGainMode(SettingsDefinitions.ThermalGainMode.HIGH,null);
            camera.setThermalDDE(-20,null);
            camera.setThermalACE(1,null);
            camera.setThermalSSO(100,null);
            camera.setThermalContrast(32,null);
            camera.setThermalBrightness(8192,null);
            camera.setThermalFFCMode(SettingsDefinitions.ThermalFFCMode.MANUAL,null);

            camera.setThermalTemperatureUnit(SettingsDefinitions.TemperatureUnit.CELSIUS,null);
            camera.setThermalBackgroundTemperature(15,null);
            camera.setThermalAtmosphericTemperature(15,null);




        }
        calibrateGimbal();



    }

    private void calibrateGimbal(){
    //    if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            FPVDemoApplication.getProductInstance().getGimbal().
                    rotate(new Rotation.Builder().pitch(-90)
                            .mode(RotationMode.ABSOLUTE_ANGLE)
                            .yaw(Rotation.NO_ROTATION)
                            .roll(Rotation.NO_ROTATION)
                            .time(0)
                            .build(), new CommonCallbacks.CompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {

                        }
                    });
      //  }
    }
}
