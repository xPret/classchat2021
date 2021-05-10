package es.deusto.mcu.classchat2021;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {

    private FirebaseAnalytics mFirebaseAnalytics;
    private Button mButtonStart;
    private Button mButtonAbout;
    private ImageView mImageViewMainIcon;
    private int aboutClicksCounter =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButtonStart = findViewById(R.id.b_start);
        mButtonAbout = findViewById(R.id.b_about);
        mImageViewMainIcon = findViewById(R.id.iv_main_icon);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        mButtonStart.setOnClickListener(v -> start("button"));
        mImageViewMainIcon.setOnClickListener(v -> start("image"));
        mButtonAbout.setOnClickListener(v -> about());
    }

    private void start(String source) {
        Bundle bundle = new Bundle();
        bundle.putString("source", source);
        mFirebaseAnalytics.logEvent("click_start", bundle);
    }

    private void about() {
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