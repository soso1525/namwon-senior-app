package kr.go.namwon.seniorcenter.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import kr.go.namwon.seniorcenter.app.databinding.ActivityCameraNoticeBinding;
import kr.go.namwon.seniorcenter.app.util.BaseAppCompatActivity;

public class CameraNoticeActivity extends BaseAppCompatActivity {

    private ActivityCameraNoticeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCameraNoticeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(intent);
            }
        });
    }
}