package se.torsteneriksson.timetogo;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Address;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.geometry.Point;

import org.w3c.dom.Text;

import java.net.URI;
import java.net.URL;

import se.torsteneriksson.timetogo.database.PreviousAddressesDatabaseHelper;
import se.torsteneriksson.timetogo.database.TravelTimeDatabaseHelper;
import se.torsteneriksson.timetogo.graph.LineGraphJJOE64;
import se.torsteneriksson.timetogo.utilities.Utilities;

import static java.security.AccessController.getContext;


public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private LineGraphJJOE64 mLineGraph;

    // Saved preferences
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String AVOID = "AVOID1";
    private static final String ISTO = "ISTO";
    public static final String RUN =  "RUN1";
    public static final String  FIRST_DB_KEY_ID = "first_db_key_id";
    public static final String  SECOND_DB_KEY_ID = "second_db_key_id";


    public static final int HISTORY_SIZE = 25;
    public static final int INTERVAL = 300000; // in seconds
    public static final double DEFAULT_MAXY = 60.0;
    private static final double PLACE_PICKER_RADIUS = 100.0;
    public static final int PREVIOUS_ADDRESSES_SIZE = 15;

    // Broadcast recevier stuff
    public static final String CURRENT_STATUS = "CURRENT_STATUS";

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;


    private static final String TAG = "MainActivity" ;

    public static final int NOTIFICATION_ID = 4711;

    // Member variables
    private boolean mIsTo = true;
    private String mAvoid = "tolls";
    private SQLiteDatabase mTravelDataDb;
    private SQLiteDatabase mPrevAddrDb = null;
    SharedPreferences mSettings = null;

    private boolean mIsFirstSelected = false;
    private String mGraphTitle = "";
    private SimpleCursorAdapter mFirstSpinnerCursorAdapter = null;
    private SimpleCursorAdapter mSecondSpinnerCursorAdapter = null;

    private Spinner mSp_firstPreviousAddresses;
    private Spinner mSp_secondPreviousAddresses;

    private AlarmManager mManager;
    private Toast mToast = null;

    // AppCompatActivity interface
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        SQLiteOpenHelper trackerDatabaseHelper = new TravelTimeDatabaseHelper(this);
        mTravelDataDb = trackerDatabaseHelper.getReadableDatabase();

        SQLiteOpenHelper previousAddressesDatabaseHelper = new PreviousAddressesDatabaseHelper(this);
        mPrevAddrDb = previousAddressesDatabaseHelper.getWritableDatabase();

        mSp_firstPreviousAddresses = (Spinner)findViewById(R.id.first_previous);
        mSp_secondPreviousAddresses = (Spinner)findViewById(R.id.second_previous);

        mSettings = getSharedPreferences(PREFS_NAME, 0);

        mIsTo = mSettings.getBoolean(ISTO, true);
        mAvoid = mSettings.getString(AVOID, "tolls");
        Log.d(TAG,"mIsTo:" + mIsTo);

        boolean isOn = mSettings.getBoolean(RUN,false);
        if(isOn) {
            Log.d(TAG,"Service is running");
            setViewsEnabled(false);
        } else {
            setViewsEnabled(true);
        }

        //mLineGraph = new LineGraphJJOE64(this, this, R.id.graph);
        FrameLayout container = (FrameLayout)findViewById(R.id.graphcontainer);
        mLineGraph = new LineGraphJJOE64(this, container);
        mLineGraph.setMinX(0.0);
        mLineGraph.setMaxX(HISTORY_SIZE *1.0);
        mLineGraph.setMinY(0.0);
        mLineGraph.setMaxY(DEFAULT_MAXY);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        setFirstPreviousAddressSpinners();
        setSecondPreviousAddressSpinners();
        showLastValue(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG,"onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timetogo, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected");
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        checkStartButton();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(CURRENT_STATUS));
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean isOn = settings.getBoolean(RUN,false);
        updateMainLayout(isOn);
    }

    @Override
    protected void onStop() {
        Log.d(TAG,"onStop");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        // Save preferences
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(ISTO,mIsTo);
        editor.putString(AVOID, mAvoid);
        editor.apply();
        if(mFirstSpinnerCursorAdapter!= null) {
            mFirstSpinnerCursorAdapter.getCursor().close();
        }
        if(mSecondSpinnerCursorAdapter != null) {
            mFirstSpinnerCursorAdapter.getCursor().close();
        }
        mTravelDataDb.close();
        mPrevAddrDb.close();
        // Unregister since the activity is about to be closed.
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
    //----------------------------------------------------------------------------------------------

    // SharedPreference interface
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,"onSharedPreferenceChanged");
        getPreferences();
    }

    private void getPreferences() {
        Log.d(TAG,"getPreferences");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPref.getBoolean(getString(R.string.pref_avoid_toll_key),
                getResources().getBoolean(R.bool.pref_avoid_toll_default))) {
            mAvoid = "tolls";

        } else {
            mAvoid = "";
        }
        Log.d(TAG, "mAvoid:" + mAvoid);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME,0);
        if( prefs.getBoolean(RUN,false)) {
            if (mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, getString(R.string.changes_takes_effect), Toast.LENGTH_LONG);
            mToast.show();
        }
    }
    //----------------------------------------------------------------------------------------------

    // BroadcastReceiver interface
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"onReceive" + intent.getAction());
            showLastValue(true);
            if(intent.getAction().equals(CURRENT_STATUS)) {
                if(mSettings.getBoolean(RUN,false)) {
                    updateGui();
                } else {
                    stop();
                    updateMainLayout(false);
                }
            }
        }
    };
    //----------------------------------------------------------------------------------------------

    // Interface to activity GUI
    public void onSwitchTravelCheckerServiceClicked(View view) {
        Log.d(TAG,"onSwitchTravelCheckerServiceClicked");
        boolean on = ((ToggleButton) view).isChecked();
        if (on) {
            start();
        } else {
            stop();
        }
    }

    public void onSwitchDirectionClicked(View view) {
        Log.d(TAG,"onSwitchDirectionClicked");
        mIsTo = !((ToggleButton) view).isChecked();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(ISTO,mIsTo);
        editor.apply();
    }

    public void onButtonClearHistoryClicked(View view) {
        Log.d(TAG,"onButtonClearHistoryClicked");
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        // Setting Alert Dialog Title
        alertDialogBuilder.setTitle(getString(R.string.clear_history_prompt));
        // Icon Of Alert Dialog
        alertDialogBuilder.setIcon(R.drawable.ic_delete_red);
        // Setting Alert Dialog Message
        alertDialogBuilder.setMessage(getString(R.string.clear_history_prompt_message));
        alertDialogBuilder.setCancelable(true);
        alertDialogBuilder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                clearHistory();
                clearAdresses();
                Toast.makeText(MainActivity.this,getString(R.string.clear_history_ok),Toast.LENGTH_SHORT).show();
                //finish();
            }
        });

        alertDialogBuilder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                Toast.makeText(MainActivity.this,getString(R.string.clear_history_cancelled),Toast.LENGTH_SHORT).show();
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void onButtonFirstUseMapClicked(View view) {
        Log.d(TAG,"onButtonFirstUseMapClicked");
        mIsFirstSelected = true;

        startAutoCompleteActivity(true, PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb,mSettings,true));
    }

    public void onButtonSecondUseMapClicked(View view) {
        Log.d(TAG,"onButtonSecondUseMapClicked");
        mIsFirstSelected = false;
        startAutoCompleteActivity(true, PreviousAddressesDatabaseHelper.getLatLngFromDB(mPrevAddrDb, mSettings,false));
    }

    public void onDeleteFirstAddressClicked(View view) {
        Log.d(TAG,"onDeleteFirstAddressClicked");
        long keyId = mSp_firstPreviousAddresses.getSelectedItemId();
        PreviousAddressesDatabaseHelper.deleteOneRecord(mPrevAddrDb, keyId);
        updatesSpinnersAfterDelete();
        showSpinner(true);
        checkStartButton();
    }

    public void onDeleteSecondAddressClicked(View view) {
        Log.d(TAG,"onDeleteSecondAddressClicked");
        long keyId = mSp_secondPreviousAddresses.getSelectedItemId();
        PreviousAddressesDatabaseHelper.deleteOneRecord(mPrevAddrDb,keyId);
        updatesSpinnersAfterDelete();
        showSpinner(false);
        checkStartButton();
    }

    public void updatesSpinnersAfterDelete() {
        Log.d(TAG,"updateSpinnersAfterDelete");

        Cursor cursor1 = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
        cursor1.moveToFirst();
        mFirstSpinnerCursorAdapter.swapCursor(cursor1).close();
        mSp_firstPreviousAddresses.setSelection(0);
        SharedPreferences.Editor editor = mSettings.edit();
        if(cursor1.getCount() > 0) {
            editor.putLong(FIRST_DB_KEY_ID,mSp_firstPreviousAddresses.getSelectedItemId());
            editor.apply();
            Log.d(TAG,"FIRST_DB_KEY_ID:" + mSettings.getLong(FIRST_DB_KEY_ID,-1));
        } else {
            editor.remove(FIRST_DB_KEY_ID);
            editor.apply();
        }

        Cursor cursor2 = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
        cursor2.moveToFirst();
        editor = mSettings.edit();
        mSecondSpinnerCursorAdapter.swapCursor(cursor2).close();
        mSp_secondPreviousAddresses.setSelection(0);
        if(cursor2.getCount() > 0) {
            editor.putLong(SECOND_DB_KEY_ID,mSp_secondPreviousAddresses.getSelectedItemId());
            editor.apply();
            Log.d(TAG,"SECOND_DB_KEY_ID:" + mSettings.getLong(SECOND_DB_KEY_ID,-1));
        } else {
            editor.remove(SECOND_DB_KEY_ID);
            editor.apply();
        }
    }


    public void onMapClicked_streetView(View view) {
        Log.d(TAG,"onMapClicked_streetView");
        Uri gmmIntentUri = Uri.parse("google.streetview:cbll=46.414382,10.013988");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void onMapDirectionsClicked(View view) {
        Log.d(TAG,"onMapDirectionsClicked");
        String dirflag = "dt";
        if(mAvoid.equals("")) {
            dirflag ="d";
        }
        Log.d(TAG,"dirflag:" + dirflag);
        findDirections(dirflag);
    }

    public void onMapPublicTransportationsClicked(View view) {
        Log.d(TAG,"onMapDirectionsClicked");
        findDirections("r");
    }

    private void findDirections(String directionMode) {
        Log.d(TAG,"findDirections");
        Cursor cursor = PreviousAddressesDatabaseHelper.getOneRecord(mPrevAddrDb,mSettings.getLong(FIRST_DB_KEY_ID,-1));
        if(cursor.getCount() == 0) {
            if(mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, getString(R.string.address_empty), Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        cursor.moveToFirst();
        String first = cursor.getString(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.ADDRESS));
        first = first.replaceAll(System.getProperty("line.separator"),",");
        cursor.close();
        cursor = PreviousAddressesDatabaseHelper.getOneRecord(mPrevAddrDb,mSettings.getLong(SECOND_DB_KEY_ID,-1));
        if(cursor.getCount() == 0) {
            if(mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, getString(R.string.address_empty), Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        cursor.moveToFirst();
        String second = cursor.getString(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.ADDRESS));
        second = second.replaceAll(System.getProperty("line.separator"),",");
        cursor.close();
        String dirflag = directionMode;
        Uri request = null;
        if(mIsTo) {
            request = Uri.parse("http://maps.google.com/maps?" +
                    "&saddr=" + Uri.encode(first) +
                    "&daddr=" + Uri.encode(second) +
                    "&dirflg=" + dirflag);
        } else {
            request = Uri.parse("http://maps.google.com/maps?" +
                    "&saddr=" + Uri.encode(second) +
                    "&daddr=" + Uri.encode(first) +
                    "&dirflg=" + dirflag);
        }
        Log.d(TAG,"Directions request:"+request);

        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, request);
        startActivity(intent);
    }

    // Directions with coordinates. This one fails sometimes
    public void onMapClicked1(View view) {
        Log.d(TAG,"onMapClicked1");
        if(!isAddressSet()) {
            if(mToast != null)
                mToast.cancel();
            mToast = Toast.makeText(this, getString(R.string.address_empty), Toast.LENGTH_SHORT);
            mToast.show();
            return;
        }
        String first_latitude   = "";
        String first_longitude  = "";
        String second_latitude  = "";
        String second_longitude = "";

        Cursor cursor = PreviousAddressesDatabaseHelper.getOneRecord(mPrevAddrDb,mSettings.getLong(FIRST_DB_KEY_ID,-1));
        if(cursor.getCount()>0) {
            cursor.moveToFirst();
            first_latitude = String.valueOf(cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LATITUDE)));
            first_longitude = String.valueOf(cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LONGITUDE)));
        }
        cursor.close();
        cursor = PreviousAddressesDatabaseHelper.getOneRecord(mPrevAddrDb,mSettings.getLong(SECOND_DB_KEY_ID,-1));
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            second_latitude = String.valueOf(cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LATITUDE)));
            second_longitude = String.valueOf(cursor.getDouble(cursor.getColumnIndex(PreviousAddressesDatabaseHelper.LONGITUDE)));
        }
        cursor.close();

        String dirflag = "dt";
        if(mAvoid.equals("")) {
            dirflag ="d";
        }
        String request;
        if(mIsTo) {
            request = "http://maps.google.com/maps?" +
                    "&saddr=" +
                    first_latitude + "," + first_longitude +
                    "&daddr=" +
                    second_latitude + "," + second_longitude +
                    "&dirflg=" + dirflag;
        } else {
            request = "http://maps.google.com/maps?" +
                    "&daddr=" +
                    first_latitude + "," + first_longitude +
                    "&saddr=" +
                    second_latitude + "," + second_longitude +
                    "&dirflg=" + dirflag;
        }
        Log.d(TAG,"Directions request:"+request);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse(request));
        startActivity(intent);
    }
    //----------------------------------------------------------------------------------------------

    // Private methods

    //----------------------------------------------------------------------------------------------

    // Google Autocomplete interface
    private void startAutoCompleteActivity(boolean useMaps, LatLng center) {
        Log.d(TAG,"startAutoCompleteActivity");
        try {
            if(useMaps) {
                int PLACE_PICKER_REQUEST = 1;
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                if(center != null) {
                    builder.setLatLngBounds(latLngToBounds(center,PLACE_PICKER_RADIUS));
                }
                startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);

            } else {
                Intent intent =
                        new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                .build(this);
                startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
            }
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult");
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.d(TAG, "Place: " + place.getName() + place.getAddress());
                updateLocationInformation(place, mIsFirstSelected);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.d(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }
    //----------------------------------------------------------------------------------------------

    // Private methods
    private void updateLocationInformation(Place place, boolean isFirstSelected) {
        Log.d(TAG,"updateLocationInformation");
        Address placeAddress;
        if(place != null) {
            placeAddress = Utilities.getLocationFromAddress(this, place.getAddress().toString());
            if(placeAddress == null)
                return;
        } else {
            return;
        }
        Log.d(TAG,"place.getAddress:" + place.getAddress().toString());
        String address = Utilities.formatAddress(placeAddress);
        Log.d(TAG, "address:" + address);

        if(isFirstSelected)
            Log.d(TAG, "First: " + "," + placeAddress.getLatitude() + "," + placeAddress.getLongitude());
        else
            Log.d(TAG,"Second:" +  placeAddress.getLatitude()+","+placeAddress.getLongitude());
        Log.d(TAG,"http://maps.google.com?q="+placeAddress.getLatitude()+","+placeAddress.getLongitude());

        storePreviousAddress(address, placeAddress);
        // Select the latest
        if(isFirstSelected) {
            mSp_firstPreviousAddresses.setSelection(0);
            storeSelectedAddressToSettings(FIRST_DB_KEY_ID, mSp_firstPreviousAddresses.getSelectedItemId());
            showSpinner(true);
        } else {
            mSp_secondPreviousAddresses.setSelection(0);
            storeSelectedAddressToSettings(SECOND_DB_KEY_ID, mSp_secondPreviousAddresses.getSelectedItemId());
            showSpinner(false);
        }
        updateGui();
    }

    private void storeSelectedAddressToSettings(String key,long id) {

        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(key,id);
        editor.apply();
    }

    private void storePreviousAddress(String addressAsString, Address address) {
        Log.d(TAG,"storePreviousAddress");
        ContentValues previousAddress = new ContentValues();
        previousAddress.put(PreviousAddressesDatabaseHelper.ADDRESS,addressAsString);
        previousAddress.put(PreviousAddressesDatabaseHelper.LATITUDE,address.getLatitude());
        previousAddress.put(PreviousAddressesDatabaseHelper.LONGITUDE,address.getLongitude());

        PreviousAddressesDatabaseHelper.insertUniqely(mPrevAddrDb,previousAddress);
        if(mFirstSpinnerCursorAdapter != null) {
            Cursor cursor = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
            mFirstSpinnerCursorAdapter.swapCursor(cursor).close();
        }
        if(mSecondSpinnerCursorAdapter != null) {
            Cursor cursor = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
            mSecondSpinnerCursorAdapter.swapCursor(cursor).close();
        }
    }

    private void updateMainLayout(boolean isOn) {
        Log.d(TAG,"updateMainLayout");
        ToggleButton tracker_switch = (ToggleButton) findViewById(R.id.switch_travelchecker_service);
        tracker_switch.setChecked(isOn);
        updateGui();
    }

    private void updateGui() {
        Log.d(TAG,"updateGui");
        Cursor dbCursor = TravelTimeDatabaseHelper.getAllRecords(mTravelDataDb);
        Point[] travelData = Utilities.getTravelData(dbCursor);
        String lastUpdated = Utilities.getLastUpdatedTime(dbCursor);
        long currentDistance    = Utilities.getCurrentDistance(dbCursor);
        long currentTravelTime  = Utilities.getCurrentTravelTime(dbCursor);
        if(!lastUpdated.isEmpty())
            mGraphTitle = getString(R.string.graph_title) + ", " + lastUpdated;
        else
            mGraphTitle = getString(R.string.graph_title);
        dbCursor.close();
        Log.d(TAG, "Got message: " + travelData);
        if(travelData != null) {
            displayValues(travelData);
        }
        TextView travelTime = (TextView) findViewById(R.id.travelTime);
        TextView travelDistance = (TextView) findViewById(R.id.travelDistance);

        if(travelData.length == 0)
            return;

        Log.d(TAG, "time&distance" + currentTravelTime + "," + currentDistance);
        travelTime.setText(Utilities.formatTravelTime(this,(int)currentTravelTime));
        travelDistance.setText(Utilities.formatDistance(this,(int)currentDistance));
    }

    private void displayValues(Point[] travelData) {
        Log.d(TAG,"displayValues");
        Log.d(TAG,"travelData" + travelData);
        if(travelData.length > 0) {
            mLineGraph.setMaxY(Utilities.getMaxY(travelData) * 1.2);
        } else {
            mLineGraph.setMaxY(DEFAULT_MAXY);
        }
        TextView tv_table_title = (TextView) findViewById(R.id.table_title);
        tv_table_title.setText(mGraphTitle);


        mLineGraph.deleteSerie();
        mLineGraph.addSerie(travelData);
        mLineGraph.setSerieColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark, null));


    }

    // Start and Stop
    private void start() {
        Log.d(TAG,"start");
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(AVOID, mAvoid);
        editor.putBoolean(RUN,true);
        editor.apply();

        Intent serviceIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, serviceIntent, 0);
        mManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mManager.cancel(pendingIntent);

        mManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
        Log.d(TAG,"Alarm set");
        if(mToast != null)
            mToast.cancel();

        mToast = Toast.makeText(this, getString(R.string.started), Toast.LENGTH_SHORT);
        mToast.show();

        clearHistory();
        showLastValue(false);
        setViewsEnabled(false);
    }

    private void stop() {
        Log.d(TAG,"stop");
        setViewsEnabled(true);
        showLastValue(true);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME,0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(RUN,false);
        editor.apply();
        NotificationManager nmgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nmgr.cancel(NOTIFICATION_ID);
    }


    // Button and field enablers
    private void checkStartButton() {
        Log.d(TAG, "checkStartButton");
        ToggleButton startStop = (ToggleButton) findViewById(R.id.switch_travelchecker_service);
        if(isAddressSet()) {
            startStop.setTextColor(ResourcesCompat.getColor(getResources(), R.color.windowBackground, null));
            startStop.setEnabled(true);
        } else {
            startStop.setTextColor(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryDark, null));
            startStop.setEnabled(false);
        }
    }

    private void setViewsEnabled(boolean enabled) {
        Log.d(TAG,"setViewsEnabled");
        ToggleButton tg = (ToggleButton) findViewById(R.id.direction);
        tg.setEnabled(enabled);
        Button clear = (Button) findViewById(R.id.clear_history);
        clear.setEnabled(enabled);
        ImageButton first_ib = (ImageButton) findViewById(R.id.map_first);
        ImageButton second_ib = (ImageButton) findViewById(R.id.map_second);
        ImageView first_delete = (ImageView) findViewById(R.id.first_previous_delete);
        ImageView second_delete = (ImageView) findViewById(R.id.second_previous_delete);
        first_delete.setEnabled(enabled);
        second_delete.setEnabled(enabled);
        mSp_firstPreviousAddresses.setEnabled(enabled);
        mSp_secondPreviousAddresses.setEnabled(enabled);
        first_ib.setEnabled(enabled);
        second_ib.setEnabled(enabled);
        tg.setChecked(!mIsTo);
        if(enabled) {
            tg.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.directionbutton_toggle, null));
            clear.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
            clear.setTextColor(ResourcesCompat.getColor(getResources(),R.color.windowBackground,null));
            first_ib.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorPrimary,null));
            second_ib.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorPrimary,null));

        } else {
            tg.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.directionbutton_toggle_disabled, null));
            clear.getBackground().setColorFilter(ContextCompat.getColor(this, R.color.colorPrimaryLight), PorterDuff.Mode.MULTIPLY);
            //clear.setPaintFlags(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryLight, null));
            first_ib.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryLight,null));
            second_ib.setColorFilter(ResourcesCompat.getColor(getResources(), R.color.colorPrimaryLight,null));
        }
    }

    private void clearHistory() {
        Log.d(TAG,"clearHistory");
        Log.d(TAG, "Delete all from DB");
        SQLiteOpenHelper timeToGoDatabaseHelper = new TravelTimeDatabaseHelper(MainActivity.this);
        SQLiteDatabase db = timeToGoDatabaseHelper.getWritableDatabase();
        db.execSQL("delete from " + TravelTimeDatabaseHelper.TIMETOGO);
        db.close();
        //mGraph.removeAllSeries();
        mLineGraph.deleteSerie();
        mGraphTitle = getString(R.string.graph_title);
        TextView tv_table_title = (TextView) findViewById(R.id.table_title);
        tv_table_title.setText(mGraphTitle);
        TextView travelTime = (TextView) findViewById(R.id.travelTime);
        travelTime.setText(getString(R.string.unknown));

    }
    private void clearAdresses() {
        Log.d(TAG,"clearAdresses");
        // Remove the saved preferences
        SharedPreferences.Editor editor = mSettings.edit();
        editor.remove(FIRST_DB_KEY_ID);
        editor.remove(SECOND_DB_KEY_ID);
        PreviousAddressesDatabaseHelper.deleteAllRecords(mPrevAddrDb);
        editor.apply();
        checkStartButton();
        setFirstPreviousAddressSpinners();
        setSecondPreviousAddressSpinners();
        showSpinner(true);
        showSpinner(false);
    }

    private void showLastValue(boolean showVal) {
        Log.d(TAG,"showLastValue");
        ProgressBar pb = (ProgressBar) findViewById(R.id.sp_waitfor_result);
        TextView lastVal = (TextView)findViewById(R.id.travelTime);
        TextView lastDistance = (TextView) findViewById(R.id.travelDistance);
        if(showVal) {
            pb.setVisibility(View.INVISIBLE);
            lastVal.setVisibility(View.VISIBLE);
            lastDistance.setVisibility(View.VISIBLE);
        } else {
            pb.setVisibility(View.VISIBLE);
            lastVal.setVisibility(View.INVISIBLE);
            lastDistance.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isAddressSet() {
        Log.d(TAG,"isAddressSet");
        Log.d(TAG, "DBKEYID1:" +  mSettings.getLong(FIRST_DB_KEY_ID,-1));
        Log.d(TAG, "DBKEYID2:" +  mSettings.getLong(SECOND_DB_KEY_ID,-1));
        if(mSettings.getLong(FIRST_DB_KEY_ID,-1) == -1 ||
                mSettings.getLong(SECOND_DB_KEY_ID, -1) == -1)
            return false;
        else
            return true;
    }

    private LatLngBounds latLngToBounds(LatLng center, double radius) {
        Log.d(TAG,"latLngToBounds");
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }

    private void setFirstPreviousAddressSpinners() {
        Log.d(TAG,"setFirstPreviousAddressSpinners");
        long keyId;
        // Create an adapter based on the DB content
        String[] adapterCols = new String[]{PreviousAddressesDatabaseHelper.ADDRESS, PreviousAddressesDatabaseHelper.KEY_ID};
        int[] adapterRowViews = new int[]{R.id.text};

        // First address
        Cursor firstCursor = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
        Log.d(TAG,"Cursor count:" + firstCursor.getCount());

        mFirstSpinnerCursorAdapter = new SimpleCursorAdapter(
                this, R.layout.sp_previous_addresses, firstCursor, adapterCols, adapterRowViews, 0);
        mFirstSpinnerCursorAdapter.setDropDownViewResource(R.layout.sp_previous_addresses);
        mSp_firstPreviousAddresses.setAdapter(mFirstSpinnerCursorAdapter);
        FirstSpinnerInteractionListener listener = new FirstSpinnerInteractionListener();
        mSp_firstPreviousAddresses.setOnItemSelectedListener(listener);
        mSp_firstPreviousAddresses.setOnTouchListener(listener);
        /*
        mSp_firstPreviousAddresses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean userSelect = false;
            public boolean onTouch(View v, MotionEvent event) {
                userSelect = true;
                return false;
            }
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!userSelect)
                    return;
                Log.d(TAG, "onItemClick first pos:" + position + " id:" + id);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putLong(FIRST_DB_KEY_ID,id);
                editor.apply();
                checkStartButton();
                showSpinner(true);
            }
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onItemClick nothing selected");
            }
        });*/

        keyId = mSettings.getLong(FIRST_DB_KEY_ID, -1);
        for (int i = 0; i < mSp_firstPreviousAddresses.getCount(); i++) {
            if (mSp_firstPreviousAddresses.getItemIdAtPosition(i) == keyId) {
                mSp_firstPreviousAddresses.setSelection(i);
            }
        }

        showSpinner(true);
    }

    public class FirstSpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (userSelect) {
                Log.d(TAG, "onItemClick first pos:" + pos + " id:" + id);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putLong(FIRST_DB_KEY_ID,id);
                editor.apply();
                checkStartButton();
                showSpinner(true);
                userSelect = false;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(TAG, "onItemClick nothing selected");
        }
    }

    private void setSecondPreviousAddressSpinners() {
        Log.d(TAG,"setSecondPreviousAddressSpinners");
        long keyId;
        // Create an adapter based on the DB content
        String[] adapterCols = new String[]{PreviousAddressesDatabaseHelper.ADDRESS, PreviousAddressesDatabaseHelper.KEY_ID};
        int[] adapterRowViews = new int[]{R.id.text};

        // Second address
        Cursor secondCursor = PreviousAddressesDatabaseHelper.getAllRecords(mPrevAddrDb);
        mSecondSpinnerCursorAdapter = new SimpleCursorAdapter(
                this, R.layout.sp_previous_addresses, secondCursor, adapterCols, adapterRowViews, 0);
        mSecondSpinnerCursorAdapter.setDropDownViewResource(R.layout.sp_previous_addresses);

        mSp_secondPreviousAddresses.setAdapter(mSecondSpinnerCursorAdapter);
        SecondSpinnerInteractionListener listener = new SecondSpinnerInteractionListener();
        mSp_secondPreviousAddresses.setOnItemSelectedListener(listener);
        mSp_secondPreviousAddresses.setOnTouchListener(listener);

        /*mSp_secondPreviousAddresses.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick second pos:" + position + " id:" + id);
                if(mIsFirstSelected)
                    return;
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putLong(SECOND_DB_KEY_ID,id);
                editor.apply();
                checkStartButton();
                showSpinner(false);
            }
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onItemClick nothing selected");
            }
        }); */
        keyId = mSettings.getLong(SECOND_DB_KEY_ID, -1);
        for (int i = 0; i < mSp_secondPreviousAddresses.getCount(); i++) {
            if (mSp_secondPreviousAddresses.getItemIdAtPosition(i) == keyId) {
                mSp_secondPreviousAddresses.setSelection(i);
            }
        }
        showSpinner(false);
    }

    public class SecondSpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {

        boolean userSelect = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (userSelect) {
                Log.d(TAG, "onItemClick second pos:" + pos + " id:" + id);
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putLong(SECOND_DB_KEY_ID,id);
                editor.apply();
                checkStartButton();
                showSpinner(false);
                userSelect = false;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(TAG, "onItemClick nothing selected");
        }
    }


    private void showSpinner(boolean isFirst) {
        Log.d(TAG,"showSpinner");
        TextView hint;
        ImageView delete;
        Spinner spinner;
        boolean selected = false;
        if(isFirst) {
            hint = (TextView) findViewById(R.id.first_previous_hint);
            delete =(ImageView) findViewById(R.id.first_previous_delete);
            spinner = mSp_firstPreviousAddresses;
            selected = mSettings.getLong(FIRST_DB_KEY_ID,-1) != -1;

        } else {
            hint = (TextView) findViewById(R.id.second_previous_hint);
            delete = (ImageView) findViewById(R.id.second_previous_delete);
            spinner = mSp_secondPreviousAddresses;
            selected = mSettings.getLong(SECOND_DB_KEY_ID,-1) != -1;
        }
        if(!selected) {
            hint.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.INVISIBLE);
            delete.setVisibility(View.INVISIBLE);
        } else {
            hint.setVisibility(View.INVISIBLE);
            spinner.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);
        }
    }
}