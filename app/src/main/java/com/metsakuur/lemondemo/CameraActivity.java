package com.metsakuur.lemondemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import com.metsakuur.lemondemo.databinding.ActivityCameraBinding;
import com.metsakuur.lemondemo.retrofit.UFaceApiManager;
import com.metsakuur.lemondemo.retrofit.UFaceDataUntactRequest;
import com.metsakuur.lemondemo.retrofit.UFaceResultData;
import com.metsakuur.lemondemo.util.BaseAppCompatActivity;
import com.metsakuur.lemondemo.util.LoadingDialog;
import com.metsakuur.lemondemo.util.UFaceConfig;
import com.metsakuur.ufacedetector.UFaceDetector;
import com.metsakuur.ufacedetector.UFaceDetectorListener;
import com.metsakuur.ufacedetector.model.UFaceError;
import com.metsakuur.ufacedetector.model.UFaceGeometryModel;
import com.metsakuur.ufacedetector.model.UFaceProcessingMode;
import com.metsakuur.ufacedetector.model.UFaceResult;
import com.metsakuur.ufacedetector.model.UFaceStateModel;
import com.metsakuur.ufacedetector.util.UFaceBitmapUtils;
import com.metsakuur.ufacetotpclient.UFaceTotpClient;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CameraActivity extends BaseAppCompatActivity implements UFaceDetectorListener {
    public static final String TAG = "TAG_CameraActivity";

    private ActivityCameraBinding binding;

    private LoadingDialog loadingDialog;

    // 얼굴 검출기 UFaceDetector
    private UFaceDetector uFaceDetector = null;

    // 설정화면 -> 눈깜빡임 검출 활성화 여부
    Boolean isEyeBlinkEnabled = false;

    // 눈깜빡임 통과 여부, 기본값 성공 및 통과
    Boolean isEyeBlink= true;

    // 고개돌림 체크 방향
    int nowDirection = 0;
    Random random;

    // 현재 고개돌림 성공 횟수
    int direction_count = 0;

    // 고개돌림 체크 횟수, 기본값 비활성화 = 0
    int DIRECTION_MAX = 0;

    // 좌우 고개돌림 체크 완료
    boolean isYawFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 로딩바 초기화
        loadingDialog = new LoadingDialog(CameraActivity.this);

        // 설정 화면 환경변수 세팅
        // 눈깜빡임 체크 활성화 여부
        isEyeBlinkEnabled = getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).getBoolean(UFaceConfig.FACE_EYE_BLINK_ENABLED, false);
        // 고개 돌림 체크 활성화 여부
        boolean isYawRollEnabled = getSharedPreferences(UFaceConfig.SHARED_NAME, MODE_PRIVATE).getBoolean(UFaceConfig.FACE_YAWROLL_ENABLED, false);
