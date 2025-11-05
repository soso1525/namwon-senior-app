package kr.go.namwon.seniorcenter.app;

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

import kr.go.namwon.seniorcenter.app.databinding.ActivityLoginBinding;
import kr.go.namwon.seniorcenter.app.model.FaceVerifyRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.BaseAppCompatActivity;
import kr.go.namwon.seniorcenter.app.util.ImageUtil;
import kr.go.namwon.seniorcenter.app.util.LoadingDialog;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseAppCompatActivity implements UFaceDetectorListener {
    public static final String TAG = "TAG_LoginActivity";

    private ActivityLoginBinding binding;

    private LoadingDialog loadingDialog;

    // 얼굴 검출기 UFaceDetector
    private UFaceDetector uFaceDetector = null;

    // 눈깜빡임 검출 활성화 여부
    Boolean isEyeBlinkEnabled = false;

    // 눈깜빡임 통과 여부, 기본값 성공 및 통과
    Boolean isEyeBlink = true;

    // 좌우 고개돌림 체크 완료
    boolean isYawFinish = true;
    UFaceResult result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.faceAuthBtn.setOnClickListener(view -> {
            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.empty_face_result), Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show();
            binding.faceAuthBtn.setEnabled(false);

            FaceVerifyRequest request = new FaceVerifyRequest(ImageUtil.bitmapToBase64(result.getFullImage()));
            ApiClient.authApi()
                    .verify(request)
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            loadingDialog.dismiss();
                            binding.faceAuthBtn.setEnabled(true);

                            if (response.isSuccessful()) {
                                JsonObject res = response.body();
                                if (res != null) {
                                    String accessToken = res.get("bizportal-access-token").getAsString();
                                    String refreshToken = res.get("bizportal-refresh-token").getAsString();
                                    String memberId = res.get("mdtlMbrId").getAsString();

                                } else {
                                    Log.e(TAG, "Response body is null");
                                }
                            } else {
                                if (response.code() == 404) {
                                    openAlertView(getString(R.string.unregistered_user), (dialogInterface, i) -> dialogInterface.dismiss());
                                }

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


        loadingDialog = new LoadingDialog(LoginActivity.this);
        initDetector();
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
            if (!isEyeBlink) {
                binding.tvCameraText.setText(getString(R.string.camera_eye_blink));
            }
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
            /**
             * 각 프레임 별로 디텍트 여부 호출
             */
            case UFACE_STATE_FACE_DETECTED:
                isDetectFace(true);
                break;

            case UFACE_STATE_FACE_NOT_DETECTED:
                isDetectFace(false);
                this.result = null;
                break;

            /**
             * 얼굴이 너무 멀리있을 때 호출
             */
            case UFACE_STATE_FACE_SMALL:
                binding.tvCameraText.setText(getString(R.string.face_too_far));
                this.result = null;
                break;

            /**
             * 얼굴이 너무 가까울 때 호출
             */
            case UFACE_STATE_FACE_LARGE:
                binding.tvCameraText.setText(getString(R.string.face_too_close));
                this.result = null;
                break;

            /**
             * Blur 감지 되었을 때 호출
             */
            case UFACE_STATE_FACE_BLUR:
                binding.tvCameraText.setText(getString(R.string.face_unclear));
                this.result = null;
                break;

            default:
                this.result = null;
                break;

        }
    }

    /**
     * (ProcessMode = GEOMETRY MODE 일 경우에는 블러 체크 및 얼굴 검출 result 리스너 수행 안됨)
     * UFaceGeometryModel 결과값 수신
     * fullImage : 카메라 화면 전체 이미지
     * boundingBox : 얼굴 크롭 이미지
     * pitch : 얼굴 위 아래 돌림 수치(중앙 0 기준으로 위가 양수 값, 아래가 음수 값)
     * yaw : 얼굴 좌우 돌림 수치 (중앙 0 기준으로 왼쪽이 음수 값, 오른쪽이 양수 값)
     * roll : 얼굴 갸우뚱 수치 (중앙 0 기준으로 오른쪽 갸우뚱이 음수 값, 왼쪽 갸우뚱이 양수 값)
     * landmarks: 얼굴 랜드 마크 정보 (UFaceLandmarks Model)
     */
    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceGeometryModel faceGeometry) {
    }

    /**
     * 에러 발생시 호출 에러코드는 가이드 문서 참조
     */
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

    /**
     * 얼굴 정상 디텍트 되었을 때 호출(블러까지 통과 이후)
     * 해당 리스너가 호출되면 디텍터가 동작을 멈추기 때문에 detector.resumeDetector() 호출해야 다음사진 결과 넘어옵니다.
     */
    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceResult result) {

        // isEyeBlinkEnabled = 데모앱 설정화면 눈깜빡임 사용 여부 환경 변수 (사용 안할 시 false 가 기본값이므로 깜빡임 여부 상관없이 통과)
        // result.isEyeBlinked = 눈깜빡임 성공 여부
        // 환경 변수 조건은 데모앱 시나리오(isEyeBlinkEnabled)
        // 눈깜빡임 여부는 result.isEyeBlinked 로 체크 가능
        if (!isEyeBlinkEnabled || result.isEyeBlinked()) {
            // 눈깜빡임 체크 성공
            isEyeBlink = true;

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