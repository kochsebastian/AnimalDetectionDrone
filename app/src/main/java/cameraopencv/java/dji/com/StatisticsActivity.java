package cameraopencv.java.dji.com;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import cameraopencv.java.dji.com.model.StatisticEntry;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.importSDKDemo.model.Field;
import com.dji.importSDKDemo.model.FieldsSelectAdapter;
import com.dji.importSDKDemo.model.StatisticsAdapter;
import org.jetbrains.annotations.NotNull;

public class StatisticsActivity extends Activity implements View.OnClickListener,
        RecyclerViewClickListener {


    private RecyclerView recyclerView;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        initUI();
    }

    private void initUI() {
        RecyclerView.LayoutManager viewManager = new LinearLayoutManager(this);
        RecyclerView.Adapter viewAdapter = new StatisticsAdapter(this);

        recyclerView = findViewById(R.id.statisticlist);
        recyclerView.setLayoutManager(viewManager);
        recyclerView.setAdapter(viewAdapter);

        backButton = findViewById(R.id.statistics_back);
        backButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.statistics_back:
                finish();
                break;
        }

    }

    @Override
    public void recyclerViewListClicked(@NotNull View v, int position) {
        StatisticEntry s = ApplicationModel.statistics.get(position);
        System.out.println("Selected statistic " + s.getFieldName());

        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);



    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}