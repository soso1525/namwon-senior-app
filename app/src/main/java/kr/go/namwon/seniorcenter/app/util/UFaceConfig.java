package kr.go.namwon.seniorcenter.app.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.Settings;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UFaceConfig {

    private static final UFaceConfig instance = new UFaceConfig();
    private UFaceConfig() {}
    public static UFaceConfig getInstance() {
        return instance;
    }

    private String idKey = "test";
    String untactType = "";
    Bitmap pictureBitmap = null;
    byte[] pictureByteArray = null;

    public String getIdKey() {
        return idKey;
    }

    public void setIdKey(String idKey) {
        this.idKey = idKey;
    }

    public String getUntactType() {
        return untactType;
    }

    public void setUntactType(String untactType) {
        this.untactType = untactType;
    }

    public Bitmap getPictureBitmap() {
        return pictureBitmap;
    }

    public void setPictureBitmap(Bitmap pictureBitmap) {
        this.pictureBitmap = pictureBitmap;
    }

    public byte[] getPictureByteArray() {
        return pictureByteArray;
    }

    public void setPictureByteArray(byte[] pictureByteArray) {
        this.pictureByteArray = pictureByteArray;
    }

    //server ip
    public static final String SERVER_IP = "https://fms.metsafr.com";
    //server port
    public static final String SERVER_PORT = ":28282";

    public static final String channel = "BMT";
    public static final String SHARED_NAME = "SHARED_NAME";
    public static final String osType = "AND";
    public static final String YAW_CHECK_COUNT = "YAW_CHECK_COUNT";
    public static final String FACE_COUNT = "FACE_COUNT";
    public static final String DELAY_TIME = "DELAY_TIME";
    public static final String FACE_EYE_BLINK_ENABLED = "FACE_EYE_BLINK_ENABLED";
    public static final String FACE_YAWROLL_ENABLED = "FACE_YAWROLL_ENABLED";

    public static String getUUID(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return sha256(androidId);
    }

    public static String sha256(String str) {
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            sh.update(str.getBytes());
            byte[] byteData = sh.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : byteData) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
}