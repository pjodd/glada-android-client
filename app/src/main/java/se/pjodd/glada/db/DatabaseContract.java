package se.pjodd.glada.db;

import android.provider.BaseColumns;

/**
 * @author kalle
 * @since 2017-05-13 00:03
 */

public class DatabaseContract {

    private DatabaseContract() {
    }

    public static class Entry implements BaseColumns {
        public static final String TABLE_NAME = "entry";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_SSID = "ssid";
        public static final String COLUMN_NAME_BSSID = "bssid";
        public static final String COLUMN_NAME_DBM = "dbm";
        public static final String COLUMN_NAME_FREQUENCY = "frequency";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_ACCURACY = "accuracy";
        public static final String COLUMN_NAME_PDOP = "pdop";
    }

}

