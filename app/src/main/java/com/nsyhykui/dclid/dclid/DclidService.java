package com.nsyhykui.dclid.dclid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class DclidService extends Service {
    private DclidDaemon daemon;

    @Override
    public void onCreate() {
        super.onCreate();
        String channelId = "dclid_daemon";
        NotificationChannel channel = new NotificationChannel(channelId, "Dclid Daemon", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, channelId)
                .setContentTitle("Dclid 正在运行")
                .setContentText("监听端口 12345")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();

        startForeground(1, notification);
        
        daemon = new DclidDaemon(this);
        daemon.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (daemon != null) {
            daemon.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
