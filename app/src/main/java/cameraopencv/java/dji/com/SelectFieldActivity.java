package cameraopencv.java.dji.com;


import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.importSDKDemo.model.Field;
import com.dji.importSDKDemo.model.FieldsSelectAdapter;
import org.jetbrains.annotations.NotNull;

public class SelectFieldActivity extends Activity implements View.OnClickListener,
        RecyclerViewClickListener {


    private RecyclerView recyclerView;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_field);
        initUI();
    }

    private void initUI() {
        RecyclerView.LayoutManager viewManager = new LinearLayoutManager(this);
        RecyclerView.Adapter viewAdapter = new FieldsSelectAdapter(this);

        recyclerView = findViewById(R.id.fieldlist);
        recyclerView.setLayoutManager(viewManager);
        recyclerView.setAdapter(viewAdapter);

        backButton = findViewById(R.id.selectfield_back);
        backButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.selectfield_back:
                finish();
                break;
        }

    }

    @Override
    public void recyclerViewListClicked(@NotNull View v, int position) {
        Field f = ApplicationModel.fields.get(position);
        System.out.println("Selected field " + f.getName());
        //finish();

        TimelineFlight tlf = new TimelineFlight(this); // Hinweis: Nicht sicher, ob als Kontext diese Aktiitaet uebergeben werden kann, oder ob es MapActivity.this sein muss
        tlf.runTimeLine(f.getPolygon());
        ApplicationModel.fields.remove(f);
        ApplicationModel.fields.add(0,f);
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);



    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}