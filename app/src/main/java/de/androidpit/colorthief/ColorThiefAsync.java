package de.androidpit.colorthief;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import cz.destil.moodsync.core.Config;

public class ColorThiefAsync {
    static final String LOG_TAG = "ColorThiefAsync";

    public static Builder from(Bitmap bitmap) {
        return new Builder(bitmap);
    }

    public static final class Builder {

        private final Bitmap mBitmap;
        private Rect mRegion;
        private Integer mOverallBrightness = 0;

        public Builder(Bitmap bitmap) {
            if (bitmap == null || bitmap.isRecycled()) {
                throw new IllegalArgumentException("Bitmap is not valid");
            }
            mBitmap = bitmap;
            mRegion = new Rect();
            mRegion.set(0,0,mBitmap.getWidth(),mBitmap.getHeight());
        }

        @NonNull
        public Builder setRegion(int left, int top, int right, int bottom) {
            if (mBitmap != null) {
                if (mRegion == null) mRegion = new Rect();
                // Set the Rect to be initially the whole Bitmap
                mRegion.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
                // Now just get the intersection with the region
                if (!mRegion.intersect(left, top, right, bottom)) {
                    throw new IllegalArgumentException("The given region must intersect with "
                            + "the Bitmap's dimensions.");
                }
            }
            return this;
        }

        public Integer getOverallBrightness(){
            return mOverallBrightness;
        }

        @NonNull
        public Integer getDominantColor() {
            if (mBitmap != null) {
                Bitmap bitmap = mBitmap;
                Rect region = mRegion;

                int[][] palette = ColorThief.getWPalette(bitmap,region,5,1,false);

                int overallBrightness = 0;

                int[] dominantColor = {0, 0, 0, 0};
                if (palette == null) {
                    return 0;
                }
                //dominantColor[0] = palette[0][0]; //count
                dominantColor[1] = palette[0][1]; //r
                dominantColor[2] = palette[0][2]; //g
                dominantColor[3] = palette[0][3]; //b
                //calculate average brightness
                for(int i = 0; i<palette.length; i++){
                    for (int j =1; j<4;j++){
                        overallBrightness += palette[i][j]*palette[i][0];
                    }
               }


                int pixelCount = 0;
                for(int i = 0; i<palette.length; i++){
                    pixelCount+=palette[i][0];
                }

                overallBrightness = overallBrightness / (3*pixelCount);
                //set dominant color opacity to weight average
                //dominantColor[0] = overallBrightness;
                dominantColor[0] = palette[0][0];
                mOverallBrightness = overallBrightness;

                for(int i = 0; i<palette.length; i++){
                    palette[i][0] = (int)(((float)palette[i][0]/pixelCount) * 255);
                }

                if(palette[0][0] < Config.MINIMUM_COLOR_DOMINANCE){
                    return 0;
                }

                int value = ((dominantColor[0] & 0xFF) << 24) | //alpha being repurposed to store color dominance
                        (((int) dominantColor[1] & 0xFF) << 16) | //red
                        (((int) dominantColor[2] & 0xFF) << 8) | //green
                        (((int) dominantColor[3] & 0xFF) << 0); //blue

                return value;
            } else {
                return 0;
            }
        }

        @NonNull
        public AsyncTask<Bitmap, Void, Integer[]> getDominantColor(final ColorThiefAsyncListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener can not be null");
            }


            return new AsyncTask<Bitmap, Void, Integer[]>() {
                @Override
                protected Integer[] doInBackground(Bitmap... bitmap) {
                    try {
                        Integer[] result = new Integer[2];
                        result[0] = getDominantColor();
                        result[1] = getOverallBrightness();
                        return result;
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception thrown during async generate", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Integer[] result) {
                    listener.onColorExtracted(result[0], result[1]);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mBitmap);
        }
    }
    public interface ColorThiefAsyncResponse {
        void processFinish(Integer result);
    }

    public interface ColorThiefAsyncListener {
        void onColorExtracted(Integer color, Integer overallBrightness);
    }

    public ColorThiefAsyncResponse delegate = null;//Call back interface

    public ColorThiefAsync(ColorThiefAsyncResponse asyncResponse) {
        delegate = asyncResponse;//Assigning call back interfacethrough constructor
    }


  /*  @Override
    protected Integer doInBackground(Bitmap... bitmap) {


        int[] rgb = ColorThief.getColor(bitmap[0]);
        int value = ((255 & 0xFF) << 24) | //alpha
                (((int)rgb[0] & 0xFF) << 16) | //red
                (((int)rgb[1] & 0xFF) << 8)  | //green
                (((int)rgb[2] & 0xFF) << 0); //blue
        return value;
    }*/


/*    @Override
    protected void onPostExecute(Integer result) {
        delegate.processFinish(result);
    }*/

}
