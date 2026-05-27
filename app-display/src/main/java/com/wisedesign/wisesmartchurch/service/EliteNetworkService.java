package com.wisedesign.wisesmartchurch.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.wisedesign.wisesmartchurch.WiseSmartApp;
import com.wisedesign.wisesmartchurch.display.R;
import com.wisedesign.wisesmartchurch.network.EliteNetworkManager;
import com.wisedesign.wisesmartchurch.ui.display.TvDisplayActivity;

/**
 * Service Foreground qui maintient les serveurs HTTP + WebSocket
 * actifs même quand l'interface est en arrière-plan.
 */
public class EliteNetworkService extends Service {

    private static final int NOTIFICATION_ID = 1001;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Serveur EliteCast actif"));
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EliteNetworkManager.getInstance(this).stop();
    }

    private Notification buildNotification(String text) {
        Intent notifIntent = new Intent(this, TvDisplayActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, WiseSmartApp.CHANNEL_ID_NETWORK)
                .setContentTitle("EliteCast Pro")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_broadcast)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
