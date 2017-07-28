package se.torsteneriksson.timetogo;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

import se.torsteneriksson.timetogo.database.PreviousAddressesDatabaseHelper;
import se.torsteneriksson.timetogo.database.TravelTimeDatabaseHelper;
import se.torsteneriksson.timetogo.utilities.Utilities;

import static android.content.Context.NOTIFICATION_SERVICE;
import static se.torsteneriksson.timetogo.MainActivity.AVOID;
import static se.torsteneriksson.timetogo.MainActivity.CURRENT_STATUS;
import static se.torsteneriksson.timetogo.MainActivity.HISTORY_SIZE;
import static se.torsteneriksson.timetogo.MainActivity.INTERVAL;
import static se.torsteneriksson.timetogo.MainActivity.NOTIFICATION_ID;
import static se.torsteneriksson.timetogo.MainActivity.PREFS_NAME;
import static se.torsteneriksson.timetogo.MainActivity.RUN;

/**
 * Created by torsten on 2/15/2017.
 *
 */

public class AlarmReceiver extends BroadcastReceiver {


    private static final String TAG = "AlarmReceiver";
    private Context mContext;
    private String mAvoid;
    private Location mOrigin;
    private Location mDestination;
    private String mOriginAsString;
    private String mDestinationAsString;
    private SharedPreferences mSettings;
    private SQLiteDatabase mPrevAddrDb;
    private NotificationManager mNotifyMgr;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive:" + intent.getAction());
        mContext = context;
        SQLiteOpenHelper previousAddressesDatabaseHelper = new PreviousAddressesDatabaseHelper(context);
        mPrevAddrDb = previousAddressesDatabaseHelper.getReadableDatabase();
        mSettings = context.getSharedPreferences(PREFS_NAME,0);

