package cameraopencv.java.dji.com;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

public class MenuActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {


    private Button mFields, mFlight, mStatistics;
    private RadioGroup trackingActive;
    private GoogleMap gMap;
    private Marker droneMarker = null;
    private double droneLocationLat = 181, droneLocationLng = 181;
    private View map;
    private FlightController mFlightController;
    private Button toggleCameraButton;
    private VideoSurfaceHandler videoSurfaceHandler;
    private ObjectDetection objectDetection;

    private HeatmapTileProvider heatmapTileProvider = null;
    private TileOverlay heatmapTileOverlay = null;

    // repeating task to analyse image
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (FPVDemoApplication.detectionActive) {
                objectDetection.trackHeatSignatures();
            } else {
                MenuActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (videoSurfaceHandler.mVideoSurface.isAvailable()) {
                            Bitmap displayBitmap = Bitmap.createScaledBitmap(videoSurfaceHandler.mVideoSurface.getBitmap(),
                                    videoSurfaceHandler.mVideoSurface.getBitmap().getWidth(),
                                    videoSurfaceHandler.mVideoSurface.getBitmap().getHeight(), false);
                            videoSurfaceHandler.mImageSurface.setImageBitmap(null);
                            videoSurfaceHandler.mImageSurface.setImageBitmap(displayBitmap);
                        }
                    }
                });
            }
            handler.postDelayed(runnable, 30);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_menu);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initFlightController();
        initUI(savedInstanceState);
        videoSurfaceHandler = new VideoSurfaceHandler(this);
        videoSurfaceHandler.init();


        Runnable objectDetectedCallback = new Runnable() {
            @Override
            public void run() {
                if (heatmapTileProvider == null){
                    // Create a heat map tile provider, passing it the latlngs of the police stations.
                    heatmapTileProvider = new HeatmapTileProvider.Builder()
                            .weightedData(objectDetection.locs)
                            .build();
                    // Add a tile overlay to the map, using the heat map tile provider.
                    heatmapTileOverlay = gMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));
                } else {
                    heatmapTileProvider.setWeightedData(objectDetection.locs);
                    heatmapTileOverlay.clearTileCache();
                }
            }
        };

        objectDetection = new ObjectDetection(this, videoSurfaceHandler.mVideoSurface,
                videoSurfaceHandler.mImageSurface, objectDetectedCallback);

        handler.postDelayed(runnable, 1000);
    }

    private void initUI(Bundle savedInstanceState) {
        mFields = findViewById(R.id.fields);
        mFlight = findViewById(R.id.flight);
        mStatistics = findViewById(R.id.statistics);

        mFields.setOnClickListener(this);
        mFlight.setOnClickListener(this);
        mStatistics.setOnClickListener(this);

        toggleCameraButton = findViewById(R.id.btn_toggle_camera);
        toggleCameraButton.setOnClickListener(this);

        map = findViewById(R.id.map);

        trackingActive = findViewById(R.id.toggle);
        //RadioButton off = findViewById(R.id.off);
       // off.toggle();
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.fields:
                Intent intent = new Intent(this, FieldsActivity.class);
                startActivity(intent);
                break;

            case R.id.flight:
                Intent intent2 = new Intent(this, SelectFieldActivity.class);
                startActivity(intent2);
                break;

            case R.id.statistics:
                Intent intent3 = new Intent(this, StatisticsActivity.class);
                startActivity(intent3);
                break;

            case R.id.btn_toggle_camera:
                switch (map.getVisibility()) {
                    case View.VISIBLE:
                        map.setVisibility(View.INVISIBLE);
                        break;
                    case View.INVISIBLE:
                        map.setVisibility(View.VISIBLE);
                        break;
                }
                break;

            default:
                break;
        }
    }
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.on:
                if (checked) {
                    ToastUtils.showToast("On");
                    FPVDemoApplication.detectionActive = true;
                }
                    break;
            case R.id.off:
                if (checked) {
                    ToastUtils.showToast("Off");
                    FPVDemoApplication.detectionActive = false;
                }
                    break;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap != null) {
            ToastUtils.showToast("map ready again?!");
            return;
        }
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //gMap.setOnMapClickListener(this);
        cameraUpdate(); // updates map position
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        if(checkGpsCoordinates(droneLocationLat,droneLocationLng)) {
            float zoomlevel = (float) 18.0;
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
            gMap.moveCamera(cu);
        }else
            gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(51.055705, 13.510207)));
    }

    public static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
    }

    private void initFlightController() {
        BaseProduct product = FPVDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    updateDroneLocation();
                }
            });
        }
    }

    private void updateDroneLocation(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_img));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                    droneMarker = null;
                }

                if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }
}