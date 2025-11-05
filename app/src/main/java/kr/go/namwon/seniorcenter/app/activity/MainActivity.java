package kr.go.namwon.seniorcenter.app.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import kr.go.namwon.seniorcenter.app.databinding.ActivityMainBinding;
import kr.go.namwon.seniorcenter.app.util.JsBridge;
import kr.go.namwon.seniorcenter.app.util.JsBridgeInterface;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;

public class MainActivity extends BaseAppCompatActivity implements JsBridgeInterface {

    private ActivityMainBinding binding;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        webView = binding.webView;
        initToken();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.addJavascriptInterface(new JsBridge(this, this), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.swipeRefreshLayout.setRefreshing(false);
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
}