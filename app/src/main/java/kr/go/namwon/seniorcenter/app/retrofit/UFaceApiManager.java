package kr.go.namwon.seniorcenter.app.retrofit;

import retrofit2.Callback;
import retrofit2.Call;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UFaceApiManager {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void requestUntactApi(UFaceDataUntactRequest data, Callback<UFaceResultData> callback) {
        executorService.submit(() -> {
            Call<UFaceResultData> call = UFaceRetrofitClient.getRetrofitInterface().requestUntactApi(data);
            call.enqueue(callback);
        });
    }
}