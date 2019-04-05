package cameraopencv.java.dji.com;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;
import dji.ux.widget.MapWidget;

public class MenuActivity extends Activity implements View.OnClickListener {



    private MapWidget mapWidget;
    private Button mFields, mFlight, mStatistics;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_menu);

        initUI(savedInstanceState);
        ApplicationModel.INSTANCE.load();
    }

    private void initUI(Bundle savedInstanceState) {
        mFields = findViewById(R.id.fields);
        mFlight = findViewById(R.id.flight);
        mStatistics = findViewById(R.id.statistics);

        mFields.setOnClickListener(this);
        mFlight.setOnClickListener(this);
        mStatistics.setOnClickListener(this);

        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);
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
                Intent intent3 = new Intent(this, AddFieldActivity.class);
                startActivity(intent3);
                break;

            default:
                break;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


}