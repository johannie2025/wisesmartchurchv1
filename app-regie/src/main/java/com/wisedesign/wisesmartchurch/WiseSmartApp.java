package com.wisedesign.elitecastpro;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.multidex.MultiDex;
import android.content.Context;

public class EliteCastApp extends Application {

    public static final String CHANNEL_ID_NETWORK = "elitecast_network";
    public static final String CHANNEL_ID_BROADCAST = "elitecast_broadcast";

    private static EliteCastApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannels();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static EliteCastApp getInstance() {
        return instance;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel netChannel = new NotificationChannel(
                    CHANNEL_ID_NETWORK,
                    "EliteCast Network Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            netChannel.setDescription("Service réseau HTTP/WebSocket/NSD");

            NotificationChannel broadcastChannel = new NotificationChannel(
                    CHANNEL_ID_BROADCAST,
                    "EliteCast Broadcast",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(netChannel);
            manager.createNotificationChannel(broadcastChannel);
        }
    }
}
