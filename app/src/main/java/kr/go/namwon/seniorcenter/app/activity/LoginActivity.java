package kr.go.namwon.seniorcenter.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

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
import kr.go.namwon.seniorcenter.app.BuildConfig;
import kr.go.namwon.seniorcenter.app.R;
import kr.go.namwon.seniorcenter.app.databinding.ActivityLoginBinding;
import kr.go.namwon.seniorcenter.app.model.FaceVerifyRequest;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.ImageUtil;
import kr.go.namwon.seniorcenter.app.dialog.LoadingDialog;
import kr.go.namwon.seniorcenter.app.dialog.LoginDialog;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends BaseAppCompatActivity implements UFaceDetectorListener {
    public static final String TAG = "TAG_LoginActivity";

    private ActivityLoginBinding binding;
    private LoadingDialog loadingDialog;
    private LoginDialog loginDialog;
    private UFaceDetector uFaceDetector = null;
    private UFaceResult result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefsHelper.clear();

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadingDialog = new LoadingDialog(LoginActivity.this);
        loginDialog = new LoginDialog(LoginActivity.this);

        initDetector();

        String version = "v" + BuildConfig.VERSION_NAME;
        if (!AppConfig.isProdFlavor()) {
            version += " (dev)";
        }

        binding.versionTextView.setText(version);
        binding.joinBtn.setOnClickListener(view -> startActivity(new Intent(this, SignUpActivity.class)));
        binding.phoneAuthBtn.setOnClickListener(view -> loginDialog.show());
        binding.faceAuthBtn.setOnClickListener(view -> {
            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.empty_face_result), Toast.LENGTH_SHORT).show();
                return;
            }

            uFaceDetector.pauseDetector();
            loadingDialog.show();
            binding.faceAuthBtn.setEnabled(false);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {

                Bitmap resized = ImageUtil.resize(result.getCropImage(), 720);
                String b64 = ImageUtil.bitmapToBase64(resized);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    FaceVerifyRequest request = new FaceVerifyRequest(b64);
                    ApiClient.authApi().verify(request)
                            .enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                    loadingDialog.dismiss();
                                    binding.faceAuthBtn.setEnabled(true);

                                    if (!response.isSuccessful()) {
                                        openAlertView("얼굴을 인식할 수 없습니다.", (dialogInterface, i) -> {
                                            isDetectFace(false);
                                            dialogInterface.dismiss();
                                        });
                                        return;
                                    }

                                    JsonObject res = response.body();
                                    int resCode = res.get("code").getAsInt();
                                    if (resCode == 0) {
                                        JsonObject resultVO = res.get("resultVO").getAsJsonObject();
                                        String accessToken = resultVO.get(AppConfig.tokenAccessKey()).getAsString();
                                        String refreshToken = resultVO.get(AppConfig.tokenRefreshKey()).getAsString();

                                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        intent.putExtra(AppConfig.tokenAccessKey(), accessToken);
                                        intent.putExtra(AppConfig.tokenRefreshKey(), refreshToken);
                                        startActivity(intent);
                                        finishAffinity();
                                        return;
                                    }

                                    switch (resCode) {
                                        case 28001:
                                            openAlertView(getString(R.string.unregistered_user), (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28002:
                                            openAlertView("일치하는 사용자가 없습니다.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28003:
                                            openAlertView("선글라스를 벗고 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28004:
                                            openAlertView("마스크를 벗고 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28005:
                                            openAlertView("눈을 뜨고 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28006:
                                            openAlertView("가까이 다가와서 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28007:
                                            openAlertView("정면에서 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        case 28008:
                                            openAlertView("카메라를 닦은 후 촬영해주세요.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                        default:
                                            openAlertView("얼굴을 인식할 수 없습니다.", (dialogInterface, i) -> {
                                                isDetectFace(false);
                                                dialogInterface.dismiss();
                                            });
                                            break;
                                    }
                                }

                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                    loadingDialog.dismiss();
                                    binding.faceAuthBtn.setEnabled(true);

                                    openAlertView(getString(R.string.login_fail_server_error), (dialogInterface, i) -> {
                                        isDetectFace(false);
                                        dialogInterface.dismiss();
                                    });

                                    Log.e(TAG, "Face authentication failed: " + t.getMessage(), t);
                                }
                            });
                });
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        isDetectFace(false);
    }

    private void initDetector() {
        // 디렉터 초기화
        uFaceDetector = new UFaceDetector();
        // 카메라 프리뷰 세팅
        uFaceDetector.setPreviewView(binding.previewView);
        // 디텍터 리스너 세팅
        uFaceDetector.setFaceDetectorListener(this);
        // 눈깜빡임 사용 여부
        uFaceDetector.setUseEyeBlink(false);
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

    // 얼굴 검출 UI 업데이트
    public void isDetectFace(Boolean isDetectFace) {
        if (isDetectFace) {
            binding.tvCameraText.setText(getString(R.string.camera_front));
            binding.ivGuide.setImageResource(R.drawable.face_guide_green);
            binding.faceAuthBtn.setEnabled(true);
        } else {
            binding.ivGuide.setImageResource(R.drawable.face_guide_red);
            binding.faceAuthBtn.setEnabled(false);
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
                new AlertDialog.Builder(this)
                        .setTitle("권한 필요")
                        .setMessage("안면인식으로 인증하기 위해서는 카메라 권한이 필요합니다.\n설정에서 권한을 허용해주세요.")
                        .setPositiveButton("설정 열기", (d, w) -> {
                            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            i.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(i);
                        })
                        .setNegativeButton("취소", null)
                        .show();
                break;

            default:
                openAlertView(String.format("%s(code: %s)", error.getErrorDescription(), error.getErrorCode()));
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