package cz.destil.moodsync.light;

import android.graphics.Bitmap;
import android.support.v7.graphics.Palette;
import android.support.v7.graphics.Target;

import cz.destil.moodsync.R;
import cz.destil.moodsync.core.App;
import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.util.SleepTask;

/**
 * Periodically extracts color from a bitmap.
 *
 * @author David Vávra (david@vavra.me)
 */
public class ColorExtractor {


    private static ColorExtractor sInstance;
    private boolean mRunning = true;
    private static Target DOMINANT;

    static {
        DOMINANT = new Target.Builder().setPopulationWeight(1f)
                .setMinimumLightness(0f)
                .setTargetLightness(0.5f)
                .setMaximumLightness(1f)
                .setMinimumSaturation(0f)
                .setTargetSaturation(0.6f)
                .setMaximumSaturation(1f)
                .setSaturationWeight(0f)
                .setLightnessWeight(0f)
                .setExclusive(false)
                .build();
    }

    public static ColorExtractor get() {
        if (sInstance == null) {
            sInstance = new ColorExtractor();
        }
        return sInstance;
    }

    public void start(final MirroringHelper mirroring, final Listener listener) {
        mRunning = true;
        new SleepTask(Config.INITIAL_DELAY, new SleepTask.Listener() {
            @Override
            public void awoken() {
                extractBitmap(mirroring, listener);
            }
        }).start();
    }

    private void extractBitmap(final MirroringHelper mirroring, final Listener listener) {
        if (mRunning) {
            mirroring.getLatestBitmap(new MirroringHelper.Listener() {
                @Override
                public void onBitmapAvailable(final Bitmap bitmap) {

                Palette.
                    from(bitmap)
                    .maximumColorCount(25)
                    //.setRegion(0,0,Config.VIRTUAL_DISPLAY_WIDTH,Config.VIRTUAL_DISPLAY_HEIGHT)
                    .clearFilters()
                    .clearTargets()
                    .addTarget(DOMINANT)
                    .generate(new Palette.PaletteAsyncListener(){
                        @Override
                        public void onGenerated(Palette palette) {
                            bitmap.recycle();
                            int defaultColor = App.get().getResources().getColor(R.color.not_recognized);
                            final int color = palette.getColorForTarget(DOMINANT,defaultColor);//.getDominantColor(defaultColor);
                            listener.onColorExtracted(color);
                            new SleepTask(Config.FREQUENCE_OF_SCREENSHOTS, new SleepTask.Listener() {
                                @Override
                                public void awoken() {
                                    extractBitmap(mirroring, listener);
                                }
                            }).start();
                        }
                    });
                }
            });
        }
    }

    public void stop() {
        mRunning = false;
    }

    public interface Listener {
        public void onColorExtracted(int color);
    }
}
