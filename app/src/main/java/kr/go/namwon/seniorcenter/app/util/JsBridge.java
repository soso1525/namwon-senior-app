package kr.go.namwon.seniorcenter.app.util;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class JsBridge {
    private final Context context;
    private JsBridgeInterface jsBridgeInterface;

    public JsBridge(Context context, JsBridgeInterface jsBridgeInterface) {
        this.context = context;
        this.jsBridgeInterface = jsBridgeInterface;
    }

    @JavascriptInterface
    public void updateToken(String accessToken) {
        jsBridgeInterface.updateToken(accessToken);
    }

    @JavascriptInterface
    public void registerFace() {
        jsBridgeInterface.registerFace();
    }

    @JavascriptInterface
    public void logout() {
        jsBridgeInterface.logout();
    }
}

