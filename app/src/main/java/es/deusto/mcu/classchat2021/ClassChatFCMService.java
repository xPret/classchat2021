package es.deusto.mcu.classchat2021;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ClassChatFCMService extends FirebaseMessagingService {

    private static final String TAG = ClassChatFCMService.class.getName();
    private static final String MSG_DATA_KEY_ADD_TITLE = "addTitle";
    private static final String MSG_DATA_KEY_ADD_DESC = "addDesc";
    private static final String MSG_DATA_KEY_ADD_IMAGE_URL = "addImgUrl";
    private static final String FCM_TOKENS = "fcmTokens";
    private static final String DB_URL = "https://classchat-mcu2021-ivan-default-rtdb.europe-west1.firebasedatabase.app";


    public static void printToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "getInstanceId failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "Current FCM token:" + token);
                });
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        RemoteMessage.Notification msgNotif =  remoteMessage
                .getNotification();
        if (msgNotif != null) {
            Log.d(TAG, "FCM Not. Msg.: Title=" + msgNotif.getTitle());
            Log.d(TAG, "FCM Not. Msg.: Text=" + msgNotif.getBody());
            Log.d(TAG, "FCM Not. Msg.: ImgURL=" + msgNotif.getImageUrl());
        }
        Map<String, String> msgData = remoteMessage.getData();
        if (msgData != null) {
            Log.d(TAG, "FCM Data Msg.: addTitle=" + msgData.get(MSG_DATA_KEY_ADD_TITLE));
            Log.d(TAG, "FCM Data Msg.: addDesc=" + msgData.get(MSG_DATA_KEY_ADD_DESC));
            Log.d(TAG, "FCM Data Msg.: addImageUrl=" + msgData.get(MSG_DATA_KEY_ADD_IMAGE_URL));
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "onNewToken: token=" + token);
        updateTokenInServer(token);
    }

    private void updateTokenInServer(String newToken) {
        String deviceId = "Dev_" + System.currentTimeMillis();
        FirebaseDatabase.getInstance(DB_URL).getReference()
                .child(FCM_TOKENS)
                .child(deviceId)
                .setValue(newToken);
    }
}