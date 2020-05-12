package cz.destil.moodsync.light;

import java.io.IOException;
import java.util.Arrays;

import olsenn1.LifxCommander.ControlMethods;
import olsenn1.LifxCommander.ReceiveMessages;
import olsenn1.Messages.DataTypes.Command;
import olsenn1.Messages.DataTypes.HSBK;
import olsenn1.Messages.Device.GetService;
import olsenn1.Messages.Light.SetPower_Light;
import olsenn1.Messages.Light.SetWaveform;
import olsenn1.Messages.Light.SetColor;
import olsenn1.Values.Power;
import olsenn1.Values.Waveforms;

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
    private Integer[][] mPreviousColors;
    private boolean mDisconnected;
    private int mPreviousColor = -1;
    private int port = 56700;
    private boolean mReduceDimLightChanges = false;
    private int mWhiteTemperature = 5500;
    private int mMinimumColorDominance = 0;
    private String mUnicastIP = "";
    private int mMinimumBrightness = 0;

    public static LightsController get() {
        if (sInstance == null) {
            sInstance = new LightsController();
        }
        return sInstance;
    }

    public void reduceDimLightChanges(boolean reduceDimLightChanges){
        mReduceDimLightChanges = reduceDimLightChanges;
    }

    public void minimumBrightness(int minimumBrightness){
        mMinimumBrightness = minimumBrightness;
    }

    public void whiteTemperature(int whiteTemperature){
        mWhiteTemperature = whiteTemperature;
    }

    public void unicastIP(String unicastIP){
        mUnicastIP = unicastIP;
    }

    public void minimumColorDominance(int minimumColorDominance){
        mMinimumColorDominance = minimumColorDominance;
    }

    public void changeColors(Integer[][] extractedColors){
        if (mWorkingFine){
            int numColors = extractedColors.length;
            HSBK colors[] = new HSBK[numColors];

            for (int i=0; i<extractedColors.length; i++) {
                mPreviousColor = mPreviousColors[i][0];
                colors[i] = convertColor(extractedColors[i][0],extractedColors[i][1]);
            }
            SetColor setColor = new SetColor(colors,Config.DURATION_OF_COLOR_CHANGE,Config.MULTIZONE_DIRECTION);
            Command changeColor = new Command(setColor);

            try {
                switch(mUnicastIP){
                    case "":
                        ControlMethods.sendBroadcastMessage(changeColor.getByteArray(), port);
                        break;
                    default:
                        ControlMethods.sendUdpMessage(changeColor.getByteArray(),mUnicastIP,port);
                        break;
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
            mPreviousColors = extractedColors;
        }
    }

    public void changeColor(int color, int overallBrightness) {
        if (mWorkingFine && color != mPreviousColor) {
            SetWaveform setWaveform = new SetWaveform();
            setWaveform.setColor(convertColor(color,overallBrightness));
            setWaveform.setCycles(1);
            setWaveform.setIsTransient(false);
            setWaveform.setPeriod(Config.DURATION_OF_COLOR_CHANGE);
            setWaveform.setWaveform(Waveforms.HALF_SINE);
            Command changeColor = new Command(setWaveform);
            try {
                switch(mUnicastIP){
                    case "":
                ControlMethods.sendBroadcastMessage(changeColor.getByteArray(), port);
                        break;
                    default:
                        ControlMethods.sendUdpMessage(changeColor.getByteArray(),mUnicastIP,port);
                        break;
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
            mPreviousColor = color;
        }
    }

    public void init() {
        mWorkingFine = false;
        mDisconnected = false;
        mPreviousColors = new Integer[Config.MULTIZONE_REGIONS][2];
        for (Integer[] row : mPreviousColors){
            Arrays.fill(row,0);
        }
    }

    private void startRocking() {
        App.bus().post(new SuccessEvent());
        mWorkingFine = true;
        SetPower_Light setPower = new SetPower_Light(Power.ON);
        Command powerOn = new Command(setPower);
        try {
            switch(mUnicastIP){
                case "":
                    ControlMethods.sendBroadcastMessage(powerOn.getByteArray(), port);
                    break;
                default:
                    ControlMethods.sendUdpMessage(powerOn.getByteArray(),mUnicastIP,port);
                    break;
            }
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

    private HSBK convertColor(int color, int overallBrightness) {
        float[] hsv = new float[3];

        int colorDominance = Color.alpha(color); //unpack stored color dominance from color alpha channel
        if (colorDominance < mMinimumColorDominance){
            color = mPreviousColor;
        }

        Color.RGBToHSV(Color.red(color),Color.green(color),Color.blue(color), hsv);
        float brightness;
        switch (Config.BRIGHTNESS_MODE) {
            case Config.BRIGHTNESS_STATIC:
                brightness = Config.LIFX_BRIGHTNESS;
                break;
            case Config.BRIGHTNESS_COLOR:
                brightness = hsv[2]*Config.LIFX_BRIGHTNESS;
                break;
            case Config.BRIGHTNESS_AVERAGE:
                brightness = (overallBrightness/255f) * Config.LIFX_BRIGHTNESS;
                break;
            default:
                brightness = Config.LIFX_BRIGHTNESS;
                break;
        }

        if(mReduceDimLightChanges) {
            if (brightness  < mMinimumBrightness) { //prevent light flickering at low brightness
                hsv[0] = 0.14f;
                hsv[1] = 0.0f;
                brightness = mMinimumBrightness;
            } else {
                float saturationModifier = Math.min(5*(brightness/65535f),1);
                hsv[1] = hsv[1]*saturationModifier; //desaturate dimmer lights
            }
        }
        hsv[1] = hsv[1]*Config.SATURATION;

        HSBK hsbk2 = new HSBK();
        hsbk2.setHue(Math.round(hsv[0] * 182));
        hsbk2.setSaturation(Math.round(hsv[1] * 65535));
        hsbk2.setBrightness(Math.round(brightness));
        hsbk2.setKelvin(mWhiteTemperature);
        return hsbk2;
    }

    public void signalStop() {
        int color = App.get().getResources().getColor(android.R.color.white);
        SetWaveform setWaveform = new SetWaveform();
        setWaveform.setColor(convertColor(color,1));
        setWaveform.setCycles(1);
        setWaveform.setIsTransient(false);
        setWaveform.setPeriod(100);
        setWaveform.setWaveform(Waveforms.HALF_SINE);
        Command changeColor = new Command(setWaveform);

        SetPower_Light setPower = new SetPower_Light(Power.OFF);
        Command powerOff = new Command(setPower);
        try {
            switch(mUnicastIP){
                case "":
                    ControlMethods.sendBroadcastMessage(changeColor.getByteArray(), port);
                    ControlMethods.sendBroadcastMessage(powerOff.getByteArray(), port);
                    break;
                default:
                    ControlMethods.sendUdpMessage(changeColor.getByteArray(),mUnicastIP,port);
                    ControlMethods.sendUdpMessage(powerOff.getByteArray(),mUnicastIP,port);
                    break;
            }
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
                switch(mUnicastIP){
                    case "":
                ControlMethods.sendBroadcastMessage(serviceCommand.getByteArray(), port);
                        break;
                    default:
                        ControlMethods.sendUdpMessage(serviceCommand.getByteArray(),mUnicastIP,port);
                        break;
                }

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