//        Log.d(TAG, "blinkCheck = ${isEyeBlinkEnabled}, yawRollCheck = ${isYawRollEnabled}");

        // 깜빡임 검출 여부 사용 시, 깜빡임 검출을 위해 isEyeBlink false 로 세팅 (기본값 통과 = true)
        if(isEyeBlinkEnabled) isEyeBlink = false;

        // 고개 좌우 돌림 사용 시, 고개 돌림 카운트 세팅, 기본값 0 = 사용안함
        if(isYawRollEnabled) DIRECTION_MAX = 3;

        // 디렉터 초기화 호출
        initDetector();
    }

    /**
     * 디텍터 초기화.
     */
    private void initDetector() {
        random = new Random();

        // 디렉터 초기화
        uFaceDetector = new UFaceDetector();

        // 고개 돌림 사용 여부, 0 = 사용 안함
        if (DIRECTION_MAX == 0) {
            isYawFinish = true;
        } else {
            isYawFinish = false;
        }

        // 고개 돌림 횟수 초기화
        direction_count = 0;
        getDirection();

        // 카메라 프리뷰 세팅
        uFaceDetector.setPreviewView(binding.previewView);
        // 디텍터 리스너 세팅
        uFaceDetector.setFaceDetectorListener(this);
        // 눈깜빡임 사용 여부
        uFaceDetector.setUseEyeBlink(true);

        // 디텍터 초기화
        uFaceDetector.initDetector(this, "4F5A465276310081D4E821C20667AFC25185AB32A55A34110F5D7A927FD24F5DCECADF08C38D5B980C243C6D72724708F69129EA19EAB0B7");
    }

    /**
     * 뷰 초기화
     */
    void initView() {
        if(uFaceDetector == null) return;

        uFaceDetector.resumeDetector();

        // 고개 돌림 사용 여부, 기본값 0 = 사용 안함
        if (DIRECTION_MAX == 0) {
            isYawFinish = true;
        } else {
            isYawFinish = false;
        }

        // 눈 깜빡임 & 고개 돌림 뷰 유효성 검사
        validateViewCondition();

        // 고개 돌림 횟수 초기화
        direction_count = 0;
        // 고개 돌림 방향 랜덤 생성
        getDirection();

        // 바인딩 뷰 초기화
        binding.tvCameraText.setText(getString(R.string.camera_front));
        binding.ivFront.setImageResource(R.drawable.front_off);
        binding.ivLeft.setImageResource(R.drawable.left_off);
        binding.ivRight.setImageResource(R.drawable.right_off);
    }

    void validateViewCondition() {

        // 데모 시나리오 = 눈깜빡임 성공 이후 고개 돌림 체크
        // 눈깜빡임 성공 or 눈깜빡임 비활성화시 bypass (기본값 = true) -> 고개 돌림 뷰 유효성 검사
        if(isEyeBlink) {
            // 고개 돌림 성공 여부, 뷰 유효성 검사 (비활성화 시 기본값 = true)
            if(isYawFinish) {
                binding.layoutYaw.setVisibility(View.GONE);
            } else {
                binding.layoutYaw.setVisibility(View.VISIBLE);
            }
        } else {
            binding.layoutYaw.setVisibility(View.GONE);
        }
    }

    // 고개 돌림 방향 랜덤 생성
    void getDirection() {
        nowDirection = random.nextInt(2);
    }

    // 고개 돌림 시나리오 수행
    public void detectPitchYawRoll(float pitch, float yaw, float roll, @NonNull Bitmap bitmap) {
        Float ufaceYaw = yaw;

        // 고개 돌림 체크
        // 데모앱 시나리오 얼굴 좌우 돌림 사용 (특정 방향으로 이동 후 다시 중앙으로 돌어오는게 한 사이클)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 0 = 왼쪽
                if (nowDirection == 0) {
                    binding.tvCameraText.setText(getString(R.string.camera_turn));
                    binding.ivFront.setImageResource(R.drawable.front_off);
                    binding.ivLeft.setImageResource(R.drawable.left_on);
                    binding.ivRight.setImageResource(R.drawable.right_off);
                    if (ufaceYaw != null) {
                        if (ufaceYaw < -20) {
                            nowDirection = 2;
                        }
                    }
                }

                // 1 = 오른쪽
                else if (nowDirection == 1) {
                    binding.tvCameraText.setText(getString(R.string.camera_turn));
                    binding.ivFront.setImageResource(R.drawable.front_off);
                    binding.ivLeft.setImageResource(R.drawable.left_off);
                    binding.ivRight.setImageResource(R.drawable.right_on);
                    if (ufaceYaw != null) {
                        if (ufaceYaw > 20) {
                            nowDirection = 2;
                        }
                    }
                }

                // 2 = 중앙
                else if (nowDirection == 2) {
                    binding.tvCameraText.setText(getString(R.string.camera_front));
                    binding.ivFront.setImageResource(R.drawable.front_on);
                    binding.ivLeft.setImageResource(R.drawable.left_off);
                    binding.ivRight.setImageResource(R.drawable.right_off);

                    int absoluteValue = (int) Math.abs(ufaceYaw);
                    if (absoluteValue < 10) {

                        // 고개 돌림 한 사이클 성공, 성공 카운트 증가
                        direction_count++;
                        if (direction_count >= DIRECTION_MAX) {
                            isYawFinish = true;
                            // 고개돌림 모두 완료, 얼굴 검출 결과값 수신 하도록 변경
                            // *** processingMode 가 DEFAULT_MODE 여야 uFaceDetector result 리스너 결과값이 수신 됨 ***
                            uFaceDetector.setProcessingMode(UFaceProcessingMode.DEFAULT_MODE);
                        } else {
                            getDirection();
                        }
                    } else {

                    }
                }
            }
        });
    }

    // 얼굴 검출 UI 업데이트
    public void isDetectFace(Boolean isDetectFace) {
        if (isDetectFace) {
            binding.tvCameraText.setText(getString(R.string.camera_front));
            if(!isEyeBlink) {
                binding.tvCameraText.setText(getString(R.string.camera_eye_blink));
            }
            binding.ivGuide.setImageResource(R.drawable.face_guide_green);
        } else {
            binding.ivGuide.setImageResource(R.drawable.face_guide_red);
            initView();
        }
    }


    /**
     * ----------------------
     * UFaceDetector Listener
     * ----------------------
     */

    /**
     * 카메라 세팅 정상 완료 후 호출
     * 타이머가 필요할 경우 여기서 시작
     */
    @Override
    public void uFaceDetectorSetCameraSessionComplete() {
    }

    /**
     * 얼굴 검출 상태(ex: 얼굴 검출 여부, 블러 상태, etc)를 수신하는
     */
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
                break;

            /**
             * 얼굴이 너무 멀리있을 때 호출
             */
            case UFACE_STATE_FACE_SMALL:
                binding.tvCameraText.setText("조금 가까이 얼굴을 보여주세요.");
                break;

            /**
             * 얼굴이 너무 가까울 때 호출
             */
            case UFACE_STATE_FACE_LARGE:
                binding.tvCameraText.setText("조금 멀리 얼굴을 보여주세요.");
                break;

            /**
             * Blur 감지 되었을 때 호출
             */
            case UFACE_STATE_FACE_BLUR:
                binding.tvCameraText.setText("카메라를 정면으로 비추고 너무 어둡거나 역광이 있는 곳을 피해 주세요.\n전면 카메라 렌즈를 깨끗이 닦아주세요.");
                break;

            default:
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

        // 고개돌림 시나리오 수행 여부 상태값 체크
        if (!isEyeBlink || isYawFinish) {
            return;
        }

        Float pitch = faceGeometry.getPitch();
        Float yaw = faceGeometry.getYaw();
        Float roll = faceGeometry.getRoll();
        Bitmap bitmap = faceGeometry.getFullImage();

        // 수신받은 pitch, yaw, roll 값으로 고개 돌림 시나리오 수행
        detectPitchYawRoll(pitch, yaw, roll, bitmap);
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
        if(!isEyeBlinkEnabled || result.isEyeBlinked()) {
            // 눈깜빡임 체크 성공
            isEyeBlink = true;
            validateViewCondition();

            // 고개 돌림 성공 체크 (사용 안할 시 true가 기본값이므로 통과)
            if (isYawFinish) {
                loadingDialog.show();

                // 선행 조건 통과 이후 최종 서버 통신 프로세스 진행
                requestUntact(result);
            } else {
                // 해당 리스너로 결과괎이 수신되면 디텍터가 검출을 멈추기 때문에 resumeDetector를 호출해야 디텍터가 다시 동작 함
                // 고개 돌림 아직 진행 중이므로, 고개 돌림 프로세스 다시 진행하도록 processingMode.GEOMETRY_MODE 로 변경
                uFaceDetector.setProcessingMode(UFaceProcessingMode.GEOMETRY_MODE);
                uFaceDetector.resumeDetector();
            }


        } else {
            Log.d(TAG, "눈을 깜빡여 주세요.");
            uFaceDetector.resumeDetector();
        }
    }


    private void requestUntact(UFaceResult result) {
        loadingDialog.show();
        String uuid = UFaceConfig.getUUID(getBaseContext());
        Bitmap resultImg = result.getFullImage();

        // 서버 전송을 위한 이미지 변환(암호화 및 이미지 변환)
        String dataImg = UFaceTotpClient.INSTANCE.getEncryptedImage(uuid, UFaceTotpClient.TAG_TYPE_IMAGE, UFaceBitmapUtils.jpegData(resultImg, 90));

        // 서버 통신 예시 (API 목록은 서버 API 가이드 문서 확인)
        if(dataImg != null ) {
            if(UFaceConfig.getInstance().getPictureByteArray() != null) {
                byte[] idImg = UFaceConfig.getInstance().getPictureByteArray();

                UFaceApiManager.requestUntactApi(
                        new UFaceDataUntactRequest(
                                uuid,
                                UFaceConfig.getInstance().getIdKey(),
                                UFaceConfig.osType,
                                UFaceConfig.getInstance().getUntactType(),
                                "LI",
                                dataImg,
                                UFaceTotpClient.INSTANCE.getEncryptedImage(
                                        uuid,
                                        UFaceTotpClient.TAG_TYPE_IDIMAGE,
                                        idImg
                                ),
                                UFaceConfig.channel
                        ), new Callback<UFaceResultData>() {
                            @Override
                            public void onResponse(Call<UFaceResultData> call, Response<UFaceResultData> response) {
                                String code = response.body().getCode();
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                if (code.equals("OK")) {
                                    Intent intent = new Intent(getBaseContext(), CompleteActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    String errorCode = response.body().getezResponse().getResp_code();
                                    if (errorCode != null) {
                                        switch (errorCode) {
                                            case "20033":
                                            case "20034":
                                            case "20035":
                                            case "20036": {
                                                openAlertView(
                                                        "신분증 얼굴과 불일치합니다.\n다시 시도해주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent =
                                                                    new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            case "20004":
                                            case "20014": {
                                                openAlertView(
                                                        "스푸핑이 감지되었습니다.\n다시 시도해주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new
                                                                    Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            case "20007": {
                                                openAlertView(
                                                        "선글라스를 벗어주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            case "20008": {
                                                openAlertView(
                                                        "마스크를 벗어주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            case "20015": {
                                                openAlertView(
                                                        "눈을 크게 떠주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            case "40000":
                                            case "40001": {
                                                openAlertView(
                                                        "얼굴이 검출되지 않았습니다.\n다시 시도해주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }

                                            default: {
                                                openAlertView(
                                                        "얼굴 비교 검증에 실패했습니다.\n다시 시도해주세요.",
                                                        "오류코드 : " + errorCode,
                                                        "종료",
                                                        (dialog, which) -> {
                                                            Intent intent = new Intent(getBaseContext(), MainActivity.class);
                                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                            startActivity(intent);
                                                        },
                                                        "재시도",
                                                        (dialog, which) -> {
                                                            initView();
                                                            dialog.dismiss();
                                                        });
                                                break;
                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<UFaceResultData> call, Throwable t) {
                                loadingDialog.dismiss();
                                openAlertView("onFailure : " + t.getMessage(),  (dialog, which ) -> { finish(); });
                            }
                        }
                );
            }
        }
    }

    @Override protected void onStop() {
        super.onStop();
        if(uFaceDetector != null) {
            uFaceDetector.pauseDetector();
        }
    }

    @Override protected void onStart() {
        super.onStart();
        initView();
    }

    @Override protected void onDestroy() {
        super.onDestroy();

        if(uFaceDetector != null) {
            uFaceDetector.deinitDetector();
            if(uFaceDetector.getPreview() != null) {
                uFaceDetector.getPreview().setSurfaceProvider(null);
            }
            if(uFaceDetector.getCameraExecutor() != null) {
                uFaceDetector.getCameraExecutor().shutdown();
            }
            if(uFaceDetector.getCameraProvider() != null) {
                uFaceDetector.getCameraProvider().unbindAll();
            }
        }
    }
}