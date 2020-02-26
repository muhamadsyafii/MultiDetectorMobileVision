package dev.syafii.scanbarcode.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.syafii.scanbarcode.R;

import java.util.Objects;

import dev.syafii.scanbarcode.ResultBarcodeActivity;

public class CustomDialog {
    public void showDialogBarcode(final Activity activity, String title, String message,
                                  String btnTextYes, String btnTextNo) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.item_dialog_barcode);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        tvTitle.setText(title);
        TextView tvMessage = dialog.findViewById(R.id.tv_message);
        tvMessage.setText(message);
// Yes
        Button dialogYes = dialog.findViewById(R.id.btn_yes);
        dialogYes.setText(btnTextYes);
        dialogYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, ResultBarcodeActivity.class);
                activity.startActivity(intent);
                activity.finish();
                Toast.makeText(activity, "Sucess", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
// No
        final Button dialogNo = dialog.findViewById(R.id.btn_no);
        dialogNo.setText(btnTextNo);
        dialogNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ActivityUtils.openActivity(activity, BarcodeActivity.class);
//                activity.finish();
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void showDialogBarcodeBack(final Activity activity, String title, String message, String btnTextYes, String btnTextNo) {

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.item_dialog_barcode);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvTitle = dialog.findViewById(R.id.tv_title);
        tvTitle.setText(title);

        TextView tvMessage = dialog.findViewById(R.id.tv_message);
        tvMessage.setText(message);

        // Yes
        Button dialogYes = dialog.findViewById(R.id.btn_yes);
        dialogYes.setText(btnTextYes);
        dialogYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityUtils.closeActivity(activity);
                dialog.dismiss();
            }
        });

        // No
        final Button dialogNo = dialog.findViewById(R.id.btn_no);
        dialogNo.setText(btnTextNo);
        dialogNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                ActivityUtils.openActivity(activity, ScanBarcodeActivity.class);
//                activity.finish();
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
