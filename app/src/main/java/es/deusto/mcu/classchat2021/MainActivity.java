package es.deusto.mcu.classchat2021;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final String MSG_DATA_KEY_ADD_TITLE = "addTitle";
    private static final String MSG_DATA_KEY_ADD_DESC = "addDesc";
    private static final String MSG_DATA_KEY_ADD_IMAGE_URL = "addImgUrl";
    private FirebaseAnalytics mFirebaseAnalytics;
    private Button mButtonStart;
    private Button mButtonAbout;
    private ImageView mImageViewMainIcon;
    private int aboutClicksCounter =0;

    private TextView tvAddTitle;
    private TextView tvAddDescription;
    private ImageView ivAddImage;
    private ConstraintLayout addLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonStart = findViewById(R.id.b_start);
        mButtonAbout = findViewById(R.id.b_about);

        tvAddTitle = findViewById(R.id.tv_add_title);
        tvAddDescription = findViewById(R.id.tv_add_desc);
        ivAddImage = findViewById(R.id.iv_add_img);
        addLayout = findViewById(R.id.l_add_container);

        mImageViewMainIcon = findViewById(R.id.iv_main_icon);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mButtonStart.setOnClickListener(v -> start("button"));
        mImageViewMainIcon.setOnClickListener(v -> start("image"));
        mButtonAbout.setOnClickListener(v -> about());
        checkFCMMessage(getIntent().getExtras());
    }

    private void checkFCMMessage(Bundle fcmMessageData) {
        if (null != fcmMessageData) {
            String addTitle = fcmMessageData.getString(MSG_DATA_KEY_ADD_TITLE);
            String addDesc = fcmMessageData.getString(MSG_DATA_KEY_ADD_DESC);
            String addImageUrl = fcmMessageData.getString(MSG_DATA_KEY_ADD_IMAGE_URL);
            Log.d(TAG, "checkFCMMessage: Title=" + addTitle);
            Log.d(TAG, "checkFCMMessage: Desc=" + addDesc);
            Log.d(TAG, "checkFCMMessage: ImageUrl=" + addImageUrl);
            if (addTitle != null) {
                showAdd(addTitle, addDesc, addImageUrl);
            }
        }
    }

    private void showAdd(String addTitle, String addDescription, String addImageUrl) {
        tvAddDescription.setText(addDescription);
        tvAddTitle.setText(addTitle);
        Glide.with(ivAddImage.getContext())
                .load(addImageUrl)
                .into(ivAddImage);
        addLayout.setOnClickListener(view -> addLayout.setVisibility(View.GONE));
        addLayout.setVisibility(View.VISIBLE);
    }

    private void start(String source) {
        Bundle bundle = new Bundle();
        bundle.putString("source", source);
        mFirebaseAnalytics.logEvent("click_start", bundle);
        ChatActivity.startActivity(this);
        finish();
    }

    private void about() {
        ClassChatFCMService.printToken();
        mFirebaseAnalytics.logEvent("click_about", null);
        aboutClicksCounter ++;
        if (aboutClicksCounter >= 3) {
            registerUserAsInterested();
            aboutClicksCounter = 0;
        }
    }

    private void registerUserAsInterested() {
        mFirebaseAnalytics.setUserProperty("interested", "high");
        Toast.makeText(getBaseContext(),
                "Gracias por interesarte tanto", Toast.LENGTH_LONG).show();
    }
}