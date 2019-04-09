package cameraopencv.java.dji.com;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.*;
import android.util.Log;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
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
import dji.sdk.useraccount.UserAccountManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ManualFlightActivity extends Activity implements SurfaceTextureListener,OnClickListener{

    private static final String TAG = ManualFlightActivity.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

    protected TextureView mVideoSurface = null;
    protected ImageView mImageSurface;
    private TextView recordingTime;
    private boolean isVideoRecording;

    List<Point> locs = new ArrayList<Point>();


    private Handler handler;


    File file ;

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
        File root = Environment.getExternalStorageDirectory();

        Calendar c = Calendar.getInstance();


        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String formattedDate = df.format(c.getTime());



       // file = new File(root, "gpsData" +formattedDate +".csv");

        setContentView(R.layout.activity_manual_flight);
    //    TimelineFlight tfv = new TimelineFlight(this);


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
                        isVideoRecording = cameraSystemState.isRecording();

                        ManualFlightActivity.this.runOnUiThread(new Runnable() {

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
                Toast.makeText(ManualFlightActivity.this, msg, Toast.LENGTH_SHORT).show();
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

    static int numHeatSignatures = 0;
    private void trackHeatSignatures(){




        if(isVideoRecording) {
            showToast("isRecording");

          //  recordGPSData();
        }
        Bitmap sourceBitmap = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(),720,480,false);
      //  showToast("" + sourceBitmap.getWidth()+ "\t" + sourceBitmap.getHeight());
    /*
    NOTES
    Probability
    Object found with both filters => prob > 90%
    Object found with global threshold => prob < 50
    Object found with Canny => prob > 50%
     */
        /*
        TODO identify same object
         */
        Mat droneImage = new Mat();
        Utils.bitmapToMat(sourceBitmap, droneImage);
        Mat copy = droneImage.clone();

        Imgproc.cvtColor(droneImage, droneImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(droneImage, droneImage,new Size(5,5), 0);


        MatOfDouble mu = new MatOfDouble();
        MatOfDouble sig = new MatOfDouble();
        Core.meanStdDev(droneImage, mu, sig);


        double sig1 = mu.get(0, 0)[0]+sig.get(0, 0)[0];
        double sig2 = mu.get(0, 0)[0]+2*sig.get(0, 0)[0];
        double sig3 = mu.get(0, 0)[0]+3*sig.get(0, 0)[0];



        Imgproc.Canny(droneImage, droneImage, sig2, sig3,3);


        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();


        Imgproc.findContours(droneImage, contours, new Mat(), Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(copy, contours, -1, new Scalar(0,255,255),2);

        int tmpnum = 0;

        Point p = new Point(-100,-100); // -100: not a real coordinate
        for(MatOfPoint cnt : contours) {
            MatOfPoint2f cnt2f = new MatOfPoint2f( cnt.toArray() );
            if(Imgproc.contourArea(cnt) > 10 || Imgproc.arcLength(cnt2f,false)>20) {
                // 5 and 10 too small, maybe with more gaussian blur
                // TODO maybe closed contours have a higher probability to be real
                double x = Imgproc.boundingRect(cnt).x;
                double y = Imgproc.boundingRect(cnt).y;
                if(Math.abs(p.x -  x) > 5 && Math.abs(p.y - y) > 5) {
                    //  avoid tracking two heat signatures from the same object
                    tmpnum++;
                    p.x = x;
                    p.y = y;
                    Imgproc.circle(copy, p, 30, new Scalar( 0, 0, 255 ),2);
                    locs.add(calculatePosition(p.x,p.y));
                    //6th decimal point is 1/9m => regard every location that is the same till the 6th decimal point as identical
                }
            }
        }
        // Number of Heatsignatures
        if(tmpnum != numHeatSignatures || tmpnum == 0){
            if(tmpnum > numHeatSignatures){
              //  vibratePhone();
            }
            numHeatSignatures = tmpnum;
        }


        displayAlteredImage(copy);

    }

    private void displayAlteredImage(Mat img){

        Bitmap bmpImageSurface =  Bitmap.createBitmap(img.cols(),
                img.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img,bmpImageSurface);
        Bitmap displayBitmap = Bitmap.createScaledBitmap(bmpImageSurface,mVideoSurface.getBitmap().getWidth(),mVideoSurface.getBitmap().getHeight(),false);
        mImageSurface.setImageBitmap(null);
        mImageSurface.setImageBitmap(displayBitmap);

    }


    private Point calculatePosition(double px_x, double px_y) {
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
        double droneHeading = FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getCompass()
                .getHeading();


        double x_scalingfactor = image_size.x/720;
        double y_scalingfactor = image_size.y/480;

        px_x = px_x - 720/2;
        px_y = -px_y + 480/2;
        double dist_x = (px_x * x_scalingfactor);
        double dist_y = (px_y * y_scalingfactor);


        Point dist_rot = rotateVektors(dist_x,dist_y,droneHeading);
        dist_y = dist_rot.y;
        dist_x = dist_rot.x;

        double geo_new_latitude  = droneLatitude  + (dist_y / 6378137) * (180 / Math.PI);
        double geo_new_longitude = droneLongitude + (dist_x / 6378137) * (180 / Math.PI) / Math.cos((droneLatitude * Math.PI/180));

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

    private Point rotateVektors(double x, double y, double droneHeading) {
        Point xy = new Point(x,y);
        droneHeading = Math.toRadians(droneHeading);
        double x_rot = x * Math.cos(droneHeading) + y * Math.sin(droneHeading);
        double y_rot = (-x)*Math.sin(droneHeading) + y * Math.cos(droneHeading);
        xy.x = x_rot;
        xy.y = y_rot;
        return xy;

    }


    private void calibrateCamera(Camera camera){
        if(camera.isThermalCamera()){
            camera.setThermalPalette(SettingsDefinitions.ThermalPalette.WHITE_HOT,null);
            camera.setThermalIsothermEnabled(false,null);
           camera.setThermalGainMode(SettingsDefinitions.ThermalGainMode.HIGH,null);
          //  camera.setThermalGainMode(SettingsDefinitions.ThermalGainMode.HIGH,null);
            camera.setThermalDDE(-20,null);
            camera.setThermalACE(0,null);
            camera.setThermalSSO(100,null);
            camera.setThermalContrast(32,null);
            camera.setThermalBrightness(8192,null);
            camera.setThermalFFCMode(SettingsDefinitions.ThermalFFCMode.AUTO,null);
            camera.setThermalROI(SettingsDefinitions.ThermalROI.FULL,null);

            camera.setThermalTemperatureUnit(SettingsDefinitions.TemperatureUnit.CELSIUS,null);
           // camera.setThermalBackgroundTemperature(15,null);
            //camera.setThermalAtmosphericTemperature(15,null);




        }
        calibrateGimbal();



    }

    private void calibrateGimbal(){

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

    }

    private void vibratePhone() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }
    }

    private void recordGPSData(){

        FileWriter writer;
        double latitude = FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getState()
                .getAircraftLocation()
                .getLatitude();
        double longitude = FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getState()
                .getAircraftLocation()
                .getLongitude();


        double height = FPVDemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getAltitude();

        double heading = (double) FPVDemoApplication.getAircraftInstance().getFlightController().getCompass().getHeading();//  getAircraftHeadDirection();//  getCompass().getHeading();

       //
        try {

                writer = new FileWriter(file,true);

               // writeCsvHeader("a","b",writer);
                writeCsvData(latitude,longitude,height, heading, writer);
                writer.flush();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }


    }

    private void writeCsvData(double lat, double longi, double height, double heading,FileWriter writer) throws IOException {
        String line = String.format("%f,%f,%f,%f \n", lat, longi,height, heading);
        writer.write(line);
    }

}