        if(!mSettings.getBoolean(RUN,false))
            return;
        mNotifyMgr = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        am.cancel(pendingIntent);
        int ALARM_TYPE = AlarmManager.RTC_WAKEUP;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            am.setExactAndAllowWhileIdle(ALARM_TYPE, System.currentTimeMillis() + INTERVAL, pendingIntent);
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            am.setExact(ALARM_TYPE, System.currentTimeMillis() + INTERVAL, pendingIntent);
        else
            am.set(ALARM_TYPE, System.currentTimeMillis() + INTERVAL, pendingIntent);
        updateTravelTimeData(mSettings);
        getCurrentTravelTime();
        showRunNotification(true);
    }

    private void updateTravelTimeData(SharedPreferences settings) {
        boolean isTo = settings.getBoolean("ISTO",false);
        Double origLatitude;
        Double origLongitude;
        Double destinationLatitude;
        Double destinationLongitude;
        if(isTo) {
            LatLng latLng = PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb,mSettings,true);
            origLatitude = latLng.latitude;
            origLongitude = latLng.longitude;
            latLng = PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb,mSettings,false);
            destinationLatitude = latLng.latitude;
            destinationLongitude = latLng.longitude;
            mOriginAsString      = PreviousAddressesDatabaseHelper.getAddressFromDB(mPrevAddrDb,mSettings,true);
            mDestinationAsString = PreviousAddressesDatabaseHelper.getAddressFromDB(mPrevAddrDb,mSettings,false);
        } else {
            LatLng latLng = PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb,mSettings,false);
            origLatitude = latLng.latitude;
            origLongitude = latLng.longitude;
            latLng = PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb,mSettings,true);
            destinationLatitude = latLng.latitude;
            destinationLongitude = latLng.longitude;
            mOriginAsString      = PreviousAddressesDatabaseHelper.getAddressFromDB(mPrevAddrDb,mSettings,false);
            mDestinationAsString = PreviousAddressesDatabaseHelper.getAddressFromDB(mPrevAddrDb,mSettings,true);
        }
        mAvoid = settings.getString(AVOID,"");
        Log.d(TAG,"origLat" + origLatitude);
        Log.d(TAG,"origLong" + origLongitude);
        Log.d(TAG,"destLat" + destinationLatitude);
        Log.d(TAG,"destLong" + destinationLongitude);
        Log.d(TAG, "mAvoid:" + mAvoid);
        mOrigin = getLocation(origLatitude, origLongitude);
        mDestination = getLocation(destinationLatitude, destinationLongitude);
    }

    private void getCurrentTravelTime() {
        new AlarmReceiver.TravelTime().execute(mOrigin, mDestination, mAvoid, "driving");
        //new AlarmReceiver.TravelTime().execute(mOriginAsString, mDestinationAsString, mAvoid, "driving");
    }

    private Location getLocation(double latitude, double longitude) {
        Log.d(TAG,"getLocation");
        final Location location = new Location("dummy");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return  location;
    }

    private class TravelTime extends AsyncTask<Object, Void, String[]> {
        protected String[] doInBackground(Object... params) {
            Log.d(TAG, "doInBackground");
            TravelTimeChecker ttc = new TravelTimeChecker(mContext);
            //return ttc.searchPlaces((String) params[0], (String) params[1], (String) params[2], (String) params[3]);
            return ttc.searchPlaces((Location) params[0], (Location) params[1], (String) params[2], (String) params[3]);
        }

        protected void onPostExecute(String[] result) {
            Log.d(TAG,"onPostExecute");
            String durationInTraffic = "";
            String durationNoTraffic = "";
            String distance = "";
            if(result.length >= 3) {
                durationInTraffic = result[0];
                durationNoTraffic = result[1];
                distance = result[2];
            }
            if(durationInTraffic.matches("\\d+") && durationNoTraffic.matches("\\d+")) {
                updateDb(Integer.valueOf(durationInTraffic), Integer.valueOf(durationNoTraffic), Integer.valueOf(distance));
            } else {
                Log.d(TAG, "Result was not digits[" + durationInTraffic + "]");
                if(result.length > 0) {
                    Toast.makeText(mContext, "Duration check failed:[" + result[0] + "]", Toast.LENGTH_LONG).show();
                }
                stopExecution();
                notifyGui();
            }
        }
    }

    private void updateDb(Integer traveltime, Integer traveltimeNoTraffic, Integer distance) {
        Log.d(TAG, "updateDb" + ":" + traveltime + "," + traveltimeNoTraffic + "," + distance);

        SQLiteOpenHelper timeToGoDatabaseHelper = new TravelTimeDatabaseHelper(mContext);
        SQLiteDatabase db = timeToGoDatabaseHelper.getWritableDatabase();

        Date now = new Date();

        ContentValues locations = new ContentValues();
        locations.put(TravelTimeDatabaseHelper.TIME, now.getTime());
        locations.put(TravelTimeDatabaseHelper.ORIGLATITUDE, mOrigin.getLatitude());
        locations.put(TravelTimeDatabaseHelper.ORIGLONGITUDE, mOrigin.getLongitude());
        locations.put(TravelTimeDatabaseHelper.DESTLATITUDE, mDestination.getLatitude());
        locations.put(TravelTimeDatabaseHelper.DESTLONGITUDE, mDestination.getLongitude());
        locations.put(TravelTimeDatabaseHelper.TRAVELTIME, traveltime);
        locations.put(TravelTimeDatabaseHelper.TRAVELTIMENOTRAFFIC, traveltimeNoTraffic);
        Log.d(TAG, "TravelTime:" + traveltime+ "," + "w/o traffic:" + traveltimeNoTraffic);
        locations.put(TravelTimeDatabaseHelper.AVOID, mAvoid);
        locations.put(TravelTimeDatabaseHelper.DISTANCE, distance);
        long key_id = db.insert(TravelTimeDatabaseHelper.TIMETOGO, null, locations);
        notifyGui();
        deleteFirstRow(db);
        db.close();
    }

    private void deleteFirstRow(SQLiteDatabase db) {
        Cursor cursor = db.query(TravelTimeDatabaseHelper.TIMETOGO, null, null, null, null, null, null);
        if(cursor.getCount() > HISTORY_SIZE) {
            stopExecution();
            cursor.close();
            Utilities.notify(mContext,mContext.getString(R.string.app_name) + " " +
                    mContext.getString(R.string.app_finished));
            db.close();
            showRunNotification(false);
        }
            /*
            if (cursor.moveToFirst()) {
                String rowId = cursor.getString(cursor.getColumnIndex("_id"));
                db.delete(TravelTimeDatabaseHelper.TIMETOGO,
                        TravelTimeDatabaseHelper.KEY_ID + "=?", new String[]{rowId});
                Log.d(TAG, "One row deleted");
            }
        }
        cursor.close(); */
    }

    private void stopExecution() {
        Log.d(TAG,"stopExecution");
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(RUN, false);
        editor.apply();
    }

    private void notifyGui() {
        Log.d(TAG, "notifyGui");
        Intent intent = new Intent(CURRENT_STATUS);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private void showRunNotification(boolean show) {
        if(show) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                    .setSmallIcon(R.drawable.ic_timetogo_icon)
                    .setContentTitle(mContext.getString(R.string.app_name) + " " + mContext.getString(R.string.is_running));
            Intent resultIntent = new Intent(mContext, MainActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(
                    mContext,
                    0,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            Notification notification = mBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            mNotifyMgr.notify(NOTIFICATION_ID, notification);
        } else {
            mNotifyMgr.cancel(NOTIFICATION_ID);
        }
    }
}
