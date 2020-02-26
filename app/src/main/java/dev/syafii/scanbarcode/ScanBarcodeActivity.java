package dev.syafii.scanbarcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.syafii.scanbarcode.R;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import dev.syafii.scanbarcode.util.CustomDialog;
import es.dmoral.toasty.Toasty;

public class ScanBarcodeActivity extends AppCompatActivity {
    private String TAG = "ScanBarcodeActivity";
    private CameraSource mCameraSource;
    private BarcodeDetector detector;
    private SurfaceView surfaceView;
    private static final int REQUEST_CAMERA_PERMISSION = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_barcode_activity);
        surfaceView = findViewById(R.id.surfaceView);

        createCameraSource();


    }


    public void createCameraSource() {
        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

                try {
                    // check permission
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(ScanBarcodeActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                REQUEST_CAMERA_PERMISSION);
                        return;
                    }
                    mCameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });
        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
                Toast.makeText(getApplicationContext(), "Barcode scanner has been stopped", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> result = detections.getDetectedItems();
                if (result.size() != 0) {
                    Barcode barcode = result.valueAt(0);
                    Log.e(TAG, "BarcodeResult : " + barcode.rawValue);
                    if (barcode.rawValue.equals("SG000004")) {
                        Log.e(TAG, "receiveDetections: " + barcode.rawValue);
//                        dialog = new CustomDialog();
//                        dialog.showDialogBarcode(ScanBarcodeActivity.this, "Dialog", "Your Valid", "Yes", "No");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                CustomDialog dialog = new CustomDialog();
                                dialog.showDialogBarcode(ScanBarcodeActivity.this, "Dialog", "Your Barcode Valid ", "Yes", "No");
                            }
                        });
                    } else {
                        Log.e(TAG, "Else: " + barcode.rawValue);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toasty.error(ScanBarcodeActivity.this, "Sorry your barcode invalid", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

            }
        });

        if (!detector.isOperational()) {
            Log.w(TAG, "Barcode detector dependencies are not yet available.");
        } else {
            Log.w(TAG, "Barcode detector available.");
        }

        mCameraSource = new CameraSource.Builder(this, detector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1920, 1080)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        createCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    //method for focus camera
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            float touchMajor = event.getTouchMajor();
            float touchMinor = event.getTouchMinor();

            Rect touchRect = new Rect((int) (x - touchMajor / 2), (int) (y - touchMinor / 2), (int) (x + touchMajor / 2), (int) (y + touchMinor / 2));

            this.submitFocusAreaRect(touchRect);
        }
        return super.onTouchEvent(event);
    }

    private void submitFocusAreaRect(Rect touchRect) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(mCameraSource);
                    if (camera != null) {
                        Camera.Parameters cameraParameters = camera.getParameters();

                        if (cameraParameters.getMaxNumFocusAreas() == 0) {
                            return;
                        }

                        Rect focusArea = new Rect();

                        focusArea.set(touchRect.left * 2000 / surfaceView.getWidth() - 1000,
                                touchRect.top * 2000 / surfaceView.getHeight() - 1000,
                                touchRect.right * 2000 / surfaceView.getWidth() - 1000,
                                touchRect.bottom * 2000 / surfaceView.getHeight() - 1000);

                        ArrayList<Camera.Area> focusAreas = new ArrayList<>();
                        focusAreas.add(new Camera.Area(focusArea, 1000));

                        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        cameraParameters.setFocusAreas(focusAreas);
                        camera.setParameters(cameraParameters);

                        camera.autoFocus((Camera.AutoFocusCallback) this);
                    }
                } catch (IllegalAccessException | RuntimeException e) {
                    e.getMessage();
                }

                break;

            }
        }
    }

    @Override
    public void onBackPressed() {
        CustomDialog dialog = new CustomDialog();
        dialog.showDialogBarcodeBack(ScanBarcodeActivity.this, "Dialog", "Do you want to exit ?", "Yes", "No");
    }


}


