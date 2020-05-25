package cz.destil.moodsync.light;

import java.io.IOException;
import java.util.Arrays;

import cz.destil.moodsync.util.SleepTask;
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
    private float mRedMultiplier = 1.0f;
    private float mGreenMultiplier = 1.0f;
    private float mBlueMultiplier = 1.0f;
    private float mSaturation = 1.0f;
    private int mDuration = 400;
    private HSBK[] mPreviousHSBKColors;
    private boolean mAlternateInterpolation = false;
    private int mSampleInterval = 50;

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

    public void redCalibration(int red){
        mRedMultiplier = red/255.0f;
    }

    public void greenCalibration(int green){
        mGreenMultiplier = green/255.0f;
    }

    public void blueCalibration(int blue){
        mBlueMultiplier = blue/255.0f;
    }

    public void transitionDuration(int duration){
        mDuration = duration;
    }

    public void alternateInterpolation(boolean alternateInterpolation){
        mAlternateInterpolation = alternateInterpolation;
    }

    public void sampleInterval(int sampleInterval){
        mSampleInterval = sampleInterval;
    }

    public void saturation(int saturation) { mSaturation = saturation/255.0f; }

    public void changeColors(Integer[][] extractedColors){
        if (mWorkingFine){
            int numColors = extractedColors.length;
            HSBK colors[] = new HSBK[numColors];
            HSBK transitionColors[] = new HSBK[numColors];
            int duration = mDuration;
            boolean useInterpolatedColor = false;

            for (int i=0; i<extractedColors.length; i++) {
                mPreviousColor = mPreviousColors[i][0];
                colors[i] = convertColor(extractedColors[i][0],extractedColors[i][1]);
                if(mAlternateInterpolation) {
                    transitionColors[i] = new HSBK();
                    int distance = distance(colors[i].getHue(), mPreviousHSBKColors[i].getHue());


                    transitionColors[i].setKelvin(mWhiteTemperature);



                    if (distance > 16384) {
                        useInterpolatedColor = true;
                        transitionColors[i].setHue(mPreviousHSBKColors[i].getHue());
                        transitionColors[i].setSaturation(0);
                        transitionColors[i].setBrightness((colors[i].getBrightness() + mPreviousHSBKColors[i].getBrightness()) / 4);
                    } else {
                        transitionColors[i].setBrightness((colors[i].getBrightness() + mPreviousHSBKColors[i].getBrightness()) / 2);
                        int prevHue = mPreviousHSBKColors[i].getHue();
                        int nextHue = colors[i].getHue();
                        if ((prevHue + distance) % 65536 == nextHue) {
                            transitionColors[i].setHue((prevHue + (distance/2)) % 65536);
                        } else {
                            transitionColors[i].setHue((prevHue - (distance/2)) % 65536);
                        }
                        transitionColors[i].setSaturation((colors[i].getSaturation() + mPreviousHSBKColors[i].getSaturation()) / 2);

                    }
                }

            }
            if(useInterpolatedColor) {
                duration = mDuration/2;
                SetColor setIntermediateColor = new SetColor(transitionColors, duration, Config.MULTIZONE_DIRECTION);
                Command intermediateColorCommand = new Command(setIntermediateColor);
                sendColorCommand(intermediateColorCommand);
                SetColor setColor = new SetColor(colors, duration, Config.MULTIZONE_DIRECTION);
                final Command finalColorCommand = new Command(setColor);
                new SleepTask(mSampleInterval / 2, new SleepTask.Listener() {
                    @Override
                    public void awoken() {
                        sendColorCommand(finalColorCommand);
                    }
                }).start();
            } else {
                SetColor setColor = new SetColor(colors, duration, Config.MULTIZONE_DIRECTION);
                Command colorCommand = new Command(setColor);
                sendColorCommand(colorCommand);
            }
            mPreviousColors = extractedColors;
            mPreviousHSBKColors = colors;
        }
    }

    private int distance(int alpha, int beta) {
        int phi = Math.abs(beta - alpha) % 65536;       // This is either the distance or 360 - distance
        int distance = phi > 32786 ? 65536 - phi : phi;
        return distance;
    }

    private void sendColorCommand(Command command){
        try {
            switch(mUnicastIP){
                case "":
                    ControlMethods.sendBroadcastMessage(command.getByteArray(), port);
                    break;
                default:
                    ControlMethods.sendUdpMessage(command.getByteArray(),mUnicastIP,port);
                    break;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void changeColor(int color, int overallBrightness) {
        if (mWorkingFine && color != mPreviousColor) {
            SetWaveform setWaveform = new SetWaveform();
            setWaveform.setColor(convertColor(color,overallBrightness));
            setWaveform.setCycles(1);
            setWaveform.setIsTransient(false);
            setWaveform.setPeriod(mDuration);
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
        mPreviousHSBKColors = new HSBK[Config.MULTIZONE_REGIONS];
        for (int i=0; i<mPreviousHSBKColors.length; i++){
            mPreviousHSBKColors[i] = convertColor(0,0);
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

        int red = Math.round(Color.red(color)*mRedMultiplier);
        int green = Math.round(Color.green(color)*mGreenMultiplier);
        int blue = Math.round(Color.blue(color)*mBlueMultiplier);
        Color.RGBToHSV(red,green,blue, hsv);
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
            } else {
                float saturationModifier = Math.min(5*(brightness/65535f),1);
                hsv[1] = hsv[1]*saturationModifier; //desaturate dimmer lights
            }
        }
        if (brightness < mMinimumBrightness){
            brightness = mMinimumBrightness;
        }
        hsv[1] = hsv[1]*mSaturation;

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
