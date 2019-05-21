package cameraopencv.java.dji.com;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import cameraopencv.java.dji.com.geometrics.Point2D;
import cameraopencv.java.dji.com.model.PolygonGrid;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.importSDKDemo.model.Field;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import dji.common.flightcontroller.FlightControllerState;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.TimelineMission;
import dji.sdk.products.Aircraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;




public class AddFieldActivity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "AddFieldActivity";
    private GoogleMap gMap;

    private Button undo, finish;
    private TextView fieldName;

    // actually also show drone location. Maybe someone uses the drone while adding fields.
    private double droneLocationLat = 181, droneLocationLng = 181;
    private Marker droneMarker = null;
    private FlightController mFlightController;

    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Polygon polygon = null;


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
        unregisterReceiver(mReceiver);
    }

    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void initUI() {
        undo = findViewById(R.id.add_field_undo);
        undo.setEnabled(false);
        undo.setOnClickListener(this);

        finish = findViewById(R.id.add_field_finish);
        finish.setOnClickListener(this);

        fieldName = findViewById(R.id.add_field_fieldname);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_field);

        //Register BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(FPVDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // manually called because when drone is already connected the productChange event will not be triggered anymore
        initFlightController();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add_field_undo:
                removeMarker(mMarkers.size() - 1);
                break;

            case R.id.add_field_finish:

                List<LatLng> polygon = new ArrayList<>();
                for (int i = 0; i < mMarkers.size(); i++) {
                    Marker marker = mMarkers.get(i);
                    polygon.add(marker.getPosition());
                }


                PolygonGrid pG = new PolygonGrid();
                List<Point2D> wayPoints2D;
                try {
                    wayPoints2D = pG.makeGrid(40, polygon);
                }catch(IllegalArgumentException e){
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast("Feld muss drei Punkte haben");
                        }
                    });
                    break;
                }catch(NullPointerException e){
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showToast("Feld zu klein zum Abfliegen");
                        }
                    });
                    break;
                }

                Field field = new Field(fieldName.getText().toString(), polygon);
                ApplicationModel.INSTANCE.getFields().add(0,field);

                List<LatLng> flightWaypoints = new ArrayList<>();
                for(Point2D wayPoint2D : wayPoints2D){
                    markWaypointMarker(new LatLng(wayPoint2D.x,wayPoint2D.y));
                    flightWaypoints.add(new LatLng(wayPoint2D.x,wayPoint2D.y));
                }
                ToastUtils.showToast(""+flightWaypoints.size());
                double distance = pG.calcDistance(wayPoints2D);
                if(distance >= 7200)
                    ToastUtils.showToast("Feld wahrscheinlich zu gross!");

                finish();
                break;

            default:
                break;
        }
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            gMap.setOnMapClickListener(this);
            gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        cameraUpdate();
    }


    @Override
    public void onMapClick(LatLng point) {
        markWaypoint(point);
    }

    private void markWaypoint(LatLng point){
        //Create marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
        undo.setEnabled(true);
        updatePolygon();
    }

    private void markWaypointMarker(LatLng point){
        //Create marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
        undo.setEnabled(true);
      //  updatePolygon();
    }

    private void removeMarker(int index){
        Marker marker = mMarkers.get(index);
        mMarkers.remove(index);
        marker.remove();
        if (mMarkers.isEmpty()) {
            undo.setEnabled(false);
        }
        updatePolygon();
    }

    private void updatePolygon() {
        if (polygon != null) {
            polygon.remove();
            polygon = null;
        }

        if (!mMarkers.isEmpty()) {
            PolygonOptions polygonOptions = new PolygonOptions();
            for (int i = 0; i < mMarkers.size(); i++) {
                Marker m = mMarkers.get(i);
                polygonOptions.add(m.getPosition());
            }
            polygon = gMap.addPolygon(polygonOptions);
        }
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
            gMap.moveCamera(CameraUpdateFactory.newLatLng( new LatLng(51.055705, 13.510207)));
    }



}