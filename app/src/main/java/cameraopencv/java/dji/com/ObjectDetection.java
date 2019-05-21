package cameraopencv.java.dji.com;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;

public class ObjectDetection {

    Activity context;
    protected TextureView mVideoSurface = null;
    protected ImageView mImageSurface;
    public List<WeightedLatLng> locs = new ArrayList<>();
    private boolean isVideoRecording;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(context) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    // Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private Runnable objectDetectedCallback;

    public ObjectDetection(Activity context, TextureView VideoSurface, ImageView ImageSurface,
                           Runnable objectDetectedCallback) {
        this.context = context;
        this.mVideoSurface = VideoSurface;
        this.mImageSurface = ImageSurface;
        this.objectDetectedCallback = objectDetectedCallback;
        if (!OpenCVLoader.initDebug()) {
            // Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, mLoaderCallback);
        } else {
            //  Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        Camera camera = FPVDemoApplication.getCameraInstance();

        if (camera != null) {
            camera.setSystemStateCallback(new SystemState.Callback() {
                @Override
                public void onUpdate(SystemState cameraSystemState) {
                    if (null != cameraSystemState) {
                        isVideoRecording = cameraSystemState.isRecording();
                    }
                }});
            calibrateCamera(camera);

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
    public void trackHeatSignatures(){

        if(isVideoRecording) {
            ToastUtils.showToast("isRecording");
            //  recordGPSData();
        }
        Bitmap sourceBitmap = Bitmap.createScaledBitmap(mVideoSurface.getBitmap(),720,480,false);

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

        ToastUtils.showToast("mu: " +  mu.get(0, 0)[0]);


        double sig1 = mu.get(0, 0)[0]+sig.get(0, 0)[0];
        double sig2 = mu.get(0, 0)[0]+2.35*sig.get(0, 0)[0];
        double sig3 = mu.get(0, 0)[0]+2.88*sig.get(0, 0)[0];

        Mat frameForRect = frame.clone();
        Imgproc.Canny(frame, frame, sig2, sig3);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierachie = new Mat();
        Imgproc.findContours(frame, contours, hierachie, Imgproc.RETR_CCOMP,Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(copy, contours, -1, new Scalar(0, 255, 255), 2);

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
                    Imgproc.circle(copy, p, 30, new Scalar(0, 0, 255), 2);
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
                //Imgproc.rectangle(copy, r, new Scalar(0, 0, 255), 2);
                Imgproc.rectangle(copy, r.tl(), r.br(), new Scalar(0, 0, 255), 2);
                p1.x = (r.x+r.width)/2;
                p1.y = (r.y+r.height)/2;
                int weight = 10;
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


    private Point calculatePosition(double px_x, double px_y, double weight) {
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
        LatLng loc = new LatLng(geo_new_latitude,geo_new_longitude);
        locs.add(new WeightedLatLng(loc,weight));

        objectDetectedCallback.run();

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
        camera.setThermalScene(SettingsDefinitions.ThermalScene.PROFILE_1,null);
        camera.setThermalPalette(SettingsDefinitions.ThermalPalette.WHITE_HOT,null);
        camera.setThermalIsothermEnabled(false,null);
        camera.setThermalGainMode(SettingsDefinitions.ThermalGainMode.AUTO,null);
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

    /*private void vibratePhone() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(150);
        }
    }*/
}
