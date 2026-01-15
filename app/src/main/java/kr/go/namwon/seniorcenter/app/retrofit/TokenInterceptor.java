package kr.go.namwon.seniorcenter.app.retrofit;

import java.io.IOException;

import kr.go.namwon.seniorcenter.app.AppConfig;
import kr.go.namwon.seniorcenter.app.util.PrefsHelper;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header(AppConfig.tokenAccessKey(), PrefsHelper.getString(AppConfig.tokenAccessKey(), ""));
        Request request = builder.build();
        return chain.proceed(request);
    }
}