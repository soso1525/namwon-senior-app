package kr.go.namwon.seniorcenter.app.util;

import android.graphics.Bitmap;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageUtil {
    public static Bitmap resize(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / height;

        if (ratio > 1) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // JPEG로 변경 + 품질 70~80 추천
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);

        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }
}
