package kr.go.namwon.seniorcenter.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.metsakuur.ufacedetector.UFaceDetector;
import com.metsakuur.ufacedetector.UFaceDetectorListener;
import com.metsakuur.ufacedetector.model.UFaceError;
import com.metsakuur.ufacedetector.model.UFaceGeometryModel;
import com.metsakuur.ufacedetector.model.UFaceProcessingMode;
import com.metsakuur.ufacedetector.model.UFaceResult;
import com.metsakuur.ufacedetector.model.UFaceStateModel;

import kr.go.namwon.seniorcenter.app.R;
import kr.go.namwon.seniorcenter.app.databinding.ActivityLoginBinding;
import kr.go.namwon.seniorcenter.app.databinding.ActivityRegisterBinding;
import kr.go.namwon.seniorcenter.app.model.FaceRegisterRequest;
import kr.go.namwon.seniorcenter.app.model.FaceVerifyRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.ImageUtil;
import kr.go.namwon.seniorcenter.app.util.LoadingDialog;
import kr.go.namwon.seniorcenter.app.util.LoginDialog;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends BaseAppCompatActivity implements UFaceDetectorListener {
    public static final String TAG = "TAG_RegisterActivity";

    private ActivityRegisterBinding binding;

    private LoadingDialog loadingDialog;
    private UFaceDetector uFaceDetector = null;

    // 좌우 고개돌림 체크 완료
    boolean isYawFinish = true;
    UFaceResult result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(RegisterActivity.this);
        initDetector();

        binding.cancelBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.registerBtn.setOnClickListener(view -> {
            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.empty_face_result), Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show();
            binding.registerBtn.setEnabled(false);

            FaceRegisterRequest request = new FaceRegisterRequest(ImageUtil.bitmapToBase64(result.getFullImage()));
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
                                    getOnBackPressedDispatcher().onBackPressed();
                                } else if (code == 28012) {
                                    Toast.makeText(getBaseContext(), "이미 안면인식 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
                                    getOnBackPressedDispatcher().onBackPressed();
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
        uFaceDetector.initDetector(this, "4F5A46527631008115020932123D9CB2313497831B23111BC957CED78F1C6F8731D6A7BEB6ED3B588CC9063F0D6AA09471BDFA61207FF2A0");
    }

    /**
     * 뷰 초기화
     */
    void initView() {
        if (uFaceDetector == null) return;

        uFaceDetector.resumeDetector();
        binding.tvCameraText.setText(getString(R.string.camera_front));
    }

    // 얼굴 검출 UI 업데이트
    public void isDetectFace(Boolean isDetectFace) {
        if (isDetectFace) {
            binding.tvCameraText.setText(getString(R.string.camera_front));
            binding.ivGuide.setImageResource(R.drawable.face_guide_green);
        } else {
            binding.ivGuide.setImageResource(R.drawable.face_guide_red);
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
                isDetectFace(true);
                break;

            case UFACE_STATE_FACE_NOT_DETECTED:
                isDetectFace(false);
                this.result = null;
                break;

            case UFACE_STATE_FACE_SMALL: // 얼굴이 너무 멀리 있을 때
                binding.tvCameraText.setText(getString(R.string.face_too_far));
                this.result = null;
                break;

            case UFACE_STATE_FACE_LARGE: // 얼굴이 너무 가까이 있을 때
                binding.tvCameraText.setText(getString(R.string.face_too_close));
                this.result = null;
                break;

            case UFACE_STATE_FACE_BLUR: // 블러 감지
                binding.tvCameraText.setText(getString(R.string.face_unclear));
                this.result = null;
                break;

            default:
                this.result = null;
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
        // 고개 돌림 성공 체크 (사용 안할 시 true가 기본값이므로 통과)
        if (isYawFinish) {
            this.result = result;
        } else {
            // 해당 리스너로 결과괎이 수신되면 디텍터가 검출을 멈추기 때문에 resumeDetector를 호출해야 디텍터가 다시 동작 함
            // 고개 돌림 아직 진행 중이므로, 고개 돌림 프로세스 다시 진행하도록 processingMode.GEOMETRY_MODE 로 변경
            uFaceDetector.setProcessingMode(UFaceProcessingMode.GEOMETRY_MODE);
            uFaceDetector.resumeDetector();
        }
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