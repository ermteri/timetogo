package se.torsteneriksson.timetogo.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.android.gms.maps.model.LatLng;

import se.torsteneriksson.timetogo.MainActivity;
import static se.torsteneriksson.timetogo.MainActivity.FIRST_DB_KEY_ID;
import static se.torsteneriksson.timetogo.MainActivity.SECOND_DB_KEY_ID;

/**
 * Created by torsten on 3/28/2017.
 */



public class PreviousAddressesDatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "se.torsteneriksson.timetogo.previousaddresses";
    private static final int DB_VERSION = 1;
    public static final String PREV_ADDRESS_TABLE = "prev_address_table";
    public static final String ADDRESS = "address";
    public static final String KEY_ID = "_id";
    public static final String WEIGHT = "weight";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";

    public PreviousAddressesDatabaseHelper(Context context) {
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

    private static void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion < 1) {
            db.execSQL("CREATE TABLE " + PREV_ADDRESS_TABLE + "("
                    + KEY_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + ADDRESS   + " STRING, "
                    + WEIGHT    + " INTEGER, "
                    + LATITUDE  + " DOUBLE, "
                    + LONGITUDE + " DOUBLE);");
        }
    }

    public static Cursor getAllRecords(SQLiteDatabase db) {
        return db.query(PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,
                new String[] {
                        PreviousAddressesDatabaseHelper.KEY_ID,
                        PreviousAddressesDatabaseHelper.ADDRESS,
                        PreviousAddressesDatabaseHelper.LATITUDE,
                        PreviousAddressesDatabaseHelper.LONGITUDE},
                null, null, null, null,
                PreviousAddressesDatabaseHelper.KEY_ID + " DESC", null);
    }

    public static Cursor getOneRecord(SQLiteDatabase db, long keyId) {
        Cursor cursor = db.query(true,
                PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,
                new String[]{
                        PreviousAddressesDatabaseHelper.KEY_ID,
                        PreviousAddressesDatabaseHelper.ADDRESS,
                        PreviousAddressesDatabaseHelper.LATITUDE,
                        PreviousAddressesDatabaseHelper.LONGITUDE},
                PreviousAddressesDatabaseHelper.KEY_ID+"=?",
                new String[]{String.valueOf(keyId)},
                null,
                null,
                null,
                null);
        return cursor;
    }

    public static void insertUniqely(SQLiteDatabase db, ContentValues cv) {
        String address = cv.get(PreviousAddressesDatabaseHelper.ADDRESS).toString();
        long id = isAddressExist(db,address);
        if(id != -1) {
            deleteOneRecord(db,id);
        }
        db.insert(PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE, null, cv);
        deleteFirstRow(db);
    }

    public static void deleteAllRecords(SQLiteDatabase db) {
        db.delete(PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,null,null);
    }

    private static void deleteFirstRow(SQLiteDatabase db) {
        Cursor cursor = db.query(PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE, null, null, null, null, null, null);
        if(cursor.getCount() > MainActivity.PREVIOUS_ADDRESSES_SIZE) {
            if (cursor.moveToFirst()) {
                String rowId = cursor.getString(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.KEY_ID));
                db.delete(PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,
                        PreviousAddressesDatabaseHelper.KEY_ID + "=?", new String[]{rowId});
            }
        }
        cursor.close();
    }

    public static void deleteOneRecord(SQLiteDatabase db, long keyId) {
        db.delete(
                PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,
                PreviousAddressesDatabaseHelper.KEY_ID+"=?",
                new String[]{String.valueOf(keyId)});
    }

    public static LatLng getLatLngFromDB(SQLiteDatabase db, SharedPreferences settings, boolean isFirst) {
        double latitude = 0;
        double longitude = 0;
        Cursor cursor;
        if(isFirst) {
            cursor = PreviousAddressesDatabaseHelper.getOneRecord(db, settings.getLong(FIRST_DB_KEY_ID, -1));
        } else {
            cursor = PreviousAddressesDatabaseHelper.getOneRecord(db, settings.getLong(SECOND_DB_KEY_ID, -1));
        }

        cursor.moveToFirst();
        if(cursor.getCount() > 0) {
            latitude  = cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LATITUDE));
            longitude = cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LONGITUDE));
        }
        cursor.close();
        if(latitude != 0 && longitude != 0) {
            return new LatLng(latitude,longitude);
        } else {
            return null;
        }
    }

    public static String getAddressFromDB(SQLiteDatabase db, SharedPreferences settings, boolean isFirst) {
        String address = "";
        Cursor cursor;
        if(isFirst) {
            cursor = PreviousAddressesDatabaseHelper.getOneRecord(db, settings.getLong(FIRST_DB_KEY_ID, -1));
        } else {
            cursor = PreviousAddressesDatabaseHelper.getOneRecord(db, settings.getLong(SECOND_DB_KEY_ID, -1));
        }

        cursor.moveToFirst();
        if(cursor.getCount() > 0) {
            address  = cursor.getString(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.ADDRESS));
        }
        cursor.close();
        address = address.replaceAll(System.getProperty("line.separator"),",");
        if(!address.isEmpty()) {
            return address;
        } else {
            return null;
        }
    }

    public static long isAddressExist(SQLiteDatabase db, String address) {
        long result;
        Cursor cursor = db.query(true,
                PreviousAddressesDatabaseHelper.PREV_ADDRESS_TABLE,
                new String[]{PreviousAddressesDatabaseHelper.ADDRESS, PreviousAddressesDatabaseHelper.KEY_ID},
                PreviousAddressesDatabaseHelper.ADDRESS+"=?",
                new String[]{address},
                null,
                null,
                null,
                null);
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            result = cursor.getLong(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.KEY_ID));
        }
        else
            result = -1;
        cursor.close();
        return result;
    }


}
