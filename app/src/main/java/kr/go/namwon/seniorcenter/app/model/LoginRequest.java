package kr.go.namwon.seniorcenter.app.model;

public class LoginRequest {
    String phoneNum;
    String password;

    public LoginRequest(String phone, String password) {
         this.phoneNum = phone;
         this.password = password;
    }
}
