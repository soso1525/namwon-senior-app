package kr.go.namwon.seniorcenter.app.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
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
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kr.go.namwon.seniorcenter.app.AppConfig;
import kr.go.namwon.seniorcenter.app.databinding.ActivityMainBinding;
import kr.go.namwon.seniorcenter.app.util.JsBridge;
import kr.go.namwon.seniorcenter.app.util.JsBridgeInterface;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;

public class MainActivity extends BaseAppCompatActivity implements JsBridgeInterface {

    private static final String TAG = "TAG_MainActivity";
    private ActivityMainBinding binding;
    private WebView webView;
    private String accessToken;
    private String refreshToken;

    private static final String[] PERMS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    // ★ WebView 권한 요청 대기 핸들
    private PermissionRequest pendingMediaPermissionRequest;
    private String[] pendingMediaResources;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        accessToken = getIntent().getStringExtra(AppConfig.tokenAccessKey());
        refreshToken = getIntent().getStringExtra(AppConfig.tokenRefreshKey());

        PrefsHelper.putString(AppConfig.tokenAccessKey(), accessToken);

        initPermissionLauncher();
        requestMicIfNeeded(); // 최초 일괄 점검

        webView = binding.webView;

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        webView.addJavascriptInterface(new JsBridge(this, this), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("WebViewConsole", cm.message());
                return super.onConsoleMessage(cm);
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(view.getContext())
                        .setMessage(message)
                        .setPositiveButton("OK", (d, w) -> result.confirm())
                        .setCancelable(false)
                        .show();
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                // 팝업용 WebView 만들기
                WebView popupWebView = new WebView(view.getContext());

                WebSettings ps = popupWebView.getSettings();
                ps.setJavaScriptEnabled(true);
                ps.setDomStorageEnabled(true);
                ps.setJavaScriptCanOpenWindowsAutomatically(true);
                ps.setSupportMultipleWindows(true);

                // 팝업을 Dialog로 표시
                final Dialog dialog = new Dialog(view.getContext());
                dialog.setContentView(popupWebView);
                dialog.setCancelable(true);
                dialog.show();

                // 팝업 내부 링크 처리
                popupWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        return handleExternalUrl(view, url);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return handleExternalUrl(view, url);
                    }
                });


                // 팝업 닫기(window.close) 처리
                popupWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        try {
                            window.destroy();
                        } catch (Exception ignored) {
                        }
                        dialog.dismiss();
                    }
                });

                // ⭐️ window.open으로 생성된 WebView를 시스템에 연결 (핵심)
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popupWebView);
                resultMsg.sendToTarget();

                return true;
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

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                Log.d(TAG, "onPermissionRequestCanceled: " + request);
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                if (url.contains("/home/uaHome")) {
                    initToken();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;

                String url = request.getUrl().toString();

                // ✅ 1) intent:// / market:// / 커스텀스킴 처리
                if (handleExternalUrl(view, url)) return true;

                // ✅ 2) 기존 로직 유지
                if (url.contains("https://www.barodoctor.com/")) {
                    view.loadUrl(AppConfig.frontURL());
                    return true;
                }

                return false;
            }

            // (구형 단말용)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (handleExternalUrl(view, url)) return true;

                if (url.contains("https://www.barodoctor.com/")) {
                    view.loadUrl(AppConfig.frontURL());
                    return true;
                }

                return false;
            }
        });


//        Set<String> allowedOriginRules = new HashSet<>();
//        allowedOriginRules.add("https://namwon-senior-web.netlify.app");
//        allowedOriginRules.add("https://ecare.namwon.go.kr");
//
//        String js = "localStorage.setItem('logintool', 'basic');"
//                + "localStorage.setItem('userJwt', '" + accessToken + "');"
//                + "localStorage.setItem('refreshJwt', '" + refreshToken + "');";
//
//
//        WebViewCompat.addDocumentStartJavaScript(
//                webView, js, allowedOriginRules
//        );

        webView.loadUrl(AppConfig.frontURL());
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

    private boolean handleExternalUrl(WebView view, String url) {
        try {
            if (url.startsWith("intent://")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                // package 없으면 강제 지정(너가 확인한 값)
                if (intent.getPackage() == null) {
                    intent.setPackage("zone.cloudboda");
                }

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    // ✅ 1) 일단 실행부터!
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException notFound) {
                    // ✅ 2) 진짜 없을 때만 fallback
                    String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                    if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                        view.loadUrl(fallbackUrl);
                        return true;
                    }

                    String pkg = intent.getPackage();
                    if (pkg != null && !pkg.isEmpty()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        return true;
                    }

                    return true;
                }
            }

            if (url.startsWith("market://")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "handleExternalUrl error: " + url, e);
            return true;
        }
        return false;
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

                    // 안내 로그
                    if (mic && cam) {
                        onPermissionsGranted();
                    } else {
                        onPermissionsDenied();
                    }
                }
        );
    }

    public void requestMicIfNeeded() {
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
        Log.d(TAG, "카메라/마이크 권한 허용됨.");
    }

    private void onPermissionsDenied() {
        new AlertDialog.Builder(this)
                .setTitle("권한 필요")
                .setMessage("서비스를 사용하려면 카메라, 마이크 권한이 필요합니다.\n설정에서 권한을 허용해주세요.")
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
                    String js = "localStorage.setItem('logintool', 'basic');"
                            + "localStorage.setItem('userJwt', '');"
                            + "localStorage.setItem('refreshJwt', '');";

                    webView.evaluateJavascript(js, null);

                    Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

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
    public void tokenUpdated(String accessToken) {
        PrefsHelper.putString(AppConfig.tokenAccessKey(), accessToken);
    }

    @Override
    public void tokenExpired() {
        Toast.makeText(this, "세션이 만료되었습니다. 다시 로그인해주세요", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pendingMediaPermissionRequest = null;
        pendingMediaResources = null;
    }
}
