package kr.go.namwon.seniorcenter.app.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import kr.go.namwon.seniorcenter.app.databinding.DialogCustomBinding;

public class CustomDialog extends Dialog {

    private String message;
    private String errorCode;
    private String cancelButton;
    private DialogInterface.OnClickListener cancelButtonCallback;
    private String okButton;
    private DialogInterface.OnClickListener okButtonCallback;

    private DialogCustomBinding binding;

    public CustomDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DialogCustomBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setCanceledOnTouchOutside(true);
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setGravity(Gravity.CENTER);
        }

        initView();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismiss();
    }

    private void initView() {
        if (message != null) {
            binding.tvMessage.setText(message);
        }
        if (errorCode != null) {
            binding.tvCode.setText(errorCode);
        }
        if (cancelButton == null || cancelButton.isEmpty()) {
            binding.btnCancel.setVisibility(View.GONE);
            binding.btnOk.setText("확인");
        } else {
            binding.btnCancel.setVisibility(View.VISIBLE);
            binding.btnCancel.setText(cancelButton);
        }

        if (okButton != null && !okButton.isEmpty()) {
            binding.btnOk.setText(okButton);
        }

        if (cancelButtonCallback != null) {
            binding.btnCancel.setOnClickListener(v -> cancelButtonCallback.onClick(CustomDialog.this, DialogInterface.BUTTON_POSITIVE));
        }

        if (okButtonCallback != null) {
            binding.btnOk.setOnClickListener(v -> okButtonCallback.onClick(CustomDialog.this, DialogInterface.BUTTON_POSITIVE));
        }
    }

    public static class Builder {

        private CustomDialog dialog;

        public Builder(Context context) {
            dialog = new CustomDialog(context);
        }

        public Builder setMessage(String message) {
            dialog.message = message;
            return this;
        }

        public Builder setErrorCode(String errorCode) {
            dialog.errorCode = errorCode;
            return this;
        }

        public Builder setCancelButton(String btnString, DialogInterface.OnClickListener onClickListener) {
            dialog.cancelButton = btnString;
            dialog.cancelButtonCallback = onClickListener;
            return this;
        }

        public Builder setOkButton(String btnString, DialogInterface.OnClickListener onClickListener) {
            dialog.okButton = btnString;
            dialog.okButtonCallback = onClickListener;
            return this;
        }

        public Builder create() {
            dialog.onCreate(null);
            return this;
        }

        public CustomDialog show() {
            dialog.show();
            dialog.setCancelable(false);
            return dialog;
        }
    }
}