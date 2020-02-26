package dev.syafii.scanbarcode;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.snackbar.Snackbar;
import com.syafii.scanbarcode.R;

import java.io.IOException;

import dev.syafii.scanbarcode.camera.CameraSourcePreview;
import dev.syafii.scanbarcode.camera.GraphicOverlay;
import dev.syafii.scanbarcode.db.BarcodeApp;
import dev.syafii.scanbarcode.util.CustomDialog;
import es.dmoral.toasty.Toasty;

public class BarcodeActivity extends AppCompatActivity {
    private String TAG = "BarcodeActivity";
    private static final int RC_HANDLE_GMS = 9001;
    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    BarcodeApp barcodeApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.barcodeOverplay);
        barcodeApp = new BarcodeApp(this);
        checkPermission();
    }

    public void checkPermission() {
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }


    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    private void createCameraSource() {
        Context context = getApplicationContext();
        // A barcode detector is created to track barcodes.  An associated multi-processor instance
        // is set to receive the barcode detection results, track the barcodes, and maintain
        // graphics for each barcode on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each barcode.
        BarcodeDetector detector = new BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicBarcodeTrackerFactory())
                        .build());
        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Barcode detector dependencies are not yet available.");
            Toast.makeText(BarcodeActivity.this, "Face detector dependencies are not yet available.", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "Barcode detector available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(1920, 1080)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    //==============================================================================================
    // Graphic Barcode Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicBarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
        @Override
        public Tracker<Barcode> create(Barcode barcode) {
            return new GraphicBarcodeTracker(mGraphicOverlay, BarcodeActivity.this);
        }
    }

    /**
     * Barcode tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicBarcodeTracker extends Tracker<Barcode> {
        private GraphicOverlay mOverlay;
        private BarcodeGraphic mBarcodeGraphic;
        Activity activity;

        GraphicBarcodeTracker(GraphicOverlay overlay, Activity activity) {
            mOverlay = overlay;
            this.activity = activity;
            mBarcodeGraphic = new BarcodeGraphic(overlay);
        }

        /**
         * Start tracking the detected item instance within the item overlay.
         */
        @Override
        public void onNewItem(int barcodeId, final Barcode item) {
            mBarcodeGraphic.setId(barcodeId);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (item.rawValue.equals("SG000004")) {
                        Log.e(TAG, "Result Valid : " + item.rawValue);
                        CustomDialog dialog = new CustomDialog();
                        dialog.showDialogBarcode(BarcodeActivity.this, "Dialog", "Your Barcode Valid ", "Yes", "No");
                        barcodeApp.saveBarcode(item.rawValue);
                    } else {
                        Toasty.error(BarcodeActivity.this, "Sorry your barcode invalid", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Result inValid : " + item.rawValue);
                    }
                }
            });
        }

        /**
         * Update the position/characteristics of the barcode within the overlay.
         */
        @Override
        public void onUpdate(Detector.Detections<Barcode> detections, final Barcode barcode) {
            mOverlay.add(mBarcodeGraphic);
            mBarcodeGraphic.updateItem(barcode);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = barcode.rawValue.length(); i <= 0; i++) {
                        if (barcode.rawValue.equals("SG000004")) {
                            Log.e(TAG, "Result Valid : " + barcode.rawValue);
                            CustomDialog dialog = new CustomDialog();
                            dialog.showDialogBarcode(BarcodeActivity.this, "Dialog", "Your Barcode Valid ", "Yes", "No");
                        } else {
                            Toasty.error(BarcodeActivity.this, "Sorry your barcode invalid", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Result inValid : " + barcode.rawValue);
                        }
                    }
                }
            });
        }

        /**
         * Hide the graphic when the corresponding barcode was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(final Detector.Detections<Barcode> detections) {
            mOverlay.remove(mBarcodeGraphic);

//            activity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    Toasty.error(BarcodeActivity.this, "Sorry your barcode invalid", Toast.LENGTH_SHORT).show();
//                    Log.e(TAG, "Result inValid onMissing : " + detections.getDetectedItems());
//                }
//            });
        }

        /**
         * Called when the barcode is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
//        @Override
//        public void onDone() {
//            mOverlay.remove(mBarcodeGraphic);
//
//            activity.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    CustomDialog dialog = new CustomDialog();
//                    dialog.showNoBarcodeDialog(activity, "Dialog", "There is no face. Try again...", "Yes");
//                }
//            });
//        }
    }

    @Override
    public void onBackPressed() {
        CustomDialog dialogBack = new CustomDialog();
        dialogBack.showDialogBarcodeBack(BarcodeActivity.this, "Dialog", "Do you want to exit ?", "Yes", "No");
    }
}
