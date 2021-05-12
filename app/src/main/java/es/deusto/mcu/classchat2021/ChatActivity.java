package es.deusto.mcu.classchat2021;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.api.GoogleApiClient;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final int REQUEST_IMAGE = 0;
    private static final String FOLDER_CHAT_IMAGES = "/chat_images";
    private static final String MESSAGE_IMAGE_FIELD = "messageImageURL";
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;

    private DatabaseReference mFirebaseDatabaseRef;
    private StorageReference mFirebaseStorageRef;

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

    private ImageButton mButtonAddImage;
    private Uri mImageMessageUri = null;


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
        messageAdapter = new MessageAdapter(mChatMessagesList);

        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessagesRecycler.setLayoutManager(mLinearLayoutManager);
        mMessagesRecycler.setAdapter(messageAdapter);

        mButtonAddImage = findViewById(R.id.ib_add_image);
        mButtonAddImage.setOnClickListener(view -> {
            if (mImageMessageUri == null) {
                addImageToMessage();
            } else {
                mImageMessageUri = null;
                Snackbar.make(fab, "Image removed from message",
                        Snackbar.LENGTH_SHORT).setAction("ImgRem", null).show();
                mButtonAddImage.setImageResource(R.drawable.ic_action_add_img);
            }
        });

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
        initFirebaseCloudStorage();
    }

    private void initFirebaseCloudStorage() {
        mFirebaseStorageRef = FirebaseStorage.getInstance().getReference();
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
                    .push().setValue(newMessage, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError,
                                               @NonNull DatabaseReference databaseReference) {
                            fab.show();
                            mEditTextMessage.setVisibility(View.VISIBLE);
                            if (databaseError == null) {
                                Toast.makeText(getApplicationContext(),
                                        "Message sent",Toast.LENGTH_LONG).show();
                                mEditTextMessage.setText("");
                                if (mImageMessageUri != null) {
                                    String key = databaseReference.getKey();
                                    StorageReference newImageRef =
                                            mFirebaseStorageRef.child(FOLDER_CHAT_IMAGES)
                                                    .child(mFirebaseUser.getUid())
                                                    .child(key);
                                    putImageInStorage(newImageRef, mImageMessageUri, key);
                                    mButtonAddImage.setImageResource(R.drawable.ic_action_add_img);
                                    mImageMessageUri = null;
                                }
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Error sending message",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void putImageInStorage(final StorageReference newImageRef,
                                  final Uri imageUri,
                                  final String messageKey) {
        Log.d(TAG, "Image uploading to " + newImageRef.toString());
        newImageRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double prog = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    Log.i(TAG, "Upload is " + prog + "% done");
                })
                .addOnCompleteListener(ChatActivity.this,
                        task -> {
                            if (task.isSuccessful()) {
                                mFirebaseDatabaseRef.child(ROOMS_CHILD)
                                        .child(ROOM_ID)
                                        .child(MESSAGES_CHILD)
                                        .child(messageKey)
                                        .child(MESSAGE_IMAGE_FIELD)
                                        .setValue(newImageRef.toString());
                                Log.w(TAG, "Upload successful: " +
                                        newImageRef.toString());
                            } else {
                                Log.e(TAG, "Image upload task was not successful.",
                                        task.getException());
                            }
                        });
    }


    private void initFirebaseDatabaseRoomNameRefListener() {
        DatabaseReference mRoomNameRef = mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(ROOM_NAME_CHILD);

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
                        messageToUpdate.setMessageImageURL(
                                updatedMessage.getMessageImageURL()
                        );
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

    private void addImageToMessage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_IMAGE)
                && (resultCode == RESULT_OK) && (data != null)) {
            mImageMessageUri = data.getData();
            mButtonAddImage.setImageResource(R.drawable.ic_action_remove_img);
        }
    }
}