package kr.go.namwon.seniorcenter.app.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.metsakuur.ufacedetector.UFaceDetector;
import com.metsakuur.ufacedetector.UFaceDetectorListener;
import com.metsakuur.ufacedetector.model.UFaceError;
import com.metsakuur.ufacedetector.model.UFaceGeometryModel;
import com.metsakuur.ufacedetector.model.UFaceResult;
import com.metsakuur.ufacedetector.model.UFaceStateModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.go.namwon.seniorcenter.app.AppConfig;
import kr.go.namwon.seniorcenter.app.R;
import kr.go.namwon.seniorcenter.app.databinding.ActivityFaceRegisterBinding;
import kr.go.namwon.seniorcenter.app.model.FaceRegisterRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.ImageUtil;
import kr.go.namwon.seniorcenter.app.dialog.LoadingDialog;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FaceRegisterActivity extends BaseAppCompatActivity implements UFaceDetectorListener {
    public static final String TAG = "TAG_RegisterActivity";

    private ActivityFaceRegisterBinding binding;
    private LoadingDialog loadingDialog;
    private UFaceDetector uFaceDetector = null;
    private UFaceResult result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFaceRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(FaceRegisterActivity.this);
        initDetector();

        binding.cancelBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.registerBtn.setOnClickListener(view -> {
            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.empty_face_result), Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap bitmap = result.getFullImage();
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_face_register, null);

            ImageView imageView = dialogView.findViewById(R.id.imageView);
            imageView.setImageBitmap(bitmap);

            new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setNegativeButton("취소", (dialogInterface, i) -> {
                        isDetectFace(false);
                        initView();
                    })
                    .setPositiveButton("확인", (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        uFaceDetector.pauseDetector();
                        loadingDialog.show();
                        binding.registerBtn.setEnabled(false);

                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            Bitmap resized = ImageUtil.resize(result.getCropImage(), 720);
                            String b64 = ImageUtil.bitmapToBase64(resized);

                            runOnUiThread(() -> {
                                if (isFinishing() || isDestroyed()) return;

                                FaceRegisterRequest request = new FaceRegisterRequest(b64);
                                ApiClient.authApi()
                                        .register(request)
                                        .enqueue(new Callback<JsonObject>() {
                                            @Override
                                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                                loadingDialog.dismiss();
                                                binding.registerBtn.setEnabled(true);

                                                if (response.isSuccessful()) {
                                                    JsonObject res = response.body();
                                                    Log.e(TAG, "register result: " + res);

                                                    int code = res.get("code").getAsInt();
                                                    String message = res.get("message").getAsString();

                                                    if (code == 0) {
                                                        Toast.makeText(getBaseContext(), "얼굴이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                                                        Intent resultIntent = new Intent();
                                                        resultIntent.putExtra("refresh", true);
                                                        setResult(RESULT_OK, resultIntent);
                                                        finish();
                                                    } else if (code == 28012) {
                                                        Toast.makeText(getBaseContext(), "이미 안면인식 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                                    }
                                                } else {
                                                    Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                                    try (ResponseBody errorBody = response.errorBody()) {
                                                        String err = errorBody != null ? errorBody.string() : "null";
                                                        Log.e(TAG, "Server error " + response.code() + ": " + err);

                                                        if (response.code() == 401) {
                                                            ApiClient.authApi().updateToken().enqueue(new Callback<JsonObject>() {
                                                                @Override
                                                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                                                    JsonObject res = response.body();
                                                                    Log.e(TAG, "Token update result: " + res);
                                                                }

                                                                @Override
                                                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                                                    Log.e(TAG, "Token update fail: " + t.getMessage());
                                                                }
                                                            });
                                                        }

                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Read errorBody failed", e);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                                loadingDialog.dismiss();
                                                Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                                Log.e(TAG, "Network failure: " + t.getMessage(), t);
                                            }
                                        });
                            });
                        });
                    })
                    .show();

//            loadingDialog.show();
//            binding.registerBtn.setEnabled(false);
//
//            FaceRegisterRequest request = new FaceRegisterRequest(ImageUtil.bitmapToBase64(result.getFullImage()));
//            ApiClient.authApi()
//                    .register(request)
//                    .enqueue(new Callback<JsonObject>() {
//                        @Override
//                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                            loadingDialog.dismiss();
//                            binding.registerBtn.setEnabled(true);
//
//                            if (response.isSuccessful()) {
//                                JsonObject res = response.body();
//                                Log.e(TAG, "register result: " + res);
//
//                                int code = res.get("code").getAsInt();
//                                String message = res.get("message").getAsString();
//
//                                if (code == 0) {
//                                    Toast.makeText(getBaseContext(), "얼굴이 등록되었습니다.", Toast.LENGTH_SHORT).show();
//                                    getOnBackPressedDispatcher().onBackPressed();
//                                } else if (code == 28012) {
//                                    Toast.makeText(getBaseContext(), "이미 안면인식 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
//                                    getOnBackPressedDispatcher().onBackPressed();
//                                } else {
//                                    Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
//                                }
//                            } else {
//                                Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
//                                try (ResponseBody errorBody = response.errorBody()) {
//                                    String err = errorBody != null ? errorBody.string() : "null";
//                                    Log.e(TAG, "Server error " + response.code() + ": " + err);
//
//                                    if (response.code() == 401) {
//                                        ApiClient.authApi().updateToken().enqueue(new Callback<JsonObject>() {
//                                            @Override
//                                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
//                                                JsonObject res = response.body();
//                                                Log.e(TAG, "Token update result: " + res);
//                                            }
//
//                                            @Override
//                                            public void onFailure(Call<JsonObject> call, Throwable t) {
//                                                Log.e(TAG, "Token update fail: " + t.getMessage());
//                                            }
//                                        });
//                                    }
//
//                                } catch (Exception e) {
//                                    Log.e(TAG, "Read errorBody failed", e);
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(Call<JsonObject> call, Throwable t) {
//                            loadingDialog.dismiss();
//                            Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
//                            Log.e(TAG, "Network failure: " + t.getMessage(), t);
//                        }
//                    });
        });
    }

    private void initDetector() {

        // 디렉터 초기화
        uFaceDetector = new UFaceDetector();
        // 카메라 프리뷰 세팅
        uFaceDetector.setPreviewView(binding.previewView);
        // 디텍터 리스너 세팅
        uFaceDetector.setFaceDetectorListener(this);
        // 눈깜빡임 사용 여부
        uFaceDetector.setUseEyeBlink(true);
        // 디텍터 초기화
        uFaceDetector.initDetector(this, AppConfig.licenseKey());
    }

    /**
     * 뷰 초기화
     */
    void initView() {
        this.result = null;
        if (uFaceDetector != null)
            uFaceDetector.resumeDetector();
    }

    public void isDetectFace(Boolean isDetectFace) {
        if (isDetectFace) {
            binding.tvCameraText.setText(getString(R.string.camera_front));
            binding.ivGuide.setImageResource(R.drawable.face_guide_green);
            binding.registerBtn.setEnabled(true);
        } else {
            binding.ivGuide.setImageResource(R.drawable.face_guide_red);
            binding.registerBtn.setEnabled(false);
            initView();
        }
    }

    @Override
    public void uFaceDetectorSetCameraSessionComplete() {
    }

    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceStateModel faceState) {
        switch (faceState.getState()) {
            case UFACE_STATE_FACE_DETECTED:
                break;

            case UFACE_STATE_FACE_NOT_DETECTED:
                isDetectFace(false);
                break;

            case UFACE_STATE_FACE_SMALL: // 얼굴이 너무 멀리 있을 때
                binding.tvCameraText.setText(getString(R.string.face_too_far));
                isDetectFace(false);
                break;

            case UFACE_STATE_FACE_LARGE: // 얼굴이 너무 가까이 있을 때
                binding.tvCameraText.setText(getString(R.string.face_too_close));
                isDetectFace(false);
                break;

            case UFACE_STATE_FACE_BLUR: // 블러 감지
                binding.tvCameraText.setText(getString(R.string.face_unclear));
                isDetectFace(false);
                break;

            default:
                break;
        }
    }

    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceGeometryModel faceGeometry) {
    }

    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceError error) {
        switch (error.getErrorCode()) {
            case "72001":
                detector.resumeDetector();
                break;

            case "73001":
                // 카메라 권한 거부 시 호출.
                break;

            default:
                openAlertView("${error.errorDescription}(code : ${error.errorCode})");
                break;
        }
    }

    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceResult result) {
        isDetectFace(true);
        this.result = result;
        detector.resumeDetector();
    }

    @Override
    protected void onStop() {
        super.onStop();
        this.result = null;

        if (uFaceDetector != null) {
            uFaceDetector.pauseDetector();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.result = null;

        if (uFaceDetector != null) {
            uFaceDetector.deinitDetector();
            if (uFaceDetector.getPreview() != null) {
                uFaceDetector.getPreview().setSurfaceProvider(null);
            }
            if (uFaceDetector.getCameraExecutor() != null) {
                uFaceDetector.getCameraExecutor().shutdown();
            }
            if (uFaceDetector.getCameraProvider() != null) {
                uFaceDetector.getCameraProvider().unbindAll();
            }
        }
    }
}