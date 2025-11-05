package kr.go.namwon.seniorcenter.app.retrofit;

import com.google.gson.JsonObject;

import kr.go.namwon.seniorcenter.app.model.FaceVerifyRequest;
import kr.go.namwon.seniorcenter.app.model.LoginRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("v1/auth/ext/face/verify")
    Call<JsonObject> verify(@Body FaceVerifyRequest request);

    @Headers("Content-Type: application/json")
    @POST("v1/auth/ext/loginWithIdPwd")
    Call<JsonObject> login(@Body LoginRequest request);
}