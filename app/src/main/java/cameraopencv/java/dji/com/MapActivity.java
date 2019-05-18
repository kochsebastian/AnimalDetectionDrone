package cameraopencv.java.dji.com;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.*;
import cameraopencv.java.dji.com.geometrics.Point2D;
import cameraopencv.java.dji.com.model.PolygonGrid;
import cameraopencv.java.dji.com.model.StatisticEntry;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.importSDKDemo.model.Field;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MapActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback  {

    private GoogleMap gMap;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private Marker droneMarker = null;
    private FlightController mFlightController;

    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Field field;


    private Button homeButton;
    private Button abortFlightButton;
    private Button toggleCameraButton;
    private Button backButton;
    private View map;


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
            }
            handler.postDelayed(runnable, 30);
        }
    };


    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        handler.removeCallbacks(runnable);
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initFlightController();

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

    private void initUI() {
        homeButton = findViewById(R.id.btn_return_home);
        homeButton.setOnClickListener(this);

        abortFlightButton = findViewById(R.id.btn_abort_flight);
        abortFlightButton.setOnClickListener(this);

        toggleCameraButton = findViewById(R.id.btn_toggle_camera);
        toggleCameraButton.setOnClickListener(this);

        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(this);
        backButton.setEnabled(false);

        map = findViewById(R.id.map);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_back:
                Intent intent = new Intent(this, MenuActivity.class);
                startActivity(intent);
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

            case R.id.btn_return_home:
                FPVDemoApplication.abortAndHome();
                break;

            case R.id.btn_abort_flight:
                FPVDemoApplication.stopTimeline();
                break;

            default:
                break;
        }
    }


    @Override
    public void onMapClick(LatLng latLng) {
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap != null) {
            ToastUtils.showToast("map ready again?!");
            return;
        }
        gMap = googleMap;
        gMap.setOnMapClickListener(this);
        cameraUpdate(); // updates map position

        field = ApplicationModel.INSTANCE.getFields().get(0);
        PolygonGrid pG = new PolygonGrid();
        List<Point2D> wayPoints2D = pG.makeGrid(40,field.getPolygon());
        List<LatLng> flightWaypoints = new ArrayList<>();
        for(Point2D wayPoint2D : wayPoints2D){
            markWaypointMarker(new LatLng(wayPoint2D.x,wayPoint2D.y));
            flightWaypoints.add(new LatLng(wayPoint2D.x,wayPoint2D.y));
            // TODO connect waypoints?
        }

        Runnable reachedGoalCallable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      setMapVisible(true);
                                      setBackButtonEnabled(true);
                                  }
                              });
                StatisticEntry statisticEntry = new StatisticEntry(ApplicationModel.fields.get(0).getName(),
                        System.currentTimeMillis(),
                        objectDetection.locs);
                ApplicationModel.INSTANCE.getStatistics().add(statisticEntry);
            }
        };
        FPVDemoApplication.createTimeline(this, reachedGoalCallable);
        FPVDemoApplication.startTimeline(flightWaypoints);

    }


    private void markWaypointMarker(LatLng point){
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
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

    public static boolean checkGpsCoordinates(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
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


    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        if(checkGpsCoordinates(droneLocationLat,droneLocationLng)) {
            float zoomlevel = (float) 18.0;
            CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
            gMap.moveCamera(cu);
        }else
            gMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(51.055705, 13.510207)));
    }


    public void setMapVisible(boolean b) {
        map.setVisibility(b ? View.VISIBLE : View.INVISIBLE);
    }
    public void setBackButtonEnabled(boolean b) {
        backButton.setEnabled(b);
    }

}