package ua.andriyantonov.tales.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.daos.UpdateTalesData;
import ua.andriyantonov.tales.analytics.Analytics;

/**
 * Shows chosen tale's text
 */
public class ReadActivity extends ActionBarActivity {
    @InjectView(R.id.taleText) TextView mTaleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);
        ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        ButterKnife.inject(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        UpdateTalesData.getTaleText(this);
        UpdateTalesData.getTaleName(this);
        mTaleText.setText(UpdateTalesData.sTaleText);

        CharSequence mTitle = UpdateTalesData.sTaleName;
        setTitle(mTitle);
    }

    @Override
    public void onResume(){
        updateTextSize();
        super.onResume();
    }
    @Override
    public void onStart(){
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }
    @Override
    public void onStop(){
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.read, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                Intent intent = new Intent(this,SettingsActivity.class);
                startActivity(intent);
                break;
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Updates text size according to Settings
     */
    public void updateTextSize(){
        SharedPreferences shp;
        shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String textSize = shp.getString(
                getString(R.string.pref_textSize_key),
                getString(R.string.pref_textSize_default)
        );
        float size = Float.parseFloat(textSize);
        mTaleText.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
    }
}
