package kr.go.namwon.seniorcenter.app.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import kr.go.namwon.seniorcenter.app.R;
import kr.go.namwon.seniorcenter.app.activity.MainActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = getClass().getSimpleName();
    private final int NOTI_ID = 1001;

    public static final String ACTION_FCM_TO_UI = "app.FCM_TO_UI";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "FCM service created.");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed FCM token: " + token);
        // 필요 시 서버에 토큰을 전송하는 로직을 여기에 추가하세요.r
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        String title = "";
        String body = "";
        String link = "";

        // 1. 알림 데이터가 있으면 우선 사용
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }
        // 2. 알림 데이터가 없고 'data' 페이로드만 있다면 그것을 사용
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
            link = remoteMessage.getData().get("link");
        }

        // 최종적으로 한 번만 호출
//        if (!body.isEmpty()) {
//            sendNotification(title, body);
//        }

        if (!link.isEmpty()) {
            Intent i = new Intent(ACTION_FCM_TO_UI);
            i.setPackage(getPackageName());          // 앱 내부로만 전달
            i.putExtra("title", title);
            i.putExtra("body", body);
            i.putExtra("link", link);
            sendBroadcast(i);
        }
    }

    private void sendNotification(String title, String messageBody) {
        // 알림 클릭 시 이동할 액티비티 설정 (예: MainActivity)
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
//                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0(Oreo) 이상을 위한 알림 채널 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "General Notifications",
                    NotificationManager.IMPORTANCE_HIGH); // 중요도를 높여야 팝업이 뜹니다.
            notificationManager.createNotificationChannel(channel);
        }

        // 알림 생성
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher) // 앱 아이콘 설정 필요
                        .setContentTitle(title != null ? title : "FCM Message")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // 팝업(Heads-up) 알림 설정

        // 알림 표시 (ID를 다르게 주면 여러 개의 알림을 쌓을 수 있음, id가 같으면 알림이 하나로만 보임.)
        notificationManager.cancel(NOTI_ID);
        notificationManager.notify(NOTI_ID, notificationBuilder.build());
    }
}