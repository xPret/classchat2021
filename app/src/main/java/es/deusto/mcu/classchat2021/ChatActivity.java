package es.deusto.mcu.classchat2021;

import android.content.Intent;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChatActivity extends AppCompatActivity {

    private static final String DB_URL = "https://classchat-mcu2021-ivan-default-rtdb.europe-west1.firebasedatabase.app";
    private static final String ROOMS_CHILD = "rooms";
    private static final String ROOM_ID = "mcudeustoroomid";
    private static final String ROOM_NAME_CHILD = "roomName";
    private static final String MESSAGES_CHILD = "messages";
    private static final String TAG = ChatActivity.class.getName();
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;

    private DatabaseReference mFirebaseDatabaseRef;

    private TextView mTextViewUsername;
    private TextView mTextViewUserEmail;
    private ImageView mImageViewUserPhoto;
    private Button mButtonSignOut;
    private FloatingActionButton fab;
    private EditText mEditTextMessage;
    private TextView mTextViewRoomTitle;


    private DatabaseReference mMessagesRef;
    private ChildEventListener mMessagesChildEventListener;

    private Map<String, ChatMessage> mChatMessagesMap;
    private List<ChatMessage> mChatMessagesList;
    private MessageAdapter messageAdapter;
    private RecyclerView mMessagesRecycler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> sendMessage());

        mTextViewUsername = findViewById(R.id.tv_username);
        mTextViewUserEmail = findViewById(R.id.tv_useremail);
        mImageViewUserPhoto = findViewById(R.id.iv_userphoto);

        mButtonSignOut = findViewById(R.id.b_signout);
        mButtonSignOut.setOnClickListener(v -> signOut());

        mEditTextMessage = findViewById(R.id.et_message);
        mTextViewRoomTitle = findViewById(R.id.tv_room_name);

        mMessagesRecycler = findViewById(R.id.rv_messages);

        mChatMessagesMap = new HashMap<>();
        mChatMessagesList = new ArrayList<>();

        /** HARDCODED MESSAGES
        mChatMessagesList.add(new ChatMessage(
                "Winter is coming...",
                "Arya",
                "https://media.metrolatam.com/2019/04/29/capturadepantall-f748024b39daf7b0ca2e96a5a8922548-1200x600.jpg"));

        mChatMessagesList.add(new ChatMessage(
                "Siempre pago mis deudas!",
                "Tyrion",
                "https://upload.wikimedia.org/wikipedia/en/thumb/5/50/Tyrion_Lannister-Peter_Dinklage.jpg/220px-Tyrion_Lannister-Peter_Dinklage.jpg"));
        //*/

        messageAdapter = new MessageAdapter(mChatMessagesList);

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessagesRecycler.setLayoutManager(mLinearLayoutManager);
        mMessagesRecycler.setAdapter(messageAdapter);


        initFirebaseAuth();
        initGoogleApiClient();
        if (mFirebaseUser != null) {
            mTextViewUsername.setText(mFirebaseUser.getDisplayName());
            mTextViewUserEmail.setText(mFirebaseUser.getEmail());
            if (mFirebaseUser.getPhotoUrl() != null){
                Glide.with(mImageViewUserPhoto.getContext())
                        .load(mFirebaseUser.getPhotoUrl().toString())
                        .into(mImageViewUserPhoto);
            }
        }
        initFirebaseDatabaseReference();
        initFirebaseDatabaseRoomNameRefListener();
        initFirebaseDatabaseMessageRefListener();
    }

    private void initFirebaseAuth() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            SignInActivity.startActivity(this);
            finish();
        }
    }

    private void initGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, null)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
    }

    private void signOut() {
        mFirebaseAuth.signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        startActivity(new Intent(this, SignInActivity.class));
        finish();
    }

    public static void startActivity(AppCompatActivity caller) {
        caller.startActivity(new Intent(caller, ChatActivity.class));
    }

    private void initFirebaseDatabaseReference() {
        mFirebaseDatabaseRef = FirebaseDatabase.getInstance(DB_URL).getReference();
    }

    private void sendMessage() {
        String message = mEditTextMessage.getText().toString();
        if (!message.equals("")) {
            Snackbar.make(fab, "Sending...", Snackbar.LENGTH_LONG)
                    .setAction("Send", null).show();
            fab.hide();
            mEditTextMessage.setVisibility(View.INVISIBLE);

            ChatMessage newMessage = new ChatMessage(
                    message,
                    mFirebaseUser.getDisplayName(),
                    mFirebaseUser.getPhotoUrl().toString());

            mFirebaseDatabaseRef.child(ROOMS_CHILD).child(ROOM_ID)
                    .child(MESSAGES_CHILD)
                    .push().setValue(newMessage)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mEditTextMessage.setText("");
                            Snackbar.make(fab, R.string.message_sent_ok,
                                    Snackbar.LENGTH_LONG).show();
                            fab.show();
                            mEditTextMessage.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(fab, R.string.message_sent_error,
                                    Snackbar.LENGTH_LONG).show();
                            fab.show();
                            mEditTextMessage.setVisibility(View.VISIBLE);
                        }
                    });
            ;
        }
    }

    private void initFirebaseDatabaseRoomNameRefListener() {
        DatabaseReference mRoomNameRef = mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(ROOM_NAME_CHILD);

        // Listen only one time addValueEventListener -> addListenerForSingleValueEvent
        mRoomNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mTextViewRoomTitle.setText(dataSnapshot.getValue().toString());
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void initFirebaseDatabaseMessageRefListener() {
        mMessagesRef = mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(MESSAGES_CHILD);
        mMessagesChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot,
                                     @Nullable String s) {
                Log.d(TAG, "onChildAdded:" + dataSnapshot.getKey());
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                mChatMessagesMap.put(dataSnapshot.getKey(), chatMessage);
                mChatMessagesList.add(chatMessage);
                messageAdapter.notifyDataSetChanged();
                mMessagesRecycler.smoothScrollToPosition(messageAdapter.getItemCount()-1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot,
                                       @Nullable String s) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());
                if (mChatMessagesMap.containsKey(dataSnapshot.getKey())) {
                    ChatMessage updatedMessage =
                            dataSnapshot.getValue(ChatMessage.class);
                    ChatMessage messageToUpdate =
                            mChatMessagesMap.get(dataSnapshot.getKey());
                    if (updatedMessage != null && messageToUpdate != null) {
                        messageToUpdate.setMessageText(updatedMessage.getMessageText());
                        messageToUpdate.setSenderName(updatedMessage.getSenderName());
                        messageToUpdate.setSenderAvatarURL(
                                updatedMessage.getSenderAvatarURL());
                        messageAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
                String key = dataSnapshot.getKey();
                if (mChatMessagesMap.containsKey(key)) {
                    ChatMessage messageToRemove = mChatMessagesMap.get(key);
                    mChatMessagesMap.remove(key);
                    mChatMessagesList.remove(messageToRemove);
                    messageAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot,
                                     @Nullable String s) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled:" + databaseError.getMessage());
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMessagesRef != null) {
            mChatMessagesList.clear();
            mChatMessagesMap.clear();
            mMessagesRef.addChildEventListener(mMessagesChildEventListener);
        }
    }

    @Override
    protected void onPause() {
        if (mMessagesRef != null) {
            mMessagesRef.removeEventListener(mMessagesChildEventListener);
        }
        super.onPause();
    }
}