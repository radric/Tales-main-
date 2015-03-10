package ua.andriyantonov.tales;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
}
