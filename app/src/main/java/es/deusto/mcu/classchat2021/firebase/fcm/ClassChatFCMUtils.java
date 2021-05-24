package es.deusto.mcu.classchat2021.firebase.fcm;

import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Map;

import es.deusto.mcu.classchat2021.model.Ad;

public class ClassChatFCMUtils {

    private static final String TAG = ClassChatFCMUtils.class.getName();

    private static final String MSG_DATA_KEY_AD_TITLE = "addTitle";
    private static final String MSG_DATA_KEY_AD_DESC = "addDesc";
    private static final String MSG_DATA_KEY_AD_IMAGE_URL = "addImgUrl";

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

    public static Ad getAdFromMessageData(Map<String, String> md) {
        if (!md.containsKey(MSG_DATA_KEY_AD_TITLE)) return null;
        return new Ad(
                md.get(MSG_DATA_KEY_AD_TITLE),
                md.get(MSG_DATA_KEY_AD_DESC),
                md.get(MSG_DATA_KEY_AD_IMAGE_URL));
    }

    public static Ad getAdFromMessageData(Bundle bundle) {
        if (bundle.getString(MSG_DATA_KEY_AD_TITLE) == null) return null;
        return new Ad(
                bundle.getString(MSG_DATA_KEY_AD_TITLE),
                bundle.getString(MSG_DATA_KEY_AD_DESC),
                bundle.getString(MSG_DATA_KEY_AD_IMAGE_URL));
    }
}
