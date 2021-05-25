package es.deusto.mcu.classchat2021.firebase.data;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


public class CloudStorage {

    private static final String FOLDER_CHAT_IMAGES = "/chat_images";

    private StorageReference mFirebaseStorageRef;

    public CloudStorage() {
        mFirebaseStorageRef = FirebaseStorage.getInstance().getReference();
    }

    public interface UploadImageListener {
        void onStart(String imageRef);
        void onProgress(long transferredBytes, long totalBytes);
        void onCompleted();
        void onSuccess(String imageRef);
        void onError(String error);
    }

    public void uploadImage(final String userId, final String messageId,
                                   final Uri imageUri, final UploadImageListener listener) {

        StorageReference newImageRef =
                mFirebaseStorageRef.child(FOLDER_CHAT_IMAGES)
                        .child(userId)
                        .child(messageId);
        final String imageRef = newImageRef.toString();

        if (listener != null) listener.onStart(imageRef);
        newImageRef.putFile(imageUri)
                .addOnProgressListener(task -> {
                    if (listener != null) {
                        listener.onProgress(task.getBytesTransferred(), task.getTotalByteCount());
                    }
                })
                .addOnCompleteListener(
                        task -> {
                            if (listener == null) return;
                            listener.onCompleted();
                            if (task.isSuccessful()) {
                                listener.onSuccess(imageRef);
                            } else {
                                listener.onError("Error uploading image:" + task.getException());
                            }
                        });
    }


    public interface DownloadUrlListener {
        void onSuccess(String downloadUrl);
        void onError(String error);
    }

    public void getDownloadUrl(String messageURL, DownloadUrlListener listener) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(messageURL);
        storageRef.getDownloadUrl().addOnCompleteListener(
                task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        listener.onSuccess(task.getResult().toString());
                    } else {
                        listener.onError("Error getting downloadable URL:" + task.getException());
                    }
                });
    }

}
