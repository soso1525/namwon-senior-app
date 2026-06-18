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
    
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
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
    
                // 다이얼로그 띄우는 시점에 디텍터 멈춤 → 확인/취소까지 이미지 고정
                uFaceDetector.pauseDetector();
                UFaceResult fixedResult = result;
    
                Bitmap bitmap = fixedResult.getFullImage();
                View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_face_register, null);
                ImageView imageView = dialogView.findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
    
                new AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setNegativeButton("취소", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            initView(); // resumeDetector() + result 초기화
                        })
                        .setPositiveButton("확인", (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            loadingDialog.show();
                            binding.registerBtn.setEnabled(false);
    
                            executor.execute(() -> {
                                Bitmap resized = ImageUtil.resize(fixedResult.getFullImage(), 720);
                                String b64 = ImageUtil.bitmapToBase64(resized);
    
                                runOnUiThread(() -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    requestRegister(b64);
                                });
                            });
                        })
                        .show();
            });
        }
    
        // -------------------------------------------------------------------------
        // 디텍터 초기화
        // -------------------------------------------------------------------------
    
        private void initDetector() {
            uFaceDetector = new UFaceDetector();
            uFaceDetector.setPreviewView(binding.previewView);
            uFaceDetector.setFaceDetectorListener(this);
            uFaceDetector.setUseEyeBlink(false);
            uFaceDetector.initDetector(this, AppConfig.licenseKey());
        }
    
        // -------------------------------------------------------------------------
        // 뷰 초기화 — 얼굴 결과 초기화 + 디텍터 재개 + UI 리셋
        // -------------------------------------------------------------------------
    
        private void initView() {
            this.result = null;
            if (uFaceDetector != null) {
                uFaceDetector.resumeDetector();
            }
            setFaceDetectedState(false);
        }
    
        // -------------------------------------------------------------------------
        // 얼굴 검출 UI 업데이트 — UI 상태만 변경, 초기화 로직 없음
        // -------------------------------------------------------------------------
    
        private void setFaceDetectedState(boolean detected) {
            if (detected) {
                binding.tvCameraText.setText(getString(R.string.camera_front));
                binding.ivGuide.setImageResource(R.drawable.face_guide_green);
                binding.registerBtn.setEnabled(true);
            } else {
                binding.tvCameraText.setText(getString(R.string.camera_front));
                binding.ivGuide.setImageResource(R.drawable.face_guide_red);
                binding.registerBtn.setEnabled(false);
            }
        }
    
        // -------------------------------------------------------------------------
        // 얼굴 등록 API 요청
        // -------------------------------------------------------------------------
    
        private void requestRegister(String b64) {
            FaceRegisterRequest request = new FaceRegisterRequest(b64);
            ApiClient.authApi().register(request).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    loadingDialog.dismiss();
                    binding.registerBtn.setEnabled(true);
    
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        Log.e(TAG, "register result: " + res);
                        int code = res.get("code").getAsInt();
    
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
                            initView();
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
                                        Log.e(TAG, "Token update result: " + response.body());
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
                        initView();
                    }
                }
    
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    loadingDialog.dismiss();
                    binding.registerBtn.setEnabled(true);
                    Toast.makeText(getBaseContext(), "얼굴을 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Network failure: " + t.getMessage(), t);
                    initView();
                }
            });
        }
    
        // -------------------------------------------------------------------------
        // UFaceDetector Listener
        // -------------------------------------------------------------------------
    
        @Override
        public void uFaceDetectorSetCameraSessionComplete() {
        }
    
        @Override
        public void uFaceDetector(UFaceDetector detector, UFaceStateModel faceState) {
            switch (faceState.getState()) {
                case UFACE_STATE_FACE_DETECTED:
                    break;
    
                case UFACE_STATE_FACE_NOT_DETECTED:
                    setFaceDetectedState(false);
                    break;
    
                case UFACE_STATE_FACE_SMALL:
                    binding.tvCameraText.setText(getString(R.string.face_too_far));
                    setFaceDetectedState(false);
                    break;
    
                case UFACE_STATE_FACE_LARGE:
                    binding.tvCameraText.setText(getString(R.string.face_too_close));
                    setFaceDetectedState(false);
                    break;
    
                case UFACE_STATE_FACE_BLUR:
                    binding.tvCameraText.setText(getString(R.string.face_unclear));
                    setFaceDetectedState(false);
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
                    break;
    
                default:
                    openAlertView(String.format("%s(code: %s)", error.getErrorDescription(), error.getErrorCode()));
                    break;
            }
        }
    
        @Override
        public void uFaceDetector(UFaceDetector detector, UFaceResult result) {
            this.result = result;
            setFaceDetectedState(true);
            detector.resumeDetector();
        }
    
        // -------------------------------------------------------------------------
        // 생명주기
        // -------------------------------------------------------------------------
    
        @Override
        protected void onResume() {
            super.onResume();
            initView();
        }
    
        @Override
        protected void onStart() {
            super.onStart();
            initView();
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
        protected void onDestroy() {
            super.onDestroy();
            this.result = null;
            executor.shutdown();
    
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