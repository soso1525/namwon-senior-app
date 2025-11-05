package kr.go.namwon.seniorcenter.app.activity;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;

import kr.go.namwon.seniorcenter.app.util.CustomDialog;

public class BaseAppCompatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void openAlertView(
            String msg,
            String errorCode,
            String btnCancel,
            DialogInterface.OnClickListener onCancelClickListener,
            String btnOk,
            DialogInterface.OnClickListener onOkClickListener
    ) {
        if (msg == null) msg = "";
        if (errorCode == null) errorCode = "";
        if (btnCancel == null) btnCancel = "";
        if (btnOk == null) btnOk = "확인";

        String sMsg = msg;
        String sErrorCode = errorCode;
        String sBtnCancel = btnCancel;
        String sBtnOk = btnOk;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CustomDialog.Builder builder = new CustomDialog.Builder(BaseAppCompatActivity.this)
                        .setMessage(sMsg)
                        .setErrorCode(sErrorCode)
                        .setCancelButton(sBtnCancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (onCancelClickListener != null) {
                                    onCancelClickListener.onClick(dialog, which);
                                }
                            }
                        })
                        .setOkButton(sBtnOk, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (onOkClickListener != null) {
                                    onOkClickListener.onClick(dialog, which);
                                } else {
                                    dialog.dismiss();
                                }
                            }
                        });

                CustomDialog.Builder alertDialog = builder.create();

                if (!BaseAppCompatActivity.this.isFinishing()) {
                    alertDialog.show();
                }
            }
        });
    }

    public void openAlertView(
            String msg
    ) {
        openAlertView(msg, null, null,null, null, null);
    }

    public void openAlertView(
            String msg, DialogInterface.OnClickListener onOkClickListener
    ) {
        openAlertView(msg, null, null,null, null, onOkClickListener);
    }

}