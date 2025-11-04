package com.metsakuur.lemondemo.retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

interface UFaceRetrofiInterface {
    @POST("/v1/api/untact")
    Call<UFaceResultData> requestUntactApi(@Body UFaceDataUntactRequest data);
}