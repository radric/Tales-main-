package ua.andriyantonov.tales;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public class AudioActivity extends ActionBarActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener {
    private TextView taleText,taleDuration,currTimePos;
    private Intent serviceIntent,switchPlayStatus;
    public boolean isOnline;
    private boolean mBufferBroadcastIsRegistered;
    private ProgressDialog pdBuffer = null;
    public static ProgressDialog dlProgDialog=null;
    public static ImageButton btn_PlayResume,btn_Stop,btn_taleDownload;
    public int isPlaying;
    private boolean mADialogBroadcastIsRegistered;
    private CharSequence abTitle;


    //--SeekBar variables
    private SeekBar audioTaleSeekBar;
    private int seekProgress;
    private int seekMax;
    private static int audioTaleEnded = 0;
    private boolean mSeekBarBroadcastIsRegistered;

    //--Declare seekBar action and intent
    public static String BROADCAST_SEEKBAR = "ua.andriyantonov.tales.seekbarprogress";
    private Intent seekBarIntent;

    public static String BROADCAST_switchPlayStatus = "ua.andriyantonov.tales.switchStatus";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        serviceIntent = new Intent(this, TalePlay_Service.class);
        seekBarIntent = new Intent(BROADCAST_SEEKBAR);
        taleText = (TextView)findViewById(R.id.taleText);
        taleDuration = (TextView)findViewById(R.id.taleDuration);
        currTimePos = (TextView)findViewById(R.id.currTimePos);
        btn_PlayResume = (ImageButton)findViewById(R.id.btn_PlayResume);
        btn_Stop = (ImageButton)findViewById(R.id.btn_Stop);
        btn_taleDownload = (ImageButton)findViewById(R.id.btn_taleDownload);
        audioTaleSeekBar = (SeekBar)findViewById(R.id.audioTaleSeekBar);
        dlProgDialog = new ProgressDialog(this);

        UpdateTalesData.loadTalesData(this);
        UpdateTalesData.getTaleText(this);
        UpdateTalesData.getTaleName(this);

        taleText.setText(UpdateTalesData.taleText);
        btn_PlayResume.setOnClickListener(this);
        btn_Stop.setOnClickListener(this);
        btn_taleDownload.setOnClickListener(this);
        audioTaleSeekBar.setOnSeekBarChangeListener(this);

        abTitle=UpdateTalesData.taleName;
        getSupportActionBar().setTitle(abTitle);

        setDlProgDialog();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_PlayResume:
                switch (isPlaying){
                    case 0:
                        startTalePlay_Service();
                        isPlaying=1; // start play
                        break;
                    case 1:
                        switchPlayPause();
                        isPlaying=2;// on pause
                        break;
                    case 2:
                        switchPlayPause();
                        isPlaying=1;// start play
                        break;
                }
                break;
            case R.id.btn_Stop:
                stopTalePlay_Service();
                isPlaying=0;
                break;
            case R.id.btn_taleDownload:
                checkConnectivity();
                if (UpdateTalesData.checkTaleExist.exists()){
                    deleteDialog();
                } else if (isOnline){
                    startDowload();
                }else {
                    doIfOffline();
                }

        }
    }

    private void startTalePlay_Service(){
        registerBroadcastReceivers();
        /**check if the tale was already downloaded and mp3 file existed
         * if it was - use mp3 from storage
         * if not - upload from cloudService*/
        UpdateTalesData.loadTalesData(this);
        if (UpdateTalesData.checkTaleExist.exists()){
            doIfOnline();
            //cos code the same
        } else {
            checkConnectivity();
            if (isOnline){
                doIfOnline();
            }else {
                doIfOffline();
            }
        }
    }

    private void switchPlayPause(){
        switchPlayStatus = new Intent(BROADCAST_switchPlayStatus);
        if (isPlaying==1) {
            switchPlayStatus.putExtra("switchPlayStatus", 1);
            btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        } else if (isPlaying==2){
            switchPlayStatus.putExtra("switchPlayStatus",2);
            btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(switchPlayStatus);
    }

    private void stopTalePlay_Service(){
        btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        audioTaleSeekBar.setProgress(0);
        currTimePos.setText("00:00");
        this.stopService(serviceIntent);
        unregisterBroadcastReceivers();
    }

    private void checkConnectivity(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting() || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting())
            isOnline=true;
        else{
            isOnline=false;}
    }
    private void doIfOnline(){
        btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        this.startService(serviceIntent);
    }
    private void doIfOffline(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Відсутній доступ до мережі...");
        alertDialog.setMessage("Увімкніть будь-ласка мережу та спробуйте знову");
        alertDialog.setButton("Добре", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isPlaying=0;
            }
        });
        alertDialog.setIcon(R.drawable.ic_launcher);
        btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        alertDialog.show();
    }

    /** Updating position of seekBar from Service*/
    private BroadcastReceiver seekBarBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    private void updateUI(Intent serviceIntent){
        String taleAudioPosition = serviceIntent.getStringExtra("counter");
        String taleAudioMaxPosition = serviceIntent.getStringExtra("audioMax");
        String audioMaxText = serviceIntent.getStringExtra("audioMaxText");
        String currTimePosText = serviceIntent.getStringExtra("currTimePosText");
        UpdateTalesData.loadTalesData(getApplicationContext());
        audioTaleEnded = UpdateTalesData.audioTaleEnded;

        seekProgress=Integer.parseInt(taleAudioPosition);
        seekMax = Integer.parseInt(taleAudioMaxPosition);
        audioTaleSeekBar.setMax(seekMax);
        audioTaleSeekBar.setProgress(seekProgress);
        taleDuration.setText(audioMaxText);
        currTimePos.setText(currTimePosText);
        if (audioTaleEnded==1){
            stopTalePlay_Service();
            isPlaying=0;
            UpdateTalesData.saveTalesIntData(getApplicationContext(),"isPlaying",isPlaying);
            audioTaleEnded=0;
            UpdateTalesData.saveTalesIntData(getApplicationContext(),"audioTaleEnded",audioTaleEnded);
        }
    }

    /** Handle progress dialog for buffering*/
    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showPogrDialog(bufferIntent);
        }
    };
    private void showPogrDialog(Intent bufferIntent){
        String bufferValue = bufferIntent.getStringExtra("buffering");
        int bufferIntValue = Integer.parseInt(bufferValue);
        switch (bufferIntValue){
            case 0:
                if (pdBuffer!=null) {
                    pdBuffer.dismiss();
                }
                break;
            case 1:
                /** setup progress dialog */
                pdBuffer = ProgressDialog.show(this,getResources().getString(R.string.pdBuffer_inProgress),
                        getResources().getString(R.string.pdBuffer_inBuffer),true);
                pdBuffer.setCancelable(true);
        }
    }

    private BroadcastReceiver aDialogBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            aDialog(intent);
        }
    };

    public void aDialog(Intent intent){
        isPlaying=2;
        btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setMessage(getResources().getString(R.string.ad_afterCall_message));
        ad.setButton(getResources().getString(R.string.ad_afterCall_btnPos),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switchPlayPause();
            }
        });
        ad.setButton2(getResources().getString(R.string.ad_afterCall_btnNeg), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                stopTalePlay_Service();
                isPlaying=0;
            }
        });
        ad.setCancelable(true);
        ad.show();
    }

    public void deleteDialog(){
        AlertDialog ad = new AlertDialog.Builder(this).create();
        ad.setMessage(getResources().getString(R.string.del_message));
        ad.setButton(getResources().getString(R.string.del_btnPos),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteTaleFromStorage();
            }
        });
        ad.setButton2(getResources().getString(R.string.del_btnNeg), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        ad.setCancelable(true);
        ad.show();
    }

    private void setDlProgDialog(){
        dlProgDialog.setIcon(R.drawable.btn_download_p);
        dlProgDialog.setMessage(getResources().getString(R.string.pdBuffer_inProgress));
        dlProgDialog.setIndeterminate(true);
        dlProgDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dlProgDialog.setCancelable(false);
    }

    private void startDowload(){
        final DownloadTask downloadTask = new DownloadTask(this);
        downloadTask.execute(UpdateTalesData.data_HTTP);
        dlProgDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });

    }

    private void registerBroadcastReceivers(){
        /**register broadcastReceiver for buffer*/
        if (!mBufferBroadcastIsRegistered){
            this.registerReceiver(broadcastBufferReceiver,
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
            this.registerReceiver(aDialogBroadcastReceiver,
                    new IntentFilter(TalePlay_Service.BROADCAST_ADIALOG));
            mADialogBroadcastIsRegistered = true;
        }


    }

    private void unregisterBroadcastReceivers(){
        /**unregister broadcastReceiver for buffer*/
        if (mBufferBroadcastIsRegistered)
            try {
                this.unregisterReceiver(broadcastBufferReceiver);
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

        /** register broadcastReceiver for alertDialog */
        if (mADialogBroadcastIsRegistered){
            try {
                this.unregisterReceiver(aDialogBroadcastReceiver);
                mADialogBroadcastIsRegistered=false;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser){
            int seekPos = seekBar.getProgress();
            seekBarIntent.putExtra("seekPos",seekPos);
            this.sendBroadcast(seekBarIntent);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {  }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {   }

    public void deleteTaleFromStorage(){
        File file = new File(UpdateTalesData.checkTaleExist.toString());
        file.delete();
        stopTalePlay_Service();
        isPlaying=0;
        Toast.makeText(this, getResources().getString(R.string.del_success),
                Toast.LENGTH_SHORT).show();
        btn_taleDownload.setImageResource(R.drawable.btn_download_n);
    }

    public void checkPlayBtnPosition(){
        UpdateTalesData.loadTalesData(this);
        isPlaying=UpdateTalesData.isPlaying;
        if (isPlaying==1) {
            btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
        } else if (isPlaying==2|isPlaying==0){
            btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if (isPlaying==2){
            stopTalePlay_Service();
            isPlaying=0;
        }
        UpdateTalesData.saveTalesIntData(this,"isPlaying",isPlaying);
        unregisterBroadcastReceivers();

    }
    @Override
    public void onResume(){
        registerBroadcastReceivers();
        checkPlayBtnPosition();
        /**check every start (in case if user deleted audioTale in fileManger*/
        if (UpdateTalesData.checkTaleExist.exists()){
            btn_taleDownload.setImageResource(R.drawable.btn_download_p);
        } else {
            btn_taleDownload.setImageResource(R.drawable.btn_download_n);
        }
        super.onResume();
    }

}
