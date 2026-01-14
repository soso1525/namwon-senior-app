package kr.go.namwon.seniorcenter.app.model;

import com.google.gson.annotations.JsonAdapter;

import java.util.List;

public class CenterResponse {
    int code;
    int status;
    String message;
    List<Center> resultVO;

    public int getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<Center> getResultVO() {
        return resultVO;
    }
}
