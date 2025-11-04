package com.metsakuur.lemondemo.retrofit;


import com.google.gson.GsonBuilder;
import com.metsakuur.lemondemo.util.UFaceConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UFaceRetrofitClient {

    private static UFaceRetrofiInterface uFaceRetrofiInterface = null;
    private static Retrofit retrofit = null;

    private static final HttpLoggingInterceptor httpLoggingInterceptor =
            new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY);

    private static final OkHttpClient.Builder httpClient = TrustOkHttpClientUtil.getUnsafeOkHttpClient()
            .addInterceptor(httpLoggingInterceptor)
            .connectTimeout(5, TimeUnit.SECONDS);

    private UFaceRetrofitClient() {}

    private static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(UFaceConfig.getInstance().SERVER_IP + UFaceConfig.getInstance().SERVER_PORT)
                    .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create()))
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }

    public static UFaceRetrofiInterface getRetrofitInterface() {
        if (uFaceRetrofiInterface == null) {
            uFaceRetrofiInterface = getClient().create(UFaceRetrofiInterface.class);
        }
        return uFaceRetrofiInterface;
    }
}