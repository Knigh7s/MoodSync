package cz.destil.moodsync.light;

import android.graphics.Bitmap;

import cz.destil.moodsync.core.Config;
import cz.destil.moodsync.util.SleepTask;
import de.androidpit.colorthief.ColorThiefAsync;
/**
 * Periodically extracts color from a bitmap.
 *
 * @author David Vávra (david@vavra.me)
 */
public class ColorExtractor {


    private static ColorExtractor sInstance;
    private boolean mRunning = true;
    private boolean mIgnoreBlackLines = false;
    private int mColorRegionLeft = 0;
    private int mColorRegionRight = Config.VIRTUAL_DISPLAY_WIDTH;
    private int mColorRegionTop = 0;
    private int mColorRegionBottom = Config.VIRTUAL_DISPLAY_HEIGHT;

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

    public void ignoreBlackLines(boolean ignoreBlackLines){
        mIgnoreBlackLines = ignoreBlackLines;
    }

    public void colorRegionLeft(int colorRegionLeft){
        mColorRegionLeft = Math.round(Config.VIRTUAL_DISPLAY_WIDTH*colorRegionLeft/100f);
    }

    public void colorRegionRight(int colorRegionRight){
        mColorRegionRight = Math.round(Config.VIRTUAL_DISPLAY_WIDTH*colorRegionRight/100f);
    }

    public void colorRegionTop(int colorRegionTop){
        mColorRegionTop = Math.round(Config.VIRTUAL_DISPLAY_HEIGHT*colorRegionTop/100f);
    }

    public void colorRegionBottom(int colorRegionBottom){
        mColorRegionBottom = Math.round(Config.VIRTUAL_DISPLAY_HEIGHT*colorRegionBottom/100f);
    }

    private void extractBitmap(final MirroringHelper mirroring, final Listener listener) {
        if (mRunning) {
            mirroring.getLatestBitmap(new MirroringHelper.Listener() {
                @Override
                public void onBitmapAvailable(final Bitmap bitmap) {
                    final Bitmap bitmapCopy = bitmap.copy(bitmap.getConfig(),true);
                    ColorThiefAsync
                            .from(bitmapCopy)
                            .setRegion(mColorRegionLeft,mColorRegionTop,mColorRegionRight,mColorRegionBottom)
                            .setNumRegions(Config.MULTIZONE_REGIONS)
                            .ignoreBlackLines(mIgnoreBlackLines)
                            .getDominantColors(new ColorThiefAsync.ColorThiefAsyncListener(){
/*
                                @Override
                                public void onColorExtracted(Integer color, Integer overallBrightness) {
                                    bitmapCopy.recycle();
                                    listener.onColorExtracted(color, overallBrightness);
                                    new SleepTask(Config.FREQUENCE_OF_SCREENSHOTS, new SleepTask.Listener() {
                                        @Override
                                        public void awoken() {
                                            extractBitmap(mirroring, listener);
                                        }
                                    }).start();
                                }
                                public void onColorsExtracted(Integer[][] extractedData) { return; }
                                */
                                @Override
                                public void onColorsExtracted(Integer[][] extractedData) {
                                    bitmapCopy.recycle();
                                    listener.onColorsExtracted(extractedData);
                                    new SleepTask(Config.FREQUENCE_OF_SCREENSHOTS, new SleepTask.Listener() {
                                        @Override
                                        public void awoken() {
                                            extractBitmap(mirroring, listener);
                                        }
                                    }).start();
                                }
                                @Override
                                public void onColorExtracted(Integer color, Integer overallBrightness) { return;}
                            });
                }
            });
        }
    }

    public void stop() {
        mRunning = false;
    }

    public interface Listener {
        public void onColorExtracted(int color, int overallBrightness);
        public void onColorsExtracted(Integer[][] extractedData);
    }
}
