package se.pjodd.glada.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author kalle
 * @since 2017-05-13 00:08
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Glada.db";

    private static final String SQL_CREATE_WIFI =
            "CREATE TABLE " + DatabaseContract.WiFiEntry.TABLE_NAME + " (" +
                    DatabaseContract.WiFiEntry._ID + " INTEGER PRIMARY KEY," +
                    DatabaseContract.WiFiEntry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    DatabaseContract.WiFiEntry.COLUMN_NAME_SSID + " TEXT," +
                    DatabaseContract.WiFiEntry.COLUMN_NAME_BSSID + " TEXT," +
                    DatabaseContract.WiFiEntry.COLUMN_NAME_DBM + " INTEGER," +
                    DatabaseContract.WiFiEntry.COLUMN_NAME_FREQUENCY + " INTEGER)";

    private static final String SQL_DELETE_WIFI =
            "DROP TABLE IF EXISTS " + DatabaseContract.WiFiEntry.TABLE_NAME;

    private static final String SQL_CREATE_LOCATION =
            "CREATE TABLE " + DatabaseContract.LocationEntry.TABLE_NAME + " (" +
                    DatabaseContract.LocationEntry._ID + " INTEGER PRIMARY KEY," +
                    DatabaseContract.LocationEntry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    DatabaseContract.LocationEntry.COLUMN_NAME_LATITUDE + " REAL," +
                    DatabaseContract.LocationEntry.COLUMN_NAME_LONGITUDE + " REAL," +
                    DatabaseContract.LocationEntry.COLUMN_NAME_ACCURACY + " REAL)";

    private static final String SQL_DELETE_LOCATION =
            "DROP TABLE IF EXISTS " + DatabaseContract.LocationEntry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_WIFI);
        db.execSQL(SQL_CREATE_LOCATION);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_WIFI);
        db.execSQL(SQL_DELETE_LOCATION);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
