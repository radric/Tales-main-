package ua.andriyantonov.tales.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import ua.andriyantonov.tales.daos.DownloadCurrent;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.services.TalePlay_Service;
import ua.andriyantonov.tales.daos.UpdateTalesData;
import ua.andriyantonov.tales.analytics.Analytics;

/**
 * Shows chosen tale's text and gives opportunity to play audioTale from cloudService
 * or download and play from device
 */

public class AudioActivity extends ActionBarActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    public final static String BROADCAST_SEEKBAR = "ua.andriyantonov.tales.seekbarprogress";
    public final static String BROADCAST_switchPlayStatus = "ua.andriyantonov.tales.switchStatus";

    private int mIsPlaying;

    private static boolean mIsOnline;
    private boolean mSeekBarBroadcastIsRegistered, mADialogBroadcastIsRegistered,
            mBufferBroadcastIsRegistered;

    public static ImageButton sBtn_PlayResume,sBtn_Stop,sBtn_taleDownload;
    private Intent mServiceIntent,mSeekBarIntent;
    private ProgressDialog mProgDialBuffer = null;
    public static ProgressDialog sProgDialDownload=null;
    private SharedPreferences mShp;

    @InjectView(R.id.taleText) TextView mTaleText;
    @InjectView(R.id.taleDuration) TextView mTaleDurText;
    @InjectView(R.id.currTimePos) TextView mCurrTimePosText;
    @InjectView(R.id.audioTaleSeekBar) SeekBar mAudioTaleSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        ((Analytics) getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
        ButterKnife.inject(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mServiceIntent = new Intent(this, TalePlay_Service.class);
        mSeekBarIntent = new Intent(BROADCAST_SEEKBAR);
        sBtn_PlayResume = (ImageButton)findViewById(R.id.btn_PlayResume);
        sBtn_Stop = (ImageButton)findViewById(R.id.btn_Stop);
        sBtn_taleDownload = (ImageButton)findViewById(R.id.btn_taleDownload);
        sProgDialDownload = new ProgressDialog(this);

        UpdateTalesData.loadTalesData(this);
        UpdateTalesData.getTaleText(this);
        UpdateTalesData.getTaleName(this);

        mTaleText.setText(UpdateTalesData.sTaleText);
        sBtn_PlayResume.setOnClickListener(this);
        sBtn_Stop.setOnClickListener(this);
        sBtn_taleDownload.setOnClickListener(this);
        mAudioTaleSeekBar.setOnSeekBarChangeListener(this);

        CharSequence abTitle = UpdateTalesData.sTaleName;
        getSupportActionBar().setTitle(abTitle);

        setProgDialDownload();

    }
    @Override
    public void onPause(){
        super.onPause();
        if (mIsPlaying==2||mIsPlaying==0){
            stopTalePlay_Service();
            mIsPlaying=0;
            unregisterAfterCallBroadcastReceiver();
        }
        UpdateTalesData.saveTalesIntData(this,UpdateTalesData.ISPLAYING_KEY,mIsPlaying);
        unregisterBroadcastReceivers();
    }
    @Override
    public void onResume(){
        registerBroadcastReceivers();
        checkBtnPosition();
        checkTextSize();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_PlayResume:
                switch (mIsPlaying){
                    case 0:
                        startTalePlay_Service();
                        mIsPlaying=1; // start play
                        break;
                    case 1:
                        switchPlayPause();
                        mIsPlaying=2;// on pause
                        break;
                    case 2:
                        switchPlayPause();
                        mIsPlaying=1;// start play
                        break;
                }
                break;
            case R.id.btn_Stop:
                stopTalePlay_Service();
                mIsPlaying=0;
                break;
            case R.id.btn_taleDownload:
                checkConnectivity();
                if (UpdateTalesData.mCheckTaleExist.exists()){
                    deleteDialog();
                } else if (mIsOnline){
                    downloadDialog();
                }else {
                    doIfOffline();
                }
        }
    }

    /**
     * Starts Service's work and playing audio from storage (if was downloaded before and mp3 file exist)
     * or from cloud (if wasn't)
     */
    private void startTalePlay_Service(){
        UpdateTalesData.loadTalesData(this);
        if (UpdateTalesData.mCheckTaleExist.exists()){
            doIfOnline(); //cos code the same
        } else {
            checkConnectivity();
            if (mIsOnline){
                doIfOnline();
                autoDownloadTales();
            }else {
                doIfOffline();
            }
        }
    }

    /**
     * Switching btns Play and Pause in UI and sending command to Service via localBroadcastManager
     */
    private void switchPlayPause(){
        Intent mSwitchPlayStatus = new Intent(BROADCAST_switchPlayStatus);
        if (mIsPlaying==1) {
            mSwitchPlayStatus.putExtra("switchPlayStatus", 1);
            sBtn_PlayResume.setImageResource(R.drawable.select_btn_play);
        } else if (mIsPlaying==2){
            mSwitchPlayStatus.putExtra("switchPlayStatus",2);
            sBtn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(mSwitchPlayStatus);
    }

    /**
     * Stops Service's work, unregister broadcastReceivers
     */
    private void stopTalePlay_Service(){
        sBtn_PlayResume.setImageResource(R.drawable.select_btn_play);
        mAudioTaleSeekBar.setProgress(0);
        mCurrTimePosText.setText("00:00");
        this.stopService(mServiceIntent);
        unregisterBroadcastReceivers();

        /** unregister broadcastReceiver for alertDialog */
        unregisterAfterCallBroadcastReceiver();
    }

    /**
     * Updates text size according to Settings
     */
    public void checkTextSize(){
        mShp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String textSize = mShp.getString(
                getString(R.string.pref_textSize_key),
                getString(R.string.pref_textSize_default)
        );
        float size = Float.parseFloat(textSize);
        mTaleText.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
    }

    /**
     * Updates btns PlayResume according to mIsPlaying
     * Updates btns Download according to audioTale status (existed or not on the storage)
     */
    public void checkBtnPosition(){
        UpdateTalesData.loadTalesData(this);
        mIsPlaying=UpdateTalesData.sIsPlaying;
        if (mIsPlaying==1) {
            sBtn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        } else if (mIsPlaying==2|mIsPlaying==0){
            sBtn_PlayResume.setImageResource(R.drawable.select_btn_play);
        }

        /**check every start (in case if user deleted audioTale in fileManger*/
        if (UpdateTalesData.mCheckTaleExist.exists()){
            sBtn_taleDownload.setImageResource(R.drawable.btn_download_p);
        } else {
            sBtn_taleDownload.setImageResource(R.drawable.btn_download_n);
        }
        super.onResume();
    }

    /**
     * Checks connecting in case playing audio from cloud or downloading
     */
    public void checkConnectivity(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mIsOnline = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting() || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
    }

    private void doIfOnline(){
        registerBroadcastReceivers();
        sBtn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        this.startService(mServiceIntent);
    }
    private void doIfOffline(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Відсутній доступ до мережі...");
        alertDialog.setMessage("Увімкніть будь-ласка мережу та спробуйте знову");
        alertDialog.setButton("Добре", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsPlaying=0;
            }
        });
        alertDialog.setIcon(R.drawable.ic_launcher);
        sBtn_PlayResume.setImageResource(R.drawable.select_btn_play);
        alertDialog.show();
    }

    private BroadcastReceiver seekBarBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };
    /**
     * Gets Extras from Service via seekBarBroadcastReceivers and updates UI
     */
    private void updateUI(Intent serviceIntent){
        String taleAudioPosition = serviceIntent.getStringExtra("counter");
        String taleAudioMaxPosition = serviceIntent.getStringExtra("audioMax");
        String audioMaxText = serviceIntent.getStringExtra("audioMaxText");
        String currTimePosText = serviceIntent.getStringExtra("currTimePosText");
        UpdateTalesData.loadTalesData(getApplicationContext());
        int audioTaleEnded = UpdateTalesData.sAudioTaleEnded;

        int seekProgress = Integer.parseInt(taleAudioPosition);
        int seekMax = Integer.parseInt(taleAudioMaxPosition);
        mAudioTaleSeekBar.setMax(seekMax);
        mAudioTaleSeekBar.setProgress(seekProgress);
        mTaleDurText.setText(audioMaxText);
        mCurrTimePosText.setText(currTimePosText);
        if (audioTaleEnded ==1){
            stopTalePlay_Service();
            mIsPlaying=0;
            UpdateTalesData.saveTalesIntData(getApplicationContext(),
                    UpdateTalesData.ISPLAYING_KEY,mIsPlaying);
            audioTaleEnded =0;
            UpdateTalesData.saveTalesIntData(getApplicationContext(),"audioTaleEnded", audioTaleEnded);
        }
    }


    private BroadcastReceiver BufferBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showBufferingPogrDialog(bufferIntent);
        }
    };
    /**
     * Gets Extras from Service via bufferBroadcastReceivers and shows alertDialog
     * while audio buffering when playing from cloud
     */
    private void showBufferingPogrDialog(Intent bufferIntent){
        String bufferValue = bufferIntent.getStringExtra("buffering");
        int bufferIntValue = Integer.parseInt(bufferValue);
        switch (bufferIntValue){
            case 0:
                if (mProgDialBuffer!=null) {
                    mProgDialBuffer.dismiss();
                }
                break;
            case 1:
                /** setup progress dialog */
                mProgDialBuffer = ProgressDialog.show(this,getResources().getString(R.string.pdBuffer_inProgress),
                        getResources().getString(R.string.pdBuffer_inBuffer),true);
                mProgDialBuffer.setCancelable(true);
        }
    }


    private BroadcastReceiver afterCallBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent aDialogIntent) {
            afterCallAlertDialog();
        }
    };
    /**
     * Gets Extras from Service via afterCallBroadcastReceivers and shows alertDialog
     * after call was finished
     */
    private void afterCallAlertDialog(){
        mIsPlaying=2;
        sBtn_PlayResume.setImageResource(R.drawable.select_btn_play);
                AlertDialog ad = new AlertDialog.Builder(this).create();
                ad.setMessage(getResources().getString(R.string.ad_afterCall_message));
                ad.setButton(getResources().getString(R.string.ad_afterCall_btnPos),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switchPlayPause();
                    }
                });
                ad.setButton2(getResources().getString(R.string.ad_afterCall_btnNeg),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        stopTalePlay_Service();
                        mIsPlaying=0;
                    }
                });
                ad.setCancelable(false);
                ad.show();
    }

    /**
     * Shows alertDialog if user want to delete existed audioTale from storage
     */
    public void deleteDialog(){
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setMessage(getString(R.string.del_message));
        ad.setButton(getString(R.string.del_btnPos),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteTaleFromStorage();
            }
        });
        ad.setButton2(getResources().getString(R.string.del_btnNeg),
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        ad.setCancelable(true);
        ad.show();
    }

    /**
     * Shows alertDialog if user want to download audioTale from url
     */
    public void downloadDialog(){
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setMessage(getString(R.string.download_message)+" "+
                "\""+UpdateTalesData.sTaleName+"\""+ " ?");
        ad.setButton(getString(R.string.download_btnPos),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startDownload();
                    }
                });
        ad.setButton2(getResources().getString(R.string.download_btnNeg),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        ad.setCancelable(true);
        ad.show();
    }

    /**
     * Set progressDialog while audioTale downloading, change btn Download icon
     */
    private void setProgDialDownload(){
        sProgDialDownload.setMessage(getResources().getString(R.string.pdBuffer_inProgress));
        sProgDialDownload.setIndeterminate(true);
        sProgDialDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        sProgDialDownload.setCancelable(false);
    }

    /**
     * Starts downloading tale if has permission from Settings
     */
    public void autoDownloadTales(){
        mShp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean adlStatus = mShp.getBoolean(getString(R.string.pref_download_key),false);
        if (adlStatus){
            startDownload();
        }
    }
    /**
     * Starts downloading process from cloud
     */
    private void startDownload(){
        final DownloadCurrent downloadCurrent = new DownloadCurrent(this);
        downloadCurrent.execute(UpdateTalesData.sData_HTTP);
        sProgDialDownload.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadCurrent.cancel(true);
            }
        });

    }
    public void deleteTaleFromStorage(){
        File file = new File(UpdateTalesData.mCheckTaleExist.toString());
        file.delete();
        stopTalePlay_Service();
        mIsPlaying=0;
        Toast.makeText(this, getResources().getString(R.string.del_success),
                Toast.LENGTH_SHORT).show();
        sBtn_taleDownload.setImageResource(R.drawable.btn_download_n);
    }

    private void registerBroadcastReceivers(){
        /**register broadcastReceiver for buffer*/
        if (!mBufferBroadcastIsRegistered){
            this.registerReceiver(BufferBroadcastReceiver,
                    new IntentFilter(TalePlay_Service.BROADCAST_BUFFER));
            mBufferBroadcastIsRegistered=true;
        }
        /** register broadcastReceiver for seekBar*/
        if (!mSeekBarBroadcastIsRegistered) {
            this.registerReceiver(seekBarBroadcastReceiver,
                    new IntentFilter(TalePlay_Service.BROADCAST_ACTION));
            mSeekBarBroadcastIsRegistered = true;
        }
        /** register broadcastReceiver for alertDialog in case call when audioTale is playing*/
        if(!mADialogBroadcastIsRegistered){
            this.registerReceiver(afterCallBroadcastReceiver,
                    new IntentFilter(TalePlay_Service.BROADCAST_ADIALOG));
            mADialogBroadcastIsRegistered = true;
        }


    }

    private void unregisterBroadcastReceivers(){
        /**unregister broadcastReceiver for buffer*/
        if (mBufferBroadcastIsRegistered)
            try {
                this.unregisterReceiver(BufferBroadcastReceiver);
                mBufferBroadcastIsRegistered=false;
            }catch (Exception e){
                e.printStackTrace();
            }

        /**unregister broadcastReceiver for seekBar*/
        if (mSeekBarBroadcastIsRegistered){
            try {
                this.unregisterReceiver(seekBarBroadcastReceiver);
                mSeekBarBroadcastIsRegistered=false;
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        /** register broadcastReceiver for afterCall alertDialog
         * in StopService in case of call while playing */

    }

    private void unregisterAfterCallBroadcastReceiver(){
        /** unregister broadcastReceiver for alertDialog */
        if (mADialogBroadcastIsRegistered){
            try {
                this.unregisterReceiver(afterCallBroadcastReceiver);
                mADialogBroadcastIsRegistered=false;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets changes of seekbar from user and sendBroadcast to Service
     */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser){
            int seekPos = seekBar.getProgress();
            mSeekBarIntent.putExtra("seekPos",seekPos);
            this.sendBroadcast(mSeekBarIntent);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {  }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {   }










}
