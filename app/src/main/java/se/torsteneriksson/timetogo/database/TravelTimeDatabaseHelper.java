package se.torsteneriksson.timetogo.database;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by torsten on 2015-08-10.
 */
public class TravelTimeDatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "se.torsteneriksson.timetogo";
    private static final int DB_VERSION = 2;
    public static final String TIMETOGO = "TIMETOGO";
    public static final String KEY_ID = "_id";
    public static final String TIME = "TIME";
    public static final String ORIGLATITUDE= "ORIGLATITUDE";
    public static final String ORIGLONGITUDE = "ORIGLONGITUDE";
    public static final String DESTLATITUDE= "DESTLATITUDE";
    public static final String DESTLONGITUDE = "DESTLONGITUDE";
    public static final String TRAVELTIME = "TRAVELTIME";
    public static final String TRAVELTIMENOTRAFFIC = "TRAVELTIMENOTRAFFIC";
    public static final String AVOID = "AVOID";
    public static final String DISTANCE = "DISTANCE";
    private static String TAG = "TrackerDatabaseHelper";

    public TravelTimeDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        updateDatabase(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        updateDatabase(db,oldVersion, newVersion);
    }

    private static void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 1) {
            db.execSQL("CREATE TABLE " + TIMETOGO + "("
                    + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + TIME + " INTEGER,"
                    //+ TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                    + ORIGLATITUDE + " DOUBLE, "
                    + ORIGLONGITUDE + " DOUBLE, "
                    + DESTLATITUDE + " DOUBLE, "
                    + DESTLONGITUDE + " DOUBLE, "
                    + TRAVELTIME + " INTEGER,"
                    + TRAVELTIMENOTRAFFIC + " INTEGER,"
                    + AVOID + " STRING,"
                    + DISTANCE + " INTEGER DEFAULT 0);");
        } else if(oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TIMETOGO + " ADD COLUMN " + DISTANCE + " INTEGER DEFAULT 0");
        }
    }

    public static Cursor getAllRecords(SQLiteDatabase db) {
        return db.query(TravelTimeDatabaseHelper.TIMETOGO,
                new String[] {
                        TravelTimeDatabaseHelper.KEY_ID,
                        TravelTimeDatabaseHelper.TIME,
                        TravelTimeDatabaseHelper.ORIGLATITUDE,
                        TravelTimeDatabaseHelper.ORIGLONGITUDE,
                        TravelTimeDatabaseHelper.DESTLATITUDE,
                        TravelTimeDatabaseHelper.DESTLONGITUDE,
                        TravelTimeDatabaseHelper.TRAVELTIME,
                        TravelTimeDatabaseHelper.TRAVELTIMENOTRAFFIC,
                        TravelTimeDatabaseHelper.AVOID,
                        TravelTimeDatabaseHelper.DISTANCE},
                null, null, null, null,
                TravelTimeDatabaseHelper.KEY_ID + " DESC", null);
    }
}
