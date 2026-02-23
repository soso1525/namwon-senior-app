package kr.go.namwon.seniorcenter.app.retrofit;

import com.google.gson.JsonObject;

import java.util.Map;

import kr.go.namwon.seniorcenter.app.model.CenterResponse;
import kr.go.namwon.seniorcenter.app.model.SignUpRequest;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface MemberApi {
    @Headers("Content-Type: application/json")
    @GET("v1/ext/centers")
    Call<CenterResponse> getCenterList();

    @Headers("Content-Type: application/json")
    @POST("v1/member/ext/signup")
    Call<JsonObject> join(@Body SignUpRequest signUpRequest);

    @Headers("Content-Type: application/json")
    @POST("v1/member/saveDvcToken")
    Call<Integer> registerFcmToken(@Body Map<String, String> body);

    @Headers("Content-Type: application/json")
    @POST("v1/member/deleteDvcToken")
    Call<Integer> unregisterFcmToken();
}