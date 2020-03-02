package dev.syafii.scanbarcode.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.syafii.scanbarcode.R;

import dev.syafii.scanbarcode.db.BarcodeApp;
import dev.syafii.scanbarcode.util.Constant;

public class ResultBarcodeActivity extends AppCompatActivity {

    TextView tvResult;
    Button btnRemove;
    BarcodeApp barcodeApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_barcode);
        barcodeApp = new BarcodeApp(this);

        tvResult = findViewById(R.id.result);
        btnRemove = findViewById(R.id.clearApp);

        tvResult.setText(barcodeApp.getString(Constant.BARCODE));
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeApp.clearSharedPreference();
                tvResult.setText("");
            }
        });
    }
}
