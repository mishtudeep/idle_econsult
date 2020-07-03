package net.jitsi.sdktest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationReceiver extends BroadcastReceiver {
    private String LOCK_SCREEN_KEY = "lockScreenKey";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getBooleanExtra(LOCK_SCREEN_KEY, true)) {
           // showNotificationWithFullScreenIntent(true);
        } else {
            //context.showNotificationWithFullScreenIntent();
        }
    }
}
