package com.metsakuur.lemondemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.metsakuur.lemondemo.databinding.ActivityCameraNoticeBinding;
import com.metsakuur.lemondemo.util.BaseAppCompatActivity;

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
                Intent intent = new Intent(getBaseContext(), CameraActivity.class);
                startActivity(intent);
            }
        });
    }
}