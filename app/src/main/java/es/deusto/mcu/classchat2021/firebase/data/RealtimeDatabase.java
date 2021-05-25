package es.deusto.mcu.classchat2021.firebase.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import es.deusto.mcu.classchat2021.model.ChatMessage;

public class RealtimeDatabase {

    private static final String DB_URL = "https://classchat-mcu2021-ivan-default-rtdb.europe-west1.firebasedatabase.app";
    private static final String ROOMS_CHILD = "rooms";
    private static final String ROOM_ID = "mcudeustoroomid";
    private static final String ROOM_NAME_CHILD = "roomName";
    private static final String MESSAGES_CHILD = "messages";
    private static final String MESSAGE_IMAGE_FIELD = "messageImageURL";

    private static final String FCM_TOKENS = "fcmTokens";


    private final DatabaseReference mFirebaseDatabaseRef;
    private final DatabaseReference mMessagesRef;
    private ChildEventListener mRoomEventListener = null;

    public RealtimeDatabase() {
        mFirebaseDatabaseRef = FirebaseDatabase.getInstance(DB_URL).getReference();
        mMessagesRef = mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(MESSAGES_CHILD);
    }

    public interface RoomNameListener {
        void onRoomNameReceived(String roomName);
    }

    public void getRoomName(final RoomNameListener nameListener) {
        DatabaseReference mRoomNameRef = mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(ROOM_NAME_CHILD);
        mRoomNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nameListener.onRoomNameReceived(dataSnapshot.getValue(String.class));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }


    public interface RoomListener {
        void onMessageCreated(ChatMessage message);
        void onMessageUpdated(ChatMessage message);
        void onMessageRemoved(String messageId);
    }

    public void registerRoomListener(final RoomListener roomListener) {
        unregisterRoomListener();

        mRoomEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot,
                                     @Nullable String s) {
                ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                if (chatMessage != null) {
                    chatMessage.setId(dataSnapshot.getKey());
                    roomListener.onMessageCreated(chatMessage);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot,
                                       @Nullable String s) {
                ChatMessage updatedMessage = dataSnapshot.getValue(ChatMessage.class);
                if (updatedMessage != null) {
                    updatedMessage.setId(dataSnapshot.getKey());
                    roomListener.onMessageUpdated(updatedMessage);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                roomListener.onMessageRemoved(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot,
                                     @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };

        mMessagesRef.addChildEventListener(mRoomEventListener);
    }

    public void unregisterRoomListener() {
        if (mRoomEventListener != null) {
            mMessagesRef.removeEventListener(mRoomEventListener);
            mRoomEventListener = null;
        }
    }


    public interface MessageTransactionListener {
        void onStart();
        void onCompleted();
        void onSuccess(String newMessageId);
        void onError(String error);
    }

    public void addNewMessage(ChatMessage newMessage, final MessageTransactionListener listener) {
        if (listener != null) listener.onStart();
        mFirebaseDatabaseRef.child(ROOMS_CHILD).child(ROOM_ID)
                .child(MESSAGES_CHILD)
                .push().setValue(newMessage, (databaseError, databaseReference) -> {
                    if (listener == null) return;
                    listener.onCompleted();
                    if (databaseError == null) {
                        listener.onSuccess(databaseReference.getKey());
                    } else {
                        listener.onError("Error sending message");
                    }
                });
    }

    public void addImageToMessage(final String messageId, final String imageUri,
                                  final MessageTransactionListener listener) {
        if (listener != null) listener.onStart();
        mFirebaseDatabaseRef.child(ROOMS_CHILD)
                .child(ROOM_ID)
                .child(MESSAGES_CHILD)
                .child(messageId)
                .child(MESSAGE_IMAGE_FIELD)
                .setValue(imageUri, (databaseError, databaseReference) -> {
                    if (listener == null) return;
                    listener.onCompleted();
                    if (databaseError == null) {
                        listener.onSuccess(databaseReference.getKey());
                    } else {
                        listener.onError("Error adding message");
                    }
                });
    }

    public void addNewToken(String deviceId, String newToken) {
        mFirebaseDatabaseRef
                .child(FCM_TOKENS)
                .child(deviceId)
                .setValue(newToken);
    }
}
