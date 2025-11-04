package kr.go.namwon.seniorcenter.app;

import android.os.Bundle;
import android.view.View;

import kr.go.namwon.seniorcenter.app.databinding.ActivitySettingBinding;
import kr.go.namwon.seniorcenter.app.util.BaseAppCompatActivity;
import kr.go.namwon.seniorcenter.app.util.UFaceConfig;


public class SettingActivity extends BaseAppCompatActivity {
    private ActivitySettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.cbEyeBlink.setChecked((getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).getBoolean(UFaceConfig.FACE_EYE_BLINK_ENABLED, false)));;
        binding.cbYawRoll.setChecked((getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).getBoolean(UFaceConfig.FACE_YAWROLL_ENABLED, false)));

        binding.btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).edit().putBoolean(UFaceConfig.FACE_EYE_BLINK_ENABLED, binding.cbEyeBlink.isChecked()).apply();
                getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).edit().putBoolean(UFaceConfig.FACE_YAWROLL_ENABLED, binding.cbYawRoll.isChecked()).apply();
                finish();
            }
        });
    }
}