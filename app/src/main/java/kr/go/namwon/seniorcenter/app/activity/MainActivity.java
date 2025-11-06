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
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import kr.go.namwon.seniorcenter.app.databinding.ActivityMainBinding;
import kr.go.namwon.seniorcenter.app.util.JsBridge;
import kr.go.namwon.seniorcenter.app.util.JsBridgeInterface;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;

public class MainActivity extends BaseAppCompatActivity implements JsBridgeInterface {

    private static final String TAG = "TAG_MainActivity";
    private ActivityMainBinding binding;
    private WebView webView;

    private static final String[] PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private ActivityResultLauncher<String[]> permissionLauncher;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initPermissionLauncher();
        requestLocationAndMicIfNeeded();

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
            @Override public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebViewConsole", cm.message());
                return super.onConsoleMessage(cm);
            }

            // ★ 마이크/카메라 허용 처리 (getUserMedia)
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> {
                    List<String> allow = new ArrayList<>();
                    for (String res : request.getResources()) {
                        if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(res)) {
                            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.RECORD_AUDIO)
                                    == PackageManager.PERMISSION_GRANTED) {
                                allow.add(res);
                            }
                        }
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(res)) {      // ✅ 카메라
                            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                allow.add(res);
                            }
                        }
                    }
                    if (!allow.isEmpty()) request.grant(allow.toArray(new String[0]));
                    else request.deny();
                });
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(
                    String origin, GeolocationPermissions.Callback callback
            ) {
                boolean hasLocation =
                        ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED
                                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED;

                callback.invoke(origin, hasLocation, false);
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

    private void initPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    boolean mic = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                    boolean camera = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));

                    if ((fine || coarse) && mic && camera) {
                        onPermissionsGranted();
                    } else {
                        onPermissionsDenied();
                    }
                }
        );
    }

    public void requestLocationAndMicIfNeeded() {
        List<String> need = new ArrayList<>();
        for (String p : PERMS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need.add(p);
            }
        }
        if (!need.isEmpty()) {
            permissionLauncher.launch(need.toArray(new String[0]));
        } else {
            onPermissionsGranted();
        }
    }

    private void onPermissionsGranted() {
        Log.d(TAG, "위치, 마이크 권한 허용 됨.");
    }

    private void onPermissionsDenied() {
        new AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("서비스를 사용하려면 위치와 마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.")
                .setPositiveButton("설정 열기", (d, w) -> {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    i.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(i);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void initToken() {
        String accessToken = PrefsHelper.getString("accessToken", "");
        String refreshToken = PrefsHelper.getString("refreshToken", "");

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
}