package kr.go.namwon.seniorcenter.app.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import kr.go.namwon.seniorcenter.app.util.Ringer;
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

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 벨소리 선택 결과 처리
    private final ActivityResultLauncher<Intent> ringtoneLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    Ringer.saveRingtoneUri(this, uri);
                    Toast.makeText(this, "알림 소리가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });

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

        binding.notiSettingImageView.setOnClickListener(view -> openRingtonePicker());

        binding.joinBtn.setOnClickListener(view ->
                startActivity(new Intent(this, SignUpActivity.class))
        );

        binding.phoneAuthBtn.setOnClickListener(view ->
                loginDialog.show()
        );

        binding.faceAuthBtn.setOnClickListener(view -> {
            if (result == null) {
                Toast.makeText(getApplicationContext(), getString(R.string.empty_face_result), Toast.LENGTH_SHORT).show();
                return;
            }

            // 버튼 클릭 시점에 디텍터 멈춤 → 이 시점에 가장 가까운 result 사용
            uFaceDetector.pauseDetector();
            loadingDialog.show();
            binding.faceAuthBtn.setEnabled(false);

            executor.execute(() -> {
                Bitmap resized = ImageUtil.resize(result.getFullImage(), 720);
                String b64 = ImageUtil.bitmapToBase64(resized);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    requestVerify(b64);
                });
            });
        });
    }

    // -------------------------------------------------------------------------
    // 벨소리 선택 팝업
    // -------------------------------------------------------------------------

    private void openRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "알림 소리 선택");
        // 현재 선택된 벨소리 표시
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Ringer.getSavedRingtoneUri(this));
        ringtoneLauncher.launch(intent);
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
            binding.faceAuthBtn.setEnabled(true);
        } else {
            binding.tvCameraText.setText(getString(R.string.camera_front));
            binding.ivGuide.setImageResource(R.drawable.face_guide_red);
            binding.faceAuthBtn.setEnabled(false);
        }
    }

    // -------------------------------------------------------------------------
    // 얼굴 인증 API 요청
    // -------------------------------------------------------------------------

    private void requestVerify(String b64) {
        FaceVerifyRequest request = new FaceVerifyRequest(b64);
        ApiClient.authApi().verify(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                loadingDialog.dismiss();
                binding.faceAuthBtn.setEnabled(true);

                if (!response.isSuccessful()) {
                    showErrorAndReset("얼굴을 인식할 수 없습니다.");
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
                        showErrorAndReset(getString(R.string.unregistered_user));
                        break;
                    case 28002:
                        showErrorAndReset("일치하는 사용자가 없습니다.");
                        break;
                    case 28003:
                        showErrorAndReset("선글라스를 벗고 촬영해주세요.");
                        break;
                    case 28004:
                        showErrorAndReset("마스크를 벗고 촬영해주세요.");
                        break;
                    case 28005:
                        showErrorAndReset("눈을 뜨고 촬영해주세요.");
                        break;
                    case 28006:
                        showErrorAndReset("가까이 다가와서 촬영해주세요.");
                        break;
                    case 28007:
                        showErrorAndReset("정면에서 촬영해주세요.");
                        break;
                    case 28008:
                        showErrorAndReset("카메라를 닦은 후 촬영해주세요.");
                        break;
                    default:
                        showErrorAndReset("얼굴을 인식할 수 없습니다.");
                        break;
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                loadingDialog.dismiss();
                binding.faceAuthBtn.setEnabled(true);
                showErrorAndReset(getString(R.string.login_fail_server_error));
                Log.e(TAG, "Face authentication failed: " + t.getMessage(), t);
            }
        });
    }

    // -------------------------------------------------------------------------
    // 에러 다이얼로그 표시 후 디텍터 초기화
    // -------------------------------------------------------------------------

    private void showErrorAndReset(String message) {
        openAlertView(message, (dialogInterface, i) -> {
            initView();
            dialogInterface.dismiss();
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
                // UFaceResult 콜백에서 최종 확인 후 버튼 활성화하므로 여기선 처리 없음
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
        // 눈깜빡임/고개돌림 미사용 — 처리 없음
    }

    @Override
    public void uFaceDetector(UFaceDetector detector, UFaceError error) {
        switch (error.getErrorCode()) {
            case "72001":
                detector.resumeDetector();
                break;

            case "73001":
                // 카메라 권한 거부
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
        // blur까지 통과한 얼굴 결과 — result를 최신으로 계속 교체
        // resumeDetector()로 재개해서 버튼 누르는 시점에 가장 가까운 프레임이 전송되도록 함
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