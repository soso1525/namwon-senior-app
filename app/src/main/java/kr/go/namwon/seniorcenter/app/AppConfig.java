package kr.go.namwon.seniorcenter.app;

public class AppConfig {

    private AppConfig() {
    }

    public static String licenseKey() {
        return BuildConfig.LICENSE_KEY;
    }

    public static String baseURL() {
        return BuildConfig.BASE_URL;
    }

    public static String frontURL() {
        return BuildConfig.FRONT_URL;
    }

    public static String frontBaseURL() {
        return BuildConfig.FRONT_BASE_URL;
    }

    public static String tokenAccessKey() {
        return BuildConfig.TOKEN_ACCESS_KEY;
    }

    public static String tokenRefreshKey() {
        return BuildConfig.TOKEN_REFRESH_KEY;
    }
}
