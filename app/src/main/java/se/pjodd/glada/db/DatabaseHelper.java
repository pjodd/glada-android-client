package se.pjodd.glada.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author kalle
 * @since 2017-05-13 00:08
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "Glada.db";

    private static final String SQL_CREATE_ENTRY =
            "CREATE TABLE " + DatabaseContract.Entry.TABLE_NAME + " (" +
                    DatabaseContract.Entry._ID + " INTEGER PRIMARY KEY," +
                    DatabaseContract.Entry.COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    DatabaseContract.Entry.COLUMN_NAME_SSID + " TEXT," +
                    DatabaseContract.Entry.COLUMN_NAME_BSSID + " TEXT," +
                    DatabaseContract.Entry.COLUMN_NAME_DBM + " INTEGER," +
                    DatabaseContract.Entry.COLUMN_NAME_FREQUENCY + " INTEGER," +
                    DatabaseContract.Entry.COLUMN_NAME_LATITUDE + " REAL," +
                    DatabaseContract.Entry.COLUMN_NAME_LONGITUDE + " REAL," +
                    DatabaseContract.Entry.COLUMN_NAME_ACCURACY + " REAL," +
                    DatabaseContract.Entry.COLUMN_NAME_PDOP + " REAL)";

    private static final String SQL_DELETE_ENTRY =
            "DROP TABLE IF EXISTS " + DatabaseContract.Entry.TABLE_NAME;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_ENTRY);
        db.execSQL(SQL_CREATE_ENTRY);
    }


    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRY);
        db.execSQL(SQL_CREATE_ENTRY);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

}
