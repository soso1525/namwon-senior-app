package kr.go.namwon.seniorcenter.app.retrofit;

import com.google.gson.JsonObject;

import kr.go.namwon.seniorcenter.app.model.FaceRegisterRequest;
import kr.go.namwon.seniorcenter.app.model.FaceVerifyRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("v1/auth/ext/face/verify")
    Call<JsonObject> verify(@Body FaceVerifyRequest request);

    @Headers("Content-Type: application/json")
    @POST("v1/auth/face/register")
    Call<JsonObject> register(@Body FaceRegisterRequest request);

    @Headers("Content-Type: application/json")
    @GET("v1/auth/face/status")
    Call<JsonObject> getFaceStatus();

    @FormUrlEncoded
    @POST("v1/auth/ext/loginWithIdPwd")
    Call<JsonObject> login(
            @Field("phoneNum") String phoneNum,
            @Field("password") String password
    );

    @Headers("Content-Type: application/json")
    @GET("/kakao/updateRefreshToken")
    Call<JsonObject> updateToken();
}