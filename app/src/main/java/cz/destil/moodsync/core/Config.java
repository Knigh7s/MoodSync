package cz.destil.moodsync.core;

import com.lifx.Values.Levels;

/**
 * Global config which controls app behavior.
 *
 * @author David Vávra (david@vavra.me)
 */
public class Config {
    public static final int INITIAL_DELAY = 100; // in ms
    public static final int FINAL_DELAY = 100; // in ms
    public static final int FREQUENCE_OF_SCREENSHOTS = 50; // in ms
    public static final int DURATION_OF_COLOR_CHANGE = 400; // in ms
    public static final int LIFX_BRIGHTNESS = Levels.MAX; // 0-65535
    public static final int VIRTUAL_DISPLAY_WIDTH = 300; // in px - default of 150x84 is approximately the pixel count google's palette routine uses
    public static final int VIRTUAL_DISPLAY_HEIGHT = 169; // in px
    public static final int COLOR_REGION_LEFT = 0; //allows selecting a subset of the screen to get dominant color from
    public static final int COLOR_REGION_TOP = 0;
    public static final int COLOR_REGION_RIGHT = 300;
    public static final int COLOR_REGION_BOTTOM = 85;
    public static final String UNICAST_IP = "192.168.1.101"; //192.168.1.101
    public static final String BRIGHTNESS_STATIC = "static"; //use brightness defined in LIFX_BRIGHTNESS for lights
    public static final String BRIGHTNESS_COLOR = "color"; //use dominant color brightness for lights
    public static final String BRIGHTNESS_AVERAGE = "average"; //use image pseudo-average brightness for lights
    public static final String BRIGHTNESS_MODE = "average"; //current brightness mode to use
    public static final int MINIMUM_BRIGHTNESS = 2000; //0 - Levels.MAX,  used with REDUCE_DIM_LIGHT_CHANGES, lights will not dim below this brightness
}
