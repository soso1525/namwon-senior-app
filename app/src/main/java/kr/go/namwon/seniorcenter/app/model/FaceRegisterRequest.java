package kr.go.namwon.seniorcenter.app.model;

public class FaceRegisterRequest {
    private String token;
    private String image; // base64 문자열

    public FaceRegisterRequest(String token, String image) {
        this.token = token; this.image = image;
    }
}