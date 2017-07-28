package se.torsteneriksson.timetogo;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity"  ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar actionbar = (Toolbar) findViewById(R.id.my_toolbar);
        // Set the action bar back button to look like an up button
        if (null != actionbar) {
            actionbar.setTitle(R.string.title_activity_settings);
            actionbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NavUtils.navigateUpFromSameTask(SettingsActivity.this);
                }
            });
        }
    }

}
