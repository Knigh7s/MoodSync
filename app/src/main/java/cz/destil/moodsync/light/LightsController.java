package cz.destil.moodsync.light;

import java.io.IOException;

import com.lifx.LifxCommander.ControlMethods;
import com.lifx.LifxCommander.ReceiveMessages;
import com.lifx.Messages.DataTypes.Command;
import com.lifx.Messages.DataTypes.HSBK;
import com.lifx.Messages.Device.GetService;
import com.lifx.Messages.Light.SetPower_Light;
import com.lifx.Messages.Light.SetWaveform;
import com.lifx.Values.Power;
import com.lifx.Values.Waveforms;

import android.graphics.Color;
import android.os.StrictMode;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.BaseAsyncTask;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.event.SuccessEvent;

/**
 * Controller which controls LIFX lights.
 *
 * @author David Vávra (david@vavra.me)
 */
public class LightsController {

    private static final int TIMEOUT = 5000;
    private static LightsController sInstance;
    private boolean mWorkingFine;
    private boolean mDisconnected;
    private int mPreviousColor = -1;
    private int port = 56700;

    public static LightsController get() {
        if (sInstance == null) {
            sInstance = new LightsController();
        }
        return sInstance;
    }

    public void changeColor(int color) {
        if (mWorkingFine && color != mPreviousColor) {
            SetWaveform setWaveform = new SetWaveform();
            setWaveform.setColor(convertColor(color));
            setWaveform.setCycles(1);
            setWaveform.setIsTransient(false);
            setWaveform.setPeriod(Config.DURATION_OF_COLOR_CHANGE);
            setWaveform.setWaveform(Waveforms.HALF_SINE);
            Command changeColor = new Command(setWaveform);
            try {
                ControlMethods.sendBroadcastMessage(changeColor.getByteArray(), port);
            } catch(IOException e) {
                e.printStackTrace();
            }
            mPreviousColor = color;
        }
    }

    public void init() {
        mWorkingFine = false;
        mDisconnected = false;
    }

    private void startRocking() {
        App.bus().post(new SuccessEvent());
        mWorkingFine = true;
        SetPower_Light setPower = new SetPower_Light(Power.ON);
        Command powerOn = new Command(setPower);
        try {
            ControlMethods.sendBroadcastMessage(powerOn.getByteArray(), port);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ReceiveMessages receiveMessages = new ReceiveMessages(port);
        receiveMessages.start();
        /*if (!mWorkingFine) {
            new TimeoutTask().start();
        } else {*/
            startRocking();
//        }
    }

    public void stop() {
        mDisconnected = true;
        //TODO disconnect properly
        /*if (mNetworkContext != null && mWorkingFine) {
            mNetworkContext.disconnect();
        }*/
    }

    private HSBK convertColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        HSBK hsbk2 = new HSBK();
        hsbk2.setHue(Math.round(hsv[0] * 182));
        hsbk2.setSaturation(Math.round(hsv[1] * 65535));
        hsbk2.setBrightness(Config.LIFX_BRIGHTNESS);
        hsbk2.setKelvin(3500);
        return hsbk2;
    }

    public void signalStop() {
        int color = App.get().getResources().getColor(android.R.color.white);
        SetWaveform setWaveform = new SetWaveform();
        setWaveform.setColor(convertColor(color));
        setWaveform.setCycles(1);
        setWaveform.setIsTransient(false);
        setWaveform.setPeriod(100);
        setWaveform.setWaveform(Waveforms.HALF_SINE);
        Command changeColor = new Command(setWaveform);
        try {
            ControlMethods.sendBroadcastMessage(changeColor.getByteArray(), port);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    class TimeoutTask extends BaseAsyncTask {

        @Override
        public void inBackground() {
            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void postExecute() {
            //TODO handle response from light bulbs to know how many are they
            GetService getService = new GetService();
            Command serviceCommand = new Command(getService);
            serviceCommand.getFrame().setTagged(true);
            try {
                ControlMethods.sendBroadcastMessage(serviceCommand.getByteArray(), port);
            } catch(IOException e) {
                e.printStackTrace();
            }
            /*int numLights = mNetworkContext.getAllLightsCollection().getLights().size();
            if (numLights == 0 || mDisconnected) {
                App.bus().post(new ErrorEvent(R.string.no_lights_found));
            } else {
                startRocking();
            }*/
        }
    }
}
