package se.torsteneriksson.timetogo.utilities;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.maps.android.geometry.Point;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import se.torsteneriksson.timetogo.MainActivity;
import se.torsteneriksson.timetogo.R;
import se.torsteneriksson.timetogo.database.TravelTimeDatabaseHelper;

import static se.torsteneriksson.timetogo.MainActivity.INTERVAL;


/**
 * Created by torsten on 11/11/2016.
 */

public class Utilities {
    private static final String TAG = "Utilities";
    static public boolean isTo() {
        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 0 && hour < 12;
    }

    public static Double getMaxY(Point[] list) {
        Double max = 0.0;
        for(int i = 0;i < list.length;i++) {
            if (list[i].y > max)
                max = list[i].y;
        }
        return max;
    }

    public static Address getLocationFromAddress(Context context, String strAddress) {

        Geocoder coder = new Geocoder(context);
        List<Address> address;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return null;
            }
            for(int i=0;i<address.size();i++) {
                Log.d(TAG, address.get(i).toString());
            }
            return address.get(0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Create a notification in the notification bar.
    static public void notify(Context context, String message) {
        Log.d(TAG,"notify");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Define sound URI
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent notificationIntent = new Intent(context, MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_timetogo_icon)
                .setContentTitle("TimeToGo")
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri); //This sets the sound to play

        // Display notification
        notificationManager.notify(0, mBuilder.build());
    }

    public static String getLastUpdatedTime(Cursor cursor) {
        if(cursor.getCount() > 0 ) {
            cursor.moveToFirst();
            long lastUpdated = cursor.getLong(cursor.getColumnIndex(TravelTimeDatabaseHelper.TIME));
            return getDateTime(lastUpdated);
        } else
            return ("");
    }

    public static String getDateTime(long time) {
        Date date = new Date(time);
        return DateFormat.getDateTimeInstance().format(date);
    }

    public static String getHourMin(long time) {
        Date date = new Date(time);
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
    }

    public static String getTravelTimeNoTraffic(Cursor dbCursor) {
        dbCursor.moveToFirst();
        String travelTimeNoTraffic = "";
        if(dbCursor.getCount() > 0) {
            Log.d(TAG, String.valueOf(dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TRAVELTIMENOTRAFFIC))));
            travelTimeNoTraffic =
                    String.valueOf(Math.round(Double.valueOf(dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TRAVELTIMENOTRAFFIC))) / 60.0));
            Log.d(TAG, travelTimeNoTraffic);
        }
        return travelTimeNoTraffic;
    }

    public static String getAverageInterval(Cursor dbCursor) {
        dbCursor.moveToLast();
        String result;
        long prevTime = 0;
        long currTime;
        double averageValue = INTERVAL;
        while (!dbCursor.isBeforeFirst()) {
            currTime = dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TIME));
            if(prevTime != 0) {
                averageValue = (averageValue + (currTime - prevTime))/2.0;
            }
            prevTime = currTime;
            dbCursor.moveToPrevious();
        }
        result = String.valueOf(averageValue/1000);
        return result;
    }

    public static Point[] getTravelData(Cursor dbCursor) {
        int count = dbCursor.getCount();
        dbCursor.moveToFirst();
        Point[] values = new Point[count];
        int x = 0;
        long prevX = 0;
        while(!dbCursor.isAfterLast()) {
            long y = Math.round(Double.valueOf(dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TRAVELTIME)))/60.0);
            long X = dbCursor.getLong(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TIME));
            Log.d(TAG,"Y:" + y);
            Log.d(TAG,"time:" + X/1000);
            long diff = (X-prevX)/1000;
            Log.d(TAG,"time-diff:" + diff);
            prevX = X;
            Point v = new Point (x, y);
            values[x] = v;
            x++;
            dbCursor.moveToNext();
        }
        return values;
    }

    public static long getCurrentDistance(Cursor dbCursor) {
        long result = 0;
        dbCursor.moveToFirst();
        if(!dbCursor.isAfterLast()) {
            result = dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.DISTANCE));
            Log.d(TAG, "distance:" + result);
        }
        return result;
    }


    public static long getCurrentTravelTime(Cursor dbCursor) {
        long result = 0;
        dbCursor.moveToFirst();
        if(!dbCursor.isAfterLast()) {
            result = Math.round(Double.valueOf(dbCursor.getInt(dbCursor.getColumnIndex(TravelTimeDatabaseHelper.TRAVELTIME))/60.0));
        }
        return result;
    }

    public static String formatAddress(Address placeAddress) {
        String lf = System.getProperty("line.separator");
        String address = placeAddress.getAddressLine(0);
        address = address.replaceFirst(", ",lf);

        for (int i = 1;i <= placeAddress.getMaxAddressLineIndex();i++) {
            if(placeAddress.getAddressLine(i) != null)
                address += ", " + placeAddress.getAddressLine(i);
        }

        return address;
    }

    public static String formatTravelTime(Context context, int minutes) {
        String result;
        int hours = minutes /60;
        minutes = minutes % 60;
        if(hours > 0) {
            result = context.getString(R.string.hours_minutes, hours, minutes);
        } else  {
            result = context.getString(R.string.minutes, minutes);
        }
        return result;

    }

    public static String formatDistance(Context context, int distance) {
        String result;
        int km = distance/1000;
        result = context.getString(R.string.distance_in_km, km);

        return result;

    }
}
