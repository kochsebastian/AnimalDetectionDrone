package cameraopencv.java.dji.com;


import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import com.dji.importSDKDemo.model.ApplicationModel;
import com.dji.importSDKDemo.model.Field;
import com.dji.importSDKDemo.model.FieldsDeleteAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class FieldsActivity extends Activity implements View.OnClickListener,
        RecyclerViewClickListener {


    private RecyclerView recyclerView;
    private Button addButton;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fields);
        initUI();
    }

    private void initUI() {
        RecyclerView.LayoutManager viewManager = new LinearLayoutManager(this);
        RecyclerView.Adapter viewAdapter = new FieldsDeleteAdapter(this);


        recyclerView = findViewById(R.id.fieldlist);
        recyclerView.setLayoutManager(viewManager);
        recyclerView.setAdapter(viewAdapter);

        addButton = findViewById(R.id.add_field);
        addButton.setOnClickListener(this);

        backButton = findViewById(R.id.fields_back);
        backButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()) {
            case R.id.add_field:
                Intent intent = new Intent(this, AddFieldActivity.class);
                startActivity(intent);
                //recyclerView.getAdapter().notifyItemInserted(0);
                break;

            case R.id.fields_back:
                finish();
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void recyclerViewListClicked(@NotNull View v, int position) {
        ApplicationModel.fields.remove(position);
        recyclerView.getAdapter().notifyItemRemoved(position);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}