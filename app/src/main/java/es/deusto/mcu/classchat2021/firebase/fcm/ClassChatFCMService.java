package es.deusto.mcu.classchat2021.firebase.fcm;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import es.deusto.mcu.classchat2021.firebase.data.RealtimeDatabase;

public class ClassChatFCMService extends FirebaseMessagingService {

    private static final String TAG = ClassChatFCMService.class.getName();

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        RemoteMessage.Notification msgNotif =  remoteMessage.getNotification();
        if (msgNotif != null) {
            Log.d(TAG, "FCM Not. Msg.: Title=" + msgNotif.getTitle());
            Log.d(TAG, "FCM Not. Msg.: Text=" + msgNotif.getBody());
            Log.d(TAG, "FCM Not. Msg.: ImgURL=" + msgNotif.getImageUrl());
        }
        Log.d(TAG, "onMessageReceived: ad=" +
                ClassChatFCMUtils.getAdFromMessageData(remoteMessage.getData()));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "onNewToken: token=" + token);
        String deviceId = "Dev_" + System.currentTimeMillis();
        new RealtimeDatabase().addNewToken(deviceId, token);
    }

}