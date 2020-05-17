package cz.destil.moodsync.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.otto.Subscribe;

import cz.destil.moodsync.R;
import cz.destil.moodsync.activity.MainActivity;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.event.LocalColorEvent;
import cz.destil.moodsync.event.PreferenceEvent;
import cz.destil.moodsync.light.ColorExtractor;
import cz.destil.moodsync.light.LightsController;
import cz.destil.moodsync.light.LocalColorSwitcher;
import cz.destil.moodsync.light.MirroringHelper;
import cz.destil.moodsync.util.ScreenReceiver;
import cz.destil.moodsync.util.SleepTask;

/**
 * Service which does all the work.
 *
 * @author David Vávra (david@vavra.me)
 */
public class LightsService extends Service {
    private MirroringHelper mMirroring;
    private ColorExtractor mColorExtractor;
    private LightsController mLights;
    private WifiManager.MulticastLock mMulticastLock;
    private LocalColorSwitcher mLocalSwitcher;
    private String mUnicastIP = "";
    private ScreenReceiver mScreenReceiver;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMirroring = MirroringHelper.get();
        mColorExtractor = ColorExtractor.get();
        mLights = LightsController.get();
        mLocalSwitcher = LocalColorSwitcher.get();
        App.bus().register(this);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mScreenReceiver = new ScreenReceiver();
        registerReceiver(mScreenReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mScreenReceiver);
        App.bus().unregister(this);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case "START":
                start();
                break;
            case "STOP":
                stop();
                break;
            case "PAUSE":
                pause();
                break;
            case "RESUME":
                resume();
                break;
        }
        return START_REDELIVER_INTENT;
    }

    private void start() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        Notification notification = new Notification.Builder(this).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string
                .mirroring)).setContentText(getString(R.string.tap_to_change))
                .setContentIntent(pi).build();
        startForeground(42, notification);
        updatePreferences();
        if(mUnicastIP == "") {
            WifiManager wifi;
            wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mMulticastLock = wifi.createMulticastLock("lifx");
            mMulticastLock.acquire();
        } else {
            mLights.unicastIP(mUnicastIP);
        }

        resume();
    }

    private void updatePreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mLights.reduceDimLightChanges(sharedPreferences.getBoolean("reduce_dim_light_changes", false));
        mColorExtractor.ignoreBlackLines(sharedPreferences.getBoolean("ignore_black_lines", false));
        mUnicastIP = sharedPreferences.getString("unicast_ip", "");
        mLights.minimumBrightness(Integer.valueOf(sharedPreferences.getString("minimum_brightness", getString(R.string.default_minimum_brightness))));
        mLights.minimumColorDominance(Integer.valueOf(sharedPreferences.getString("minimum_color_dominance", getString(R.string.default_minimum_color_dominance))));
        mLights.whiteTemperature(Integer.valueOf(sharedPreferences.getString("white_temperature", getString(R.string.default_white_temperature))));
        if (sharedPreferences.getBoolean("enable_color_region", false)) {
            mColorExtractor.colorRegionLeft(Integer.valueOf(sharedPreferences.getString("color_region_left", getString(R.string.default_color_region_left))));
            mColorExtractor.colorRegionRight(Integer.valueOf(sharedPreferences.getString("color_region_right", getString(R.string.default_color_region_right))));
            mColorExtractor.colorRegionTop(Integer.valueOf(sharedPreferences.getString("color_region_top", getString(R.string.default_color_region_top))));
            mColorExtractor.colorRegionBottom(Integer.valueOf(sharedPreferences.getString("color_region_bottom", getString(R.string.default_color_region_bottom))));
        }
        mLights.redCalibration(Integer.valueOf(sharedPreferences.getString("red_calibration","255")));
        mLights.blueCalibration(Integer.valueOf(sharedPreferences.getString("blue_calibration","255")));
        mLights.greenCalibration(Integer.valueOf(sharedPreferences.getString("green_calibration","255")));
        mLights.saturation(Integer.valueOf(sharedPreferences.getString("saturation","255")));
    }

    private void pause() {
        mColorExtractor.stop();
        mLights.signalStop();
    }

    private void resume() {
        mLights.start();
        mColorExtractor.start(mMirroring, new ColorExtractor.Listener() {
/*
            @Override
            public void onColorExtracted(int color, int overallBrightness) {
                if (!mLocalSwitcher.isRunning()) {
                    mLights.changeColor(color, overallBrightness);
                }
            }
            public void onColorsExtracted(Integer[][] extractedColors){
                return;
            }*/

            @Override
            public void onColorExtracted(int color, int overallBrightness) { return;}

            @Override
            public void onColorsExtracted(Integer[][] extractedColors) {
                if (!mLocalSwitcher.isRunning()) {
                    mLights.changeColors(extractedColors);
                }
            }

        });

    }

    private void stop() {
        mColorExtractor.stop();
        mMirroring.stop();
        mLights.signalStop();
        new SleepTask(Config.FINAL_DELAY, new SleepTask.Listener() {
            @Override
            public void awoken() {
                mLights.stop();
                if (mMulticastLock != null) {
                    mMulticastLock.release();
                }
                stopForeground(true);
                stopSelf();
            }
        }).start();
    }

    @Subscribe
    public void onNewLocalColor(LocalColorEvent event) {
                Integer colors[][]  = new Integer[16][2];
        for(int i=0; i<16; i++) {
            colors[i][0] = event.newColor;
            colors[i][1] = 255;
        }
        //mLights.changeColor(event.newColor,255);
        mLights.changeColors(colors);
    }

    @Subscribe
    public void onPreferencesUpdated(PreferenceEvent event){
        updatePreferences();
    }
}
