package cameraopencv.java.dji.com;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import cameraopencv.java.dji.com.model.StatisticEntry;
import cameraopencv.java.dji.com.utils.ToastUtils;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

public class StatisticEntryActivity extends FragmentActivity implements View.OnClickListener, OnMapReadyCallback {

    private StatisticEntry stat = ApplicationModel.INSTANCE.getStatisticEntrySelected();

    private GoogleMap gMap;
    private View map;
    private Button backButton;

    private HeatmapTileProvider heatmapTileProvider = null;
    private TileOverlay heatmapTileOverlay = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic_entry);
        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void initUI() {
        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.btn_back:
                Intent intent = new Intent(this, StatisticsActivity.class);
                startActivity(intent);
                break;
        }

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap != null) {
            ToastUtils.showToast("map ready again?!");
            return;
        }
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);


        heatmapTileProvider = new HeatmapTileProvider.Builder()
                .weightedData(stat.getDetections())
                .build();
        // Add a tile overlay to the map, using the heat map tile provider.
        heatmapTileOverlay = gMap.addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));


        float zoomlevel = (float) 15.0;
        double x = 0f;
        double y = 0f;
        for (WeightedLatLng pos : stat.getDetections()) {
            x += pos.getPoint().x;
            y += pos.getPoint().y;
        }
        x /= stat.getDetections().size();
        y /= stat.getDetections().size();

        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(new LatLng(x, y), zoomlevel);
        gMap.moveCamera(cu);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}