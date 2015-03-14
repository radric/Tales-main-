package ua.andriyantonov.tales;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class ReadActivity extends ActionBarActivity {
    private TextView taleText;
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        taleText = (TextView)findViewById(R.id.taleText);

        UpdateTalesData.getTaleText(this);
        UpdateTalesData.getTaleName(this);
        taleText.setText(UpdateTalesData.taleText);

        mTitle=UpdateTalesData.taleName;
        setTitle(mTitle);
    }

    @Override
    public void onResume(){
        updateTextSize();
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.main, menu);
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

    public void updateTextSize(){
        SharedPreferences shp;
        shp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String textSize = shp.getString(
                getString(R.string.pref_textSize_key),
                getString(R.string.pref_textSize_default)
        );
        float size = Float.parseFloat(textSize);
        taleText.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
    }
}
