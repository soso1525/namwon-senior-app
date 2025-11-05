package kr.go.namwon.seniorcenter.app.model;

public class FaceVerifyRequest {
    public String image; // base64 문자열

    public FaceVerifyRequest(String image) {
        this.image = image;
    }
}