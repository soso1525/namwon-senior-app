package kr.go.namwon.seniorcenter.app.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import kr.go.namwon.seniorcenter.app.databinding.ActivityMainBinding;
import kr.go.namwon.seniorcenter.app.retrofit.ApiClient;
import kr.go.namwon.seniorcenter.app.util.JsBridge;
import kr.go.namwon.seniorcenter.app.util.JsBridgeInterface;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseAppCompatActivity implements JsBridgeInterface {

    private static final String TAG = "TAG_MainActivity";
    private ActivityMainBinding binding;
    private WebView webView;
    private String accessToken;
    private String refreshToken;

    private static final String[] PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    // ★ WebView 권한 요청 대기 핸들
    private PermissionRequest pendingMediaPermissionRequest;
    private String[] pendingMediaResources;

    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accessToken = getIntent().getStringExtra("accessToken");
        refreshToken = getIntent().getStringExtra("refreshToken");

        initPermissionLauncher();
        requestLocationAndMicIfNeeded(); // 최초 일괄 점검

        webView = binding.webView;

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);

        webView.addJavascriptInterface(new JsBridge(this, this), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebViewConsole", cm.message());
                return super.onConsoleMessage(cm);
            }

            // ★ getUserMedia 권한 처리 (카메라/마이크)
            @Override
            public void onPermissionRequest(final PermissionRequest request) { // WebView에서 카메라/마이크 권한 요청 들어오는 함수
                runOnUiThread(() -> {
                    String[] resources = request.getResources();
                    boolean needsMic = false, needsCam = false;

                    for (String res : resources) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res)) needsMic = true;
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res)) needsCam = true;
                    }

                    boolean hasMic = has(Manifest.permission.RECORD_AUDIO);
                    boolean hasCam = has(Manifest.permission.CAMERA);

                    List<String> toAsk = new ArrayList<>();
                    if (needsMic && !hasMic) toAsk.add(Manifest.permission.RECORD_AUDIO);
                    if (needsCam && !hasCam) toAsk.add(Manifest.permission.CAMERA);

                    if (!toAsk.isEmpty()) {
                        // 런타임 권한 먼저 요청 → 콜백에서 grant/deny 처리
                        pendingMediaPermissionRequest = request;
                        pendingMediaResources = resources;
                        permissionLauncher.launch(toAsk.toArray(new String[0]));
                        return;
                    }

                    // 이미 권한 보유 → 즉시 grant
                    List<String> allow = new ArrayList<>();
                    for (String res : resources) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && hasMic)
                            allow.add(res);
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && hasCam)
                            allow.add(res);
                    }
                    if (!allow.isEmpty()) request.grant(allow.toArray(new String[0]));
                    else request.deny();
                });
            }

            // ★ HTML5 Geolocation 권한 처리
            @Override
            public void onGeolocationPermissionsShowPrompt( // WebView에서 위치 권한 요청 들어오는 함수
                                                            String origin, GeolocationPermissions.Callback callback
            ) {
                boolean fine = has(Manifest.permission.ACCESS_FINE_LOCATION);
                boolean coarse = has(Manifest.permission.ACCESS_COARSE_LOCATION);

                if (fine || coarse) {
                    callback.invoke(origin, true, false);
                } else {
                    // 런타임 권한 먼저 요청 → 콜백에서 invoke 처리
                    pendingGeoCallback = callback;
                    pendingGeoOrigin = origin;
                    permissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                }
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                Log.d(TAG, "onPermissionRequestCanceled: " + request);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.swipeRefreshLayout.setRefreshing(false);

                if (url.contains("/home/uaHome")) {
                    initToken();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                if (url.contains("https://www.barodoctor.com/")) {
                    view.loadUrl("https://bit-senior.netlify.app/home/uaHome/");
                    return true;
                }

                return false;
            }
        });

        webView.loadUrl("https://bit-senior.netlify.app/home/uaHome/");
