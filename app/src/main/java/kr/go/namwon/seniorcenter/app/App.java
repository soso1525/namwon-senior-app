package kr.go.namwon.seniorcenter.app;

import android.app.Application;

import kr.go.namwon.seniorcenter.app.util.PrefsHelper;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        PrefsHelper.init(this);
    }
}
