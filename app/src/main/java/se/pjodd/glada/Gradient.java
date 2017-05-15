package se.pjodd.glada;

import android.graphics.Color;

/**
 * @author kalle
 * @since 2017-05-13 17:48
 */

public class Gradient {

    // 0x represents, this is an hexadecimal code
    // 55 represents percentage of transparency. For 100% transparency, specify 00.
    // For 0% transparency ( ie, opaque ) , specify ff
    // The remaining 6 characters(00ff00) specify the fill color

    public static final int[] TEN_RED_YELLOW_GREEN = new int[]{
            0x55ff0000,
            0x55ff3800,
            0x55ff7100,
            0x55ffaa00,
            0x55ffe200,
            0x55e2ff00,
            0x55a9ff00,
            0x5571ff00,
            0x5538ff00,
            0x5500ff00,
    };

    public static final int[] TEN_GREEN_YELLOW_RED = new int[]{

            0x5500ff00,
            0x5538ff00,
            0x5571ff00,
            0x55a9ff00,
            0x55e2ff00,
            0x55ffe200,
            0x55ffaa00,
            0x55ff7100,
            0x55ff3800,
            0x55ff0000,
    };

}