//        webView.loadUrl("https://smart-sc-senior-front.vercel.app/login");
        binding.swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private boolean has(String perm) {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    private void initPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // 일부 단말/경로에서 result map이 비어있을 수 있으므로, 최종 상태는 직접 재확인
                    boolean cam = has(Manifest.permission.CAMERA);
                    boolean mic = has(Manifest.permission.RECORD_AUDIO);
                    boolean fine = has(Manifest.permission.ACCESS_FINE_LOCATION);
                    boolean coarse = has(Manifest.permission.ACCESS_COARSE_LOCATION);

//                    dumpPermissions(); // 디버깅용

                    // ★ 대기 중인 getUserMedia 처리
                    if (pendingMediaPermissionRequest != null && pendingMediaResources != null) {
                        List<String> allow = new ArrayList<>();
                        for (String res : pendingMediaResources) {
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res) && mic) {
                                allow.add(res);
                            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res) && cam) {
                                allow.add(res);
                            }
                        }

                        if (!allow.isEmpty()) {
                            pendingMediaPermissionRequest.grant(allow.toArray(new String[0]));
                        } else {
                            pendingMediaPermissionRequest.deny();
                        }

                        pendingMediaPermissionRequest = null;
                        pendingMediaResources = null;
                    }

                    // ★ 대기 중인 Geolocation 처리
                    if (pendingGeoCallback != null && pendingGeoOrigin != null) {
                        boolean hasLocation = fine || coarse;
                        pendingGeoCallback.invoke(pendingGeoOrigin, hasLocation, false);
                        pendingGeoCallback = null;
                        pendingGeoOrigin = null;
                    }

                    // 안내 로그
                    if ((fine || coarse) && mic && cam) {
                        onPermissionsGranted();
                    } else {
                        onPermissionsDenied();
                    }
                }
        );
    }

    private void dumpPermissions() {
        String[] perms = {
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        for (String p : perms) {
            int s = ContextCompat.checkSelfPermission(this, p);
            Log.d("PermDump", p + " = " + (s == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }
    }

    public void requestLocationAndMicIfNeeded() {
        List<String> need = new ArrayList<>();
        for (String p : PERMS) {
            if (!has(p)) need.add(p);
        }
        if (!need.isEmpty()) {
            permissionLauncher.launch(need.toArray(new String[0]));
        } else {
            onPermissionsGranted();
        }
    }

    private void onPermissionsGranted() {
        Log.d(TAG, "카메라/마이크/위치 권한 허용됨.");
    }

    private void onPermissionsDenied() {
        new AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("서비스를 사용하려면 카메라, 마이크, 위치 권한이 필요합니다.\n설정에서 권한을 허용해주세요.")
                .setPositiveButton("설정 열기", (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void initToken() {
        String js = "localStorage.setItem('logintool', 'basic');"
                + "localStorage.setItem('userJwt', '" + accessToken + "');"
                + "localStorage.setItem('refreshJwt', '" + refreshToken + "');";

        webView.evaluateJavascript(js, null);
    }

    @Override
    public void logout() {
        openAlertView("로그아웃 하시겠습니까?",
                null,
                "취소",
                (dialogInterface, i) -> dialogInterface.dismiss(),
                "로그아웃", (dialogInterface, i) -> {
                    PrefsHelper.clear();
                    String js = "localStorage.setItem('logintool', 'basic');"
                            + "localStorage.setItem('userJwt', '');"
                            + "localStorage.setItem('refreshJwt', '');";
                    webView.evaluateJavascript(js, null);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                });
    }

    @Override
    public void registerFace() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    @Override
    public void updateToken(String accessToken) {
        PrefsHelper.putString("accessToken", accessToken);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // ★ pending 정리
        pendingMediaPermissionRequest = null;
        pendingMediaResources = null;
        pendingGeoCallback = null;
        pendingGeoOrigin = null;
    }
}
