package de.androidpit.colorthief;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

public class ColorThiefAsync {
    static final String LOG_TAG = "ColorThiefAsync";

    public static Builder from(Bitmap bitmap) {
        return new Builder(bitmap);
    }

    public static final class Builder {

        private final Bitmap mBitmap;
        private Rect mRegion;

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

        @NonNull
        public Integer getDominantColor() {
            if (mBitmap != null) {
                Bitmap bitmap;
                bitmap = mBitmap;
                Rect region = mRegion;

                int[] rgb = ColorThief.getColor(bitmap,region,1,false);

                // If created a new bitmap, recycle it
                if (bitmap != mBitmap) {
                    bitmap.recycle();
                }
                int value = ((255 & 0xFF) << 24) | //alpha
                        (((int) rgb[0] & 0xFF) << 16) | //red
                        (((int) rgb[1] & 0xFF) << 8) | //green
                        (((int) rgb[2] & 0xFF) << 0); //blue

                return value;
            } else {
                return 0;
            }
        }

        @NonNull
        public AsyncTask<Bitmap, Void, Integer> getDominantColor(final ColorThiefAsyncListener listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener can not be null");
            }


            return new AsyncTask<Bitmap, Void, Integer>() {
                @Override
                protected Integer doInBackground(Bitmap... bitmap) {
                    try {
                        return getDominantColor();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Exception thrown during async generate", e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Integer color) {
                    listener.onColorExtracted(color);
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mBitmap);
        }
    }
    public interface ColorThiefAsyncResponse {
        void processFinish(Integer result);
    }

    public interface ColorThiefAsyncListener {
        void onColorExtracted(Integer result);
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
