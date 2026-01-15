package kr.go.namwon.seniorcenter.app.util;

public interface JsBridgeInterface {
    void logout();
    void registerFace();

    void tokenUpdated(String accessToken);

    void tokenExpired();
}
