package com.pushbullet.android.extension;

interface IMessagingExtension {
    oneway void onMessageReceived(String conversationIden, String message);
    oneway void onConversationDismissed(String conversationIden);
}
