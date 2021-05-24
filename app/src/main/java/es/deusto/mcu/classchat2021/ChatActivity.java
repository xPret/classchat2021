package es.deusto.mcu.classchat2021;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

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

import es.deusto.mcu.classchat2021.firebase.auth.PrivateActivity;
import es.deusto.mcu.classchat2021.firebase.data.CloudStorage;
import es.deusto.mcu.classchat2021.firebase.data.RealtimeDatabase;
import es.deusto.mcu.classchat2021.model.ChatMessage;


public class ChatActivity extends PrivateActivity {

    private static final String TAG = ChatActivity.class.getName();
    private static final int REQUEST_IMAGE = 0;

    private RealtimeDatabase mRealtimeDB;
    private RealtimeDatabase.RoomListener mRoomListener;
    private CloudStorage mCloudStorage;


    private TextView mTextViewUsername;
    private TextView mTextViewUserEmail;
    private ImageView mImageViewUserPhoto;
    private Button mButtonSignOut;
    private FloatingActionButton fab;
    private EditText mEditTextMessage;
    private TextView mTextViewRoomTitle;

    private Map<String, ChatMessage> mChatMessagesMap;
    private List<ChatMessage> mChatMessagesList;
    private MessageAdapter messageAdapter;
    private RecyclerView mMessagesRecycler;

    private ImageButton mButtonAddImage;
    private Uri mImageMessageUri = null;


    public static void startActivity(AppCompatActivity caller) {
        caller.startActivity(new Intent(caller, ChatActivity.class));
    }

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

        mTextViewUsername.setText(getUserDisplayName());
        mTextViewUserEmail.setText(getUserEmail());
            if (getUserPhotoUrl() != null){
                Glide.with(mImageViewUserPhoto.getContext())
                        .load(getUserPhotoUrl().toString())
                        .into(mImageViewUserPhoto);
            }

        mRealtimeDB = new RealtimeDatabase();
        mRealtimeDB.getRoomName(new RealtimeDatabase.RoomNameListener() {
            @Override
            public void onRoomNameReceived(String roomName) {
                mTextViewRoomTitle.setText(roomName);
            }
        });

        mRoomListener = new RealtimeDatabase.RoomListener() {
            @Override
            public void onMessageCreated(ChatMessage message) {
                mChatMessagesMap.put(message.getId(), message);
                mChatMessagesList.add(message);
                messageAdapter.notifyDataSetChanged();
                mMessagesRecycler.smoothScrollToPosition(messageAdapter.getItemCount()-1);
            }

            @Override
            public void onMessageUpdated(ChatMessage message) {
                if (mChatMessagesMap.containsKey(message.getId())) {
                    ChatMessage messageToUpdate = mChatMessagesMap.get(message.getId());
                    if (messageToUpdate != null) {
                        messageToUpdate.setMessageText(message.getMessageText());
                        messageToUpdate.setSenderName(message.getSenderName());
                        messageToUpdate.setSenderAvatarURL(message.getSenderAvatarURL());
                        messageToUpdate.setMessageImageURL(message.getMessageImageURL());
                        messageAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onMessageRemoved(String messageId) {
                if (mChatMessagesMap.containsKey(messageId)) {
                    ChatMessage messageToRemove = mChatMessagesMap.get(messageId);
                    mChatMessagesMap.remove(messageId);
                    mChatMessagesList.remove(messageToRemove);
                    messageAdapter.notifyDataSetChanged();
                }
            }
        };

        mCloudStorage = new CloudStorage();
    }


    private void sendMessage() {
        String message = mEditTextMessage.getText().toString();
        if (!message.equals("")) {

            ChatMessage newMessage = new ChatMessage(
                    message,
                    getUserDisplayName(),
                    getUserPhotoUrl().toString());

            mRealtimeDB.addNewMessage(newMessage, new RealtimeDatabase.MessageTransactionListener() {
                @Override
                public void onStart() {
                    Snackbar.make(fab, "Sending...", Snackbar.LENGTH_LONG)
                            .setAction("Send", null).show();
                    fab.hide();
                    mEditTextMessage.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onCompleted() {
                    fab.show();
                    mEditTextMessage.setVisibility(View.VISIBLE);
                }

                @Override
                public void onSuccess(String newMessageId) {
                    Toast.makeText(getApplicationContext(),
                            "Message sent",Toast.LENGTH_LONG).show();
                    mEditTextMessage.setText("");
                    if (mImageMessageUri != null) {
                        mCloudStorage.uploadImage(getUserUid(), newMessageId, mImageMessageUri,
                                new CloudStorage.UploadImageListener() {
                                    @Override
                                    public void onStart(String imageRef) {
                                        Log.d(TAG, "Image uploading to " + imageRef);
                                    }

                                    @Override
                                    public void onProgress(long transferredBytes, long totalBytes) {
                                        double p = (100.0 * transferredBytes) / totalBytes;
                                        Log.i(TAG, "Upload is " + p + "% done");
                                    }

                                    @Override
                                    public void onCompleted() {

                                    }

                                    @Override
                                    public void onSuccess(String imageRef) {
                                        mRealtimeDB.addImageToMessage(newMessageId, imageRef, null);
                                        Log.w(TAG, "Upload successful: " + imageRef);
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e(TAG, error);
                                    }
                                });
                        mButtonAddImage.setImageResource(R.drawable.ic_action_add_img);
                        mImageMessageUri = null;
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getApplicationContext(),
                            error,
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mChatMessagesList.clear();
        mChatMessagesMap.clear();
        mRealtimeDB.registerRoomListener(mRoomListener);
    }

    @Override
    protected void onPause() {
        mRealtimeDB.unregisterRoomListener();
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