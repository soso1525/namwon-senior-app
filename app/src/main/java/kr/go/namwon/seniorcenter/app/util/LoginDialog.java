package kr.go.namwon.seniorcenter.app.util;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;

import kr.go.namwon.seniorcenter.app.R;
import kr.go.namwon.seniorcenter.app.activity.MainActivity;
import kr.go.namwon.seniorcenter.app.databinding.DialogPhoneLoginBinding;
import kr.go.namwon.seniorcenter.app.model.LoginRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginDialog extends Dialog {

    private static final String TAG = "LoginDialog";
    private LoadingDialog loadingDialog;
    private Button loginBtn, cancelBtn;
    private EditText phoneEt, passwordEt;


    public LoginDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_phone_login);

        loadingDialog = new LoadingDialog(getContext());

        phoneEt = findViewById(R.id.phoneEt);
        passwordEt = findViewById(R.id.passwordEt);
        loginBtn = findViewById(R.id.loginBtn);
        cancelBtn = findViewById(R.id.cancelBtn);

        loginBtn.setOnClickListener(v -> {

            String phone = phoneEt.getText().toString();
            String password = passwordEt.getText().toString();

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "휴대폰 번호와 비밀번호를 확인해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show();
            loginBtn.setEnabled(false);

            LoginRequest request = new LoginRequest(StringUtil.formatPhoneNumber(phone), password);
            ApiClient.authApi()
                    .login(StringUtil.formatPhoneNumber(phone), password)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                            loadingDialog.dismiss();
                            loginBtn.setEnabled(true);

                            if (response.isSuccessful()) {
                                JsonObject res = response.body();
                                Log.e(TAG, "response body: " + res);

                                if (res != null) {
                                    String accessToken = res.get(Constants.TokenAccessKey).getAsString();
                                    String refreshToken = res.get(Constants.TokenRefreshKey).getAsString();

                                    PrefsHelper.putString("accessToken", accessToken);
                                    PrefsHelper.putString("refreshToken", refreshToken);

                                    Intent intent = new Intent(getContext(), MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    intent.putExtra("accessToken", accessToken);
                                    intent.putExtra("refreshToken", refreshToken);
                                    getContext().startActivity(intent);
                                    dismiss();
                                } else {
                                    Log.e(TAG, "Response body is null");
                                }
                            } else {
                                Toast.makeText(getContext(), getContext().getString(R.string.login_fail_message), Toast.LENGTH_SHORT).show();
                                dismiss();

                                try (ResponseBody errorBody = response.errorBody()) {
                                    String err = errorBody != null ? errorBody.string() : "null";
                                    Log.e(TAG, "Server error " + response.code() + ": " + err);
                                } catch (Exception e) {
                                    Log.e(TAG, "Read errorBody failed", e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            loadingDialog.dismiss();
                            Log.e(TAG, "Network failure: " + t.getMessage(), t);
                        }
                    });
        });

        cancelBtn.setOnClickListener(v -> this.dismiss());

        // 취소 불가능
        setCancelable(false);

        // 배경을 투명하게 바꿔줌
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(128, 0, 0, 0)));
        }

    }
}