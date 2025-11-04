package com.metsakuur.lemondemo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.metsakuur.lemondemo.databinding.ActivityMainBinding;
import com.metsakuur.lemondemo.util.BaseAppCompatActivity;

public class MainActivity extends BaseAppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), SelectTypeActivity.class);
                startActivity(intent);
            }
        });

        binding.btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(scanIntent);
            }
        });
    }
}