package kr.go.namwon.seniorcenter.app.model;

public class FaceRegisterRequest {
    private String image; // base64 문자열

    public FaceRegisterRequest(String image) {
        this.image = image;
    }
}