package com.metsakuur.lemondemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.metsakuur.lemondemo.databinding.ActivitySelectTypeBinding;
import com.metsakuur.lemondemo.util.BaseAppCompatActivity;
import com.metsakuur.lemondemo.util.UFaceConfig;
import com.metsakuur.ufacedetector.util.UFaceBitmapUtils;

import android.widget.RadioGroup;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
public class SelectTypeActivity extends BaseAppCompatActivity {

    private ActivitySelectTypeBinding binding;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UFaceConfig.getInstance().setUntactType("NID");
        binding.rbNId.setStartPaddingDp(10F);
        binding.rbDid.setStartPaddingDp(10F);
        binding.rbPassport.setStartPaddingDp(10F);

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(androidx.activity.result.ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();

                            if (uri != null) {
                                UFaceConfig.getInstance().setPictureBitmap(getBitmapFromUri(uri));
                                UFaceConfig.getInstance().setPictureByteArray(UFaceBitmapUtils.jpegData(UFaceConfig.getInstance().getPictureBitmap(), 100));
                            }
                            if (UFaceConfig.getInstance().getPictureBitmap() != null) {
                                startActivity(new Intent(getBaseContext(), CameraNoticeActivity.class));
                            }
                        }
                    }
                }
        );

        binding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbNId) {
                    UFaceConfig.getInstance().setUntactType("NID");
                } else if (checkedId == R.id.rbDid) {
                    UFaceConfig.getInstance().setUntactType("DL");
                } else if (checkedId == R.id.rbPassport) {
                    UFaceConfig.getInstance().setUntactType("PP");
                } else {
                    UFaceConfig.getInstance().setUntactType("");
                }
            }
        });

        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("type", "type ::: ${UFaceConfig.getInstance().untactType}");
                Intent intentPicture = new Intent(Intent.ACTION_PICK);
                intentPicture.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                activityResultLauncher.launch(intentPicture);
            }
        });
    }

    public Bitmap getBitmapFromUri(Uri uri) {
        if (Build.VERSION.SDK_INT >= 28) {
            if (uri != null) {
                try {
                    ImageDecoder.Source source = ImageDecoder.createSource(getApplicationContext().getContentResolver(), uri);
                    return ImageDecoder.decodeBitmap(source);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        } else {
            try {
                return MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), uri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}