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
import cameraopencv.java.dji.com.geometrics.PointWeight;
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

    List<PointWeight> locs = new ArrayList<>();


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
        boolean tracking = true;
        if(tracking)
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

    private static boolean test4Error(Mat frame) {
        double testAgainst = frame.get(31+10, 679+10)[0];
        List<Mat> channels = new ArrayList<Mat>();
        Core.split(frame, channels);
        for(int row =31+3;row<52-3;row++) {
            for(int col =679+5;col<703-5;col++) {
                if(!(channels.get(2).get(row, col)[0]<channels.get(0).get(row, col)[0]*0.5)) {
                    //if(!(frame.get(row, col)[0]>testAgainst-2 && frame.get(row, col)[0]<testAgainst+2)) {
                    return false;
                }
            }
        }
        return true;
    }

    static int numHeatSignatures = 0;
    private void trackHeatSignatures(){

        if(isVideoRecording) {
            showToast("isRecording");

          //  recordGPSData();
        }
        Bitmap sourceBitmap = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(),720,480,false);
      //  showToast("" + sourceBitmap.getWidth()+ "\t" + sourceBitmap.getHeight());


        Mat frame = new Mat();
        Utils.bitmapToMat(sourceBitmap, frame);
        Mat copy = frame.clone();
        if(test4Error(frame)) {
            System.out.println("Error");
            return;
        }
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(frame,frame,new Size(5,5), 0);


        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new  Size(5,5));
        Imgproc.dilate(frame, frame, element);

        MatOfDouble mu = new MatOfDouble();
        MatOfDouble sig = new MatOfDouble();
        Core.meanStdDev(frame, mu, sig);


        double sig1 = mu.get(0, 0)[0]+sig.get(0, 0)[0];
        double sig2 = mu.get(0, 0)[0]+2.35*sig.get(0, 0)[0];
        double sig3 = mu.get(0, 0)[0]+2.88*sig.get(0, 0)[0];

        Mat frameForRect = frame.clone();
        Imgproc.Canny(frame, frame, sig2, sig3);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierachie = new Mat();
        Imgproc.findContours(frame, contours, hierachie, Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_SIMPLE);


        int tmpnum = 0;
        int new_object =0;
        Point p = new Point(-1000,-1000);
        for(MatOfPoint cnt : contours) {

            MatOfPoint2f cnt2f = new MatOfPoint2f( cnt.toArray() );

            if(Imgproc.contourArea(cnt) > 10 && Imgproc.arcLength(cnt2f,false)>20) {
                Rect rContour =Imgproc.boundingRect(cnt);
                double x1 = rContour.x;
                double y1 = rContour.y;
                double width1 = rContour.width;
                double height1 = rContour.height;
                if(Math.abs(p.x -  x1) > 20 && Math.abs(p.y - y1) > 20) {
                    tmpnum++;
                    p.x = (x1+(x1+width1))/2;
                    p.y = (y1+(y1+height1))/2;

                    int weight = 1;
                    calculatePosition(p.x,p.y,weight);
                }
                //System.out.println("x: " + x);
                //System.out.println("y: " + y);

            }

        }

        Imgproc.Canny(frameForRect, frameForRect, sig1, sig2);

        List<MatOfPoint> contours1 = new ArrayList<MatOfPoint>();

        Mat hierachy = new Mat();
        Imgproc.findContours(frameForRect, contours1, hierachy, Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_SIMPLE);


        Point p1 = new Point(-1000,-1000);
        for(int j = 0; j>=0 ;j=(int)hierachy.get(0, j)[0]) {
            if(j>=contours1.size()) {
                break;
            }
            Rect r = Imgproc.boundingRect(contours1.get(j));
            if(hierachy.get(0,j)[2]>0) { //Check if there is a child contour
                p1.x = (r.x+r.width)/2;
                p1.y = (r.y+r.height)/2;
                int weight = 2;
                calculatePosition(p1.x,p1.y,weight);
            }
        }

        // Number of Heatsignatures
        if(tmpnum != numHeatSignatures || tmpnum == 0) {

            if(tmpnum > numHeatSignatures) {
              //  vibratePhone();
            }
            else {
                new_object = 0;
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


    private Point calculatePosition(double px_x, double px_y, int weight) {
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
        double droneHeight = FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getState()
                .getAircraftLocation()
                .getAltitude();
        double droneHeading = FPVDemoApplication.getAircraftInstance()
                .getFlightController()
                .getCompass()
                .getHeading();




        double x_scalingfactor = image_size.x/720;
        double y_scalingfactor = image_size.y/480;


        px_x = px_x - 360;
        px_y = -px_y + 240;
        double dist_x = (px_x * x_scalingfactor);
        double dist_y = (px_y * y_scalingfactor);


        Point dist_rot = rotateVektors(dist_x,dist_y,droneHeading);
        dist_y = dist_rot.y;
        dist_x = dist_rot.x;
        double aeq_r = 6378137;
        double pol_r = 6356752;
        double r = Math.sqrt((Math.pow(Math.pow(aeq_r,2) * Math.cos(Math.toRadians(droneLatitude)),2)+Math.pow(Math.pow(pol_r,2) * Math.sin(Math.toRadians(droneLatitude)),2))/(((Math.pow(aeq_r*Math.cos(Math.toRadians(droneLatitude)),2)))+(Math.pow(pol_r*Math.sin(Math.toRadians(droneLatitude)),2))));

        double geo_new_latitude  = droneLatitude  + (dist_y / r) * (180.0 / Math.PI);
        double geo_new_longitude = droneLongitude + (dist_x / r) * (180.0 / Math.PI) / Math.cos((droneLatitude * Math.PI/180.0));
        //System.out.println(geo_new_latitude + "\t"+ geo_new_longitude);
        double geo_new_latitude6 = (double)Math.round(geo_new_latitude * 10000000d) / 10000000d;
        double geo_new_longitude6 = (double)Math.round(geo_new_longitude * 10000000d) / 10000000d;
        locs.add(new PointWeight(geo_new_latitude,geo_new_longitude,weight));


        return new Point(geo_new_latitude,geo_new_longitude);
    }

    private Point calculateFrameSize(){

        double droneAltitude = FPVDemoApplication.getAircraftInstance().getFlightController().getState().getAircraftLocation().getAltitude();

        double fov_angle_y = 27; // degree
        double fov_angle_x = 35; // degree

        double dx = 2* Math.tan(Math.toRadians(fov_angle_x/2)) * droneAltitude;
        double dy =  2* Math.tan(Math.toRadians(fov_angle_y/2)) * droneAltitude;
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
