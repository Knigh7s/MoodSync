/*
 * Java Color Thief
 * by Sven Woltmann, Fonpit AG
 *
 * modified for usage on MoodSync by johnwang16
 * 
 * https://www.androidpit.com
 * https://www.androidpit.de
 *
 * License
 * -------
 * Creative Commons Attribution 2.5 License:
 * http://creativecommons.org/licenses/by/2.5/
 *
 * Thanks
 * ------
 * Lokesh Dhakar - for the original Color Thief JavaScript version
 * available at http://lokeshdhakar.com/projects/color-thief/
 */

package de.androidpit.colorthief;

//import java.awt.image.BufferedImage;
//import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import cz.destil.moodsync.core.Config;

import de.androidpit.colorthief.MMCQ.CMap;

public class ColorThief {

    private static final int DEFAULT_QUALITY = 10;
    private static final boolean DEFAULT_IGNORE_WHITE = false;
    private static final boolean DEFAULT_IGNORE_BLACK_LINES = false;

    /**
     * Use the median cut algorithm to cluster similar colors and return the base color from the
     * largest cluster.
     *
     * @param sourceImage
     *            the source image
     *
     * @return the dominant color as RGB array
     */
    public static int[] getColor(Bitmap sourceImage, Rect region) {
        int[][] palette = getPalette(sourceImage, region, 5);
        if (palette == null) {
            return null;
        }
        int[] dominantColor = palette[0];
        return dominantColor;
    }

