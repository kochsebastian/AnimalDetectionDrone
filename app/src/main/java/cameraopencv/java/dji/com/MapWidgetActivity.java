package cameraopencv.java.dji.com;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.VectorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.HeatmapTileProvider;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.dji.mapkit.core.maps.DJIMap;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Arrays;
import java.util.Random;

import dji.common.flightcontroller.flyzone.FlyZoneCategory;
import dji.ux.widget.MapWidget;

public class MapWidgetActivity extends Activity implements CompoundButton.OnCheckedChangeListener,
        RadioGroup.OnCheckedChangeListener,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener,
        SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "MapWidgetActivity";
    private MapWidget mapWidget;
    private ImageView selectedIcon;
    private int[] iconIds = {R.id.icon_1, R.id.icon_2, R.id.icon_3, R.id.icon_4, R.id.icon_5};

    public static final String MAP_PROVIDER = "MapProvider";
    private Button flyZoneButton;
    private Spinner mapSpinner, iconSpinner, lineSpinner;
    private SeekBar lineWidthPicker;
    private int lineWidthValue;
    private TextView lineColor;
    private GroundOverlay groundOverlay;
    private TileOverlay tileOverlay;
    private int mapProvider;
    private GoogleMap googleMap;
    private AMap aMap;
    private ScrollView scrollView;
    private ImageButton btnPanel;
    private boolean isPanelOpen = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_widget);
        mapWidget = (MapWidget) findViewById(R.id.map_widget);
        MapWidget.OnMapReadyListener onMapReadyListener = new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setMapType(DJIMap.MapType.Normal);
            }
        };
        Intent intent = getIntent();
        mapProvider = intent.getIntExtra(MAP_PROVIDER, 0);
        switch (mapProvider) {
            case 1:
                mapWidget.initGoogleMap(onMapReadyListener);
                break;
            case 2:
                mapWidget.initAMap(onMapReadyListener);
                break;
            default:
                break;
        }
        mapWidget.onCreate(savedInstanceState);

        findViewById(R.id.btn_map_provider_test).setOnClickListener(this);

        ((CheckBox) findViewById(R.id.home_direction)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.lock_bounds)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.flight_path)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.home_point)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.gimbal_yaw)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.flyzone_unlock)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.flyzone_legend)).setOnCheckedChangeListener(this);
        ((CheckBox) findViewById(R.id.login_state_indicator)).setOnCheckedChangeListener(this);
        ((RadioGroup) findViewById(R.id.map_center_selector)).setOnCheckedChangeListener(this);
        scrollView = (ScrollView) findViewById(R.id.settings_scroll_view);
        btnPanel = (ImageButton) findViewById(R.id.btn_settings);
        btnPanel.setOnClickListener(this);
        findViewById(R.id.clear_flight_path).setOnClickListener(this);
        iconSpinner = (Spinner) findViewById(R.id.icon_spinner);
        iconSpinner.setOnItemSelectedListener(this);
        mapSpinner = (Spinner) findViewById(R.id.map_spinner);
        mapSpinner.setSelection(0, false); // so the listener won't be called before the map is initialized
        mapSpinner.setOnItemSelectedListener(this);
        findViewById(R.id.replace).setOnClickListener(this);
        selectedIcon = (ImageView) findViewById(R.id.icon_1);
        for (int id : iconIds) {
            findViewById(id).setOnClickListener(this);
        }
        findViewById(R.id.icon_1).setSelected(true);
        flyZoneButton = (Button) findViewById(R.id.btn_fly_zone);
        mapWidget.showAllFlyZones();
        flyZoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "fly zone button clicked", Toast.LENGTH_SHORT).show();
            }
        });

        lineSpinner = (Spinner) findViewById(R.id.line_spinner);
        lineSpinner.setOnItemSelectedListener(this);
        lineWidthPicker = (SeekBar) findViewById(R.id.line_width_picker);
        lineWidthPicker.setOnSeekBarChangeListener(this);
        lineColor = (TextView) findViewById(R.id.line_color);
        lineColor.setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()) {
            case R.id.home_direction:
                mapWidget.setDirectionToHomeVisible(isChecked);
                break;
            case R.id.lock_bounds:
                mapWidget.setAutoFrameMap(isChecked);
                break;
            case R.id.flight_path:
                mapWidget.setFlightPathVisible(isChecked);
                break;
            case R.id.home_point:
                mapWidget.setHomeVisible(isChecked);
                break;
            case R.id.gimbal_yaw:
                mapWidget.setGimbalAttitudeVisible(isChecked);
                break;
            case R.id.flyzone_unlock:
                mapWidget.setTapToUnlockEnabled(isChecked);
                break;
            case R.id.flyzone_legend:
                mapWidget.showFlyZoneLegend(isChecked);
                break;
            case R.id.login_state_indicator:
                mapWidget.showDJIAccountLoginIndicator(isChecked);
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int id) {

        switch (id) {
            case R.id.map_center_aircraft:
                mapWidget.setMapCenterLock(MapWidget.MapCenterLock.AIRCRAFT);
                break;
            case R.id.map_center_home:
                mapWidget.setMapCenterLock(MapWidget.MapCenterLock.HOME);
                break;
            case R.id.map_center_none:
                mapWidget.setMapCenterLock(MapWidget.MapCenterLock.NONE);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.clear_flight_path:
                mapWidget.clearFlightPath();
                break;
            case R.id.replace:
                VectorDrawable drawable = (VectorDrawable) selectedIcon.getDrawable();
                if ("Aircraft".equals(iconSpinner.getSelectedItem())) {
                    mapWidget.setAircraftBitmap(getBitmap(drawable));
                } else if ("Home".equals(iconSpinner.getSelectedItem())) {
                    mapWidget.setHomeBitmap(getBitmap(drawable));
                } else if ("Gimbal Yaw".equals(iconSpinner.getSelectedItem())) {
                    mapWidget.setGimbalAttitudeBitmap(getBitmap(drawable));
                } else if ("Locked Zone".equals(iconSpinner.getSelectedItem())) {
                    mapWidget.setSelfLockedBitmap(getBitmap(drawable), 0.5f, 0.5f);
                } else if ("Unlocked Zone".equals(iconSpinner.getSelectedItem())) {
                    mapWidget.setSelfUnlockedBitmap(getBitmap(drawable), 0.5f, 0.5f);
                }

                break;
            case R.id.icon_1:
            case R.id.icon_2:
            case R.id.icon_3:
            case R.id.icon_4:
            case R.id.icon_5:
                ImageView imageView = (ImageView) view;
                imageView.setSelected(true);
                selectedIcon.setSelected(false);
                selectedIcon = imageView;
                break;
            case R.id.btn_map_provider_test:
                addOverlay();
                break;
            case R.id.line_color:
                setRandomLineColor();
            case R.id.btn_settings:
                movePanel();
                break;
        }
    }

    private void movePanel() {
        int translationStart = 0;
        int translationEnd = 0;
        if (isPanelOpen) {
            translationStart = 0;
            translationEnd = -scrollView.getWidth();
        } else {

            scrollView.bringToFront();
            translationStart = -scrollView.getWidth();
            translationEnd = 0;
        }
        TranslateAnimation animate = new TranslateAnimation(
                translationStart, translationEnd, 0, 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isPanelOpen) {
                    mapWidget.bringToFront();

                }
                btnPanel.bringToFront();
                isPanelOpen = !isPanelOpen;

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        scrollView.startAnimation(animate);

    }

    private static Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId()) {
            case R.id.map_spinner:
                if (mapWidget.getMap() != null) {
                    switch ((int) id) {
                        case 0:
                            mapWidget.getMap().setMapType(DJIMap.MapType.Normal);
                            break;
                        case 1:
                            mapWidget.getMap().setMapType(DJIMap.MapType.Satellite);
                            break;
                        default:
                            mapWidget.getMap().setMapType(DJIMap.MapType.Hybrid);
                            break;
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "map not init man", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.line_spinner:
                int width = 5;
                switch ((int) id) {
                    case 0:
                        width = (int) mapWidget.getDirectionToHomeWidth();
                        lineColor.setVisibility(View.VISIBLE);
                        lineColor.setTextColor(mapWidget.getDirectionToHomeColor());
                        break;
                    case 1:
                        width = (int) mapWidget.getFlightPathWidth();
                        lineColor.setVisibility(View.VISIBLE);
                        lineColor.setTextColor(mapWidget.getFlightPathColor());
                        break;
                    case 2:
                        width = (int) mapWidget.getFlyZoneBorderWidth();
                        lineColor.setVisibility(View.GONE);
                        break;
                }
                lineWidthPicker.setProgress(width);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void addOverlay() {
        if (mapWidget.getMap() == null) {
            Toast.makeText(getApplicationContext(), "map not init man", Toast.LENGTH_SHORT).show();
            return;
        }

        float testLat = 37.4419f;
        float testLng = -122.1430f;
        switch (mapProvider) {
            case 1:
                if (groundOverlay == null) {
                    googleMap = (GoogleMap) mapWidget.getMap().getMap();
                    LatLng latLng1 = new LatLng(testLat, testLng);
                    LatLng latLng2 = new LatLng(testLat + 0.25, testLng + 0.25);
                    LatLng latLng3 = new LatLng(testLat - 0.25, testLng - 0.25);
                    LatLngBounds bounds = new LatLngBounds(latLng1, latLng2).including(latLng3);
                    BitmapDescriptor aircraftImage = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.ic_compass_aircraft));
                    GroundOverlayOptions groundOverlayOptions = new GroundOverlayOptions();
                    groundOverlayOptions.image(aircraftImage)
                            .positionFromBounds(bounds)
                            .transparency(0.5f)
                            .visible(true);
                    groundOverlay = googleMap.addGroundOverlay(groundOverlayOptions);
                } else {
                    groundOverlay.remove();
                    groundOverlay = null;
                }
                break;
            case 2:
                if (tileOverlay == null) {
                    aMap = (AMap) mapWidget.getMap().getMap();
                    com.amap.api.maps.model.LatLng[] latlngs = new com.amap.api.maps.model.LatLng[500];
                    for (int i = 0; i < 500; i++) {
                        double x_ = 0;
                        double y_ = 0;
                        x_ = Math.random() * 0.5 - 0.25;
                        y_ = Math.random() * 0.5 - 0.25;
                        latlngs[i] = new com.amap.api.maps.model.LatLng(testLat + x_, testLng + y_);
                    }
                    HeatmapTileProvider.Builder builder = new HeatmapTileProvider.Builder();
                    builder.data(Arrays.asList(latlngs));
                    HeatmapTileProvider heatmapTileProvider = builder.build();
                    TileOverlayOptions tileOverlayOptions = new TileOverlayOptions();
                    tileOverlayOptions.tileProvider(heatmapTileProvider).visible(true);
                    tileOverlay = aMap.addTileOverlay(tileOverlayOptions);
                } else {
                    tileOverlay.remove();
                    tileOverlay = null;
                }
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
        this.lineWidthValue = progressValue;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if ("Home Direction".equals(lineSpinner.getSelectedItem())) {
            mapWidget.setDirectionToHomeWidth(lineWidthValue);
        } else if ("Flight Path".equals(lineSpinner.getSelectedItem())) {
            mapWidget.setFlightPathWidth(lineWidthValue);
        } else if ("Fly Zone Border".equals(lineSpinner.getSelectedItem())) {
            mapWidget.setFlyZoneBorderWidth(lineWidthValue);
        }
    }

    private void setRandomLineColor() {
        Random rnd = new Random();
        @ColorInt int randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        lineColor.setTextColor(randomColor);
        if ("Home Direction".equals(lineSpinner.getSelectedItem())) {
            mapWidget.setDirectionToHomeColor(randomColor);
        } else if ("Flight Path".equals(lineSpinner.getSelectedItem())) {
            mapWidget.setFlightPathColor(randomColor);
        }
    }
}