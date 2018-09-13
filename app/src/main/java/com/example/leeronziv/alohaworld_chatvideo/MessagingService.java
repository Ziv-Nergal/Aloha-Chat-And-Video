package com.example.leeronziv.alohaworld_chatvideo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class MessagingService extends FirebaseMessagingService
{
    private static final String CHANNEL_ID = "messaging_channel_id";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage)
    {
        super.onMessageReceived(remoteMessage);
        createNotificationChannel();

        if(remoteMessage.getData().size() > 0)
        {
            String notificationTitleStr = remoteMessage.getNotification().getTitle();
            String notificationMessageStr = remoteMessage.getNotification().getBody();

            String clickActionStr = remoteMessage.getNotification().getClickAction();

            String senderId = remoteMessage.getData().get("sender_id");

            Intent notificationTapIntent = new Intent(clickActionStr);
            notificationTapIntent.putExtra("user_id", senderId);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationTapIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher_aloha)
                    .setContentTitle(notificationTitleStr)
                    .setContentText(notificationMessageStr)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVibrate(new long[]{500, 500})
                    .setLights(Color.YELLOW, 2000, 2000)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(notificationId, mBuilder.build());
        }
    }

    private void createNotificationChannel()
    {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