    /**
     * Use the median cut algorithm to cluster similar colors and return the base color from the
     * largest cluster.
     *
     * @param sourceImage
     *            the source image
     * @param quality
     *            1 is the highest quality settings. 10 is the default. There is a trade-off between
     *            quality and speed. The bigger the number, the faster a color will be returned but
     *            the greater the likelihood that it will not be the visually most dominant color.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the dominant color as RGB array
     */
    public static int[] getColor(Bitmap sourceImage, Rect region, int quality, boolean ignoreWhite) {
        int[][] palette = getPalette(sourceImage, region, 5, quality, ignoreWhite);
        if (palette == null) {
            int[] nocolor = {0,0,0,0};
            return nocolor;
        }
        int[] dominantColor = palette[0];
        return dominantColor;
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     * 
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     * 
     * @return the palette as array of RGB arrays
     */
    public static int[][] getPalette(Bitmap sourceImage, Rect region, int colorCount) {
        CMap cmap = getColorMap(sourceImage, region, colorCount);
        if (cmap == null) {
            return null;
        }
        return cmap.palette();
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     * 
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     * @param quality
     *            1 is the highest quality settings. 10 is the default. There is a trade-off between
     *            quality and speed. The bigger the number, the faster the palette generation but
     *            the greater the likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     * 
     * @return the palette as array of RGB arrays
     */
    public static int[][] getPalette(
            Bitmap sourceImage,
            Rect region,
            int colorCount,
            int quality,
            boolean ignoreWhite) {
        CMap cmap = getColorMap(sourceImage, region, colorCount, quality, ignoreWhite, DEFAULT_IGNORE_BLACK_LINES);
        if (cmap == null) {
            return null;
        }
        return cmap.palette();
    }

    public static int[][] getWPalette( //get weighted palette
            Bitmap sourceImage,
            Rect region,
            int colorCount,
            int quality,
            boolean ignoreWhite,
            boolean ignoreBlackLines) {
        CMap cmap = getColorMap(sourceImage, region, colorCount, quality, ignoreWhite, ignoreBlackLines);
        if (cmap == null) {
            return null;
        }
        return cmap.wpalette();
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     * 
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned (minimum 2, maximum 256)
     * 
     * @return the color map
     */
    public static CMap getColorMap(Bitmap sourceImage, Rect region, int colorCount) {
        return getColorMap(sourceImage, region, colorCount, DEFAULT_QUALITY, DEFAULT_IGNORE_WHITE, DEFAULT_IGNORE_BLACK_LINES);
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     * 
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned (minimum 2, maximum 256)
     * @param quality
     *            1 is the highest quality settings. 10 is the default. There is a trade-off between
     *            quality and speed. The bigger the number, the faster the palette generation but
     *            the greater the likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     * 
     * @return the color map
     */
    public static CMap getColorMap(
            Bitmap sourceImage,
            Rect region,
            int colorCount,
            int quality,
            boolean ignoreWhite,
            boolean ignoreBlackLines) {
        if (colorCount < 2 || colorCount > 256) {
            throw new IllegalArgumentException("Specified colorCount must be between 2 and 256.");
        }
        if (quality < 1) {
            throw new IllegalArgumentException("Specified quality should be greater then 0.");
        }

        int[][] pixelArray;

        pixelArray = getPixelsFast(sourceImage, region, quality, ignoreWhite, ignoreBlackLines);
        // Send array to quantize function which clusters values using median cut algorithm
        CMap cmap = MMCQ.quantize(pixelArray, colorCount);
        return cmap;
    }

    /**
     * Gets the image's pixels via BufferedImage.getRaster().getDataBuffer(). Fast, but doesn't work
     * for all color models.
     * 
     * @param sourceImage
     *            the source image
     * @param quality
     *            1 is the highest quality settings. 10 is the default. There is a trade-off between
     *            quality and speed. The bigger the number, the faster the palette generation but
     *            the greater the likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     * 
     * @return an array of pixels (each an RGB int array)
     */
    private static int[][] getPixelsFast(
            Bitmap sourceImage,
            Rect region,
            int quality,
            boolean ignoreWhite,
            boolean ignoreBlackLines) {
        int width = region.right-region.left;
        int height = region.bottom-region.top;
        int[] pixels = new int[width*height];

        sourceImage.getPixels(pixels, 0, width, region.left, region.top, width, height);
        int pixelCount = width * height;


        // Store the RGB values in an array format suitable for quantize function

        // numRegardedPixels must be rounded up to avoid an ArrayIndexOutOfBoundsException if all
        // pixels are good.
        int numRegardedPixels = (pixelCount + quality - 1) / quality;

        int numUsedPixels = 0;
        int[][] pixelArray = new int[numRegardedPixels][];
        int offset, r, g, b, a;

        //bitmap generated by moodsync is ARGB 4 byte
        if (ignoreBlackLines) {
            offset = 0;
            for (int row = 0; row < height; row++){
                boolean rowIsBlack = true;
                int[][] rowPixelArray = new int[width][];
                int rowPixelsUsed=0;
                for (int column = 0; column < width; column++){

                    a = pixels[offset] >> 24 & 0xFF;
                    r = pixels[offset] >> 16 & 0xFF;
                    g = pixels[offset] >> 8 & 0xFF;
                    b = pixels[offset] & 0xff;

                    // If pixel is mostly opaque and not white
                    if (a >= 125 && !(ignoreWhite && r > 250 && g > 250 && b > 250)) {
                        rowPixelArray[rowPixelsUsed] = new int[]{r, g, b};
                        rowPixelsUsed++;
                        if (r > 5 || g > 5 || b > 5){
                            rowIsBlack = false;
                        }
                    }
                    offset++;
                }

                if (!rowIsBlack){
                    for (int i =0; i<rowPixelsUsed; i++) {
                        pixelArray[numUsedPixels] = rowPixelArray[i];
                        numUsedPixels++;
                    }
                }
            }
        } else {
            for (int i = 0; i < pixelCount; i += quality) {
                offset = i;

                a = pixels[offset] >> 24 & 0xFF;
                r = pixels[offset] >> 16 & 0xFF;
                g = pixels[offset] >> 8 & 0xFF;
                b = pixels[offset] & 0xff;

                // If pixel is mostly opaque and not white
                if (a >= 125 && !(ignoreWhite && r > 250 && g > 250 && b > 250)) {
                    pixelArray[numUsedPixels] = new int[]{r, g, b};
                    numUsedPixels++;
                }
            }
        }

        // Remove unused pixels from the array
        return Arrays.copyOfRange(pixelArray, 0, numUsedPixels);
    }

    /**
     * Gets the image's pixels via BufferedImage.getRGB(..). Slow, but the fast method doesn't work
     * for all color models.
     * 
     * @param sourceImage
     *            the source image
     * @param quality
     *            1 is the highest quality settings. 10 is the default. There is a trade-off between
     *            quality and speed. The bigger the number, the faster the palette generation but
     *            the greater the likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     * 
     * @return an array of pixels (each an RGB int array)
     */
    private static int[][] getPixelsSlow(
            Bitmap sourceImage,
            int quality,
            boolean ignoreWhite) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        int pixelCount = width * height;

        // numRegardedPixels must be rounded up to avoid an ArrayIndexOutOfBoundsException if all
        // pixels are good.
        int numRegardedPixels = (pixelCount + quality - 1) / quality;

        int numUsedPixels = 0;

        int[][] res = new int[numRegardedPixels][];
        int r, g, b;

        for (int i = 0; i < pixelCount; i += quality) {
            int row = i / width;
            int col = i % width;

            int pixel = sourceImage.getPixel(col,row);
            r=Color.red(pixel);
            g=Color.green(pixel);
            b=Color.blue(pixel);

            if (!(ignoreWhite && r > 250 && g > 250 && b > 250)) {
                res[numUsedPixels] = new int[] {r, g, b};
                numUsedPixels++;
            }
        }

        return Arrays.copyOfRange(res, 0, numUsedPixels);
    }
}
