package com.pushbullet.android.extension;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;

public abstract class MessagingExtension extends Service {
    private static final String EXTRA_PACKAGE_NAME = "package_name";
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";
    private static final String EXTRA_NOTIFICATION_TAG = "notification_tag";
    private static final String EXTRA_CONVERSATION_IDEN = "converstation_iden";
    private static final String EXTRA_IMAGE = "image";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_SENDER = "sender";
    public static final String ACTION_SMS_EXTENSION = "com.pushbullet.android.extension.MessagingExtension";
    private final IMessagingExtension.Stub mBinder = new IMessagingExtension.Stub() {
        @Override
        public void onMessageReceived(final String conversationIden, final String message) throws RemoteException {
            MessagingExtension.this.onMessageReceived(conversationIden, message);
        }

        @Override
        public void onConversationDismissed(final String conversationIden) throws RemoteException {
            MessagingExtension.this.onConversationDismissed(conversationIden);
        }
    };

    public static void mirrorMessage(final Context context, final String conversationIden, final String sender, final String message, final Bitmap image,
                                     final String notificationTag, final int notificationId) {
        if (TextUtils.isEmpty(conversationIden)) {
            throw new IllegalArgumentException("conversationIden must not be null or empty");
        } else if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }

        final PackageInfo info;
        try {
            info = context.getPackageManager().getPackageInfo("com.pushbullet.android", 0);
            if (info.versionCode < 90) {
                return;
            }
        } catch (final PackageManager.NameNotFoundException e) {
            return;
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... voids) {
                final Bitmap finalImage;
                if (image.getHeight() > 512 || image.getWidth() > 512) {
                    final float factor = image.getWidth() / 512f;
                    finalImage = Bitmap.createScaledBitmap(image, 512, (int) (image.getHeight() / factor), true);
                } else {
                    finalImage = image;
                }

                final ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                finalImage.compress(Bitmap.CompressFormat.JPEG, 90, imageStream);

                final Intent mirrorMessage = new Intent();
                mirrorMessage.setComponent(new ComponentName("com.pushbullet.android",
                        "com.pushbullet.android.notifications.mirroring.ExtensionMirrorMessageReceiver"));
                mirrorMessage.putExtra(EXTRA_SENDER, sender);
                mirrorMessage.putExtra(EXTRA_MESSAGE, message);
                mirrorMessage.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
                mirrorMessage.putExtra(EXTRA_IMAGE, imageStream.toByteArray());
                mirrorMessage.putExtra(EXTRA_CONVERSATION_IDEN, conversationIden);
                mirrorMessage.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
                mirrorMessage.putExtra(EXTRA_NOTIFICATION_TAG, notificationTag);
                context.sendBroadcast(mirrorMessage);
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void) null);
    }

    protected abstract void onMessageReceived(final String conversationIden, final String message);

    protected abstract void onConversationDismissed(final String conversationIden);

    @Override
    public IBinder onBind(final Intent intent) {
        return this.mBinder;
    }
}
