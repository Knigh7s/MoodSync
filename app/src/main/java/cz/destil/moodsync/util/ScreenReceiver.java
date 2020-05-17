package cz.destil.moodsync.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.destil.moodsync.service.LightsService;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Intent i = new Intent(context, LightsService.class);
            i.setAction("PAUSE");
            context.startService(i);
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        Intent i = new Intent(context, LightsService.class);
            i.setAction("RESUME");
            context.startService(i);
        }
    }
}
