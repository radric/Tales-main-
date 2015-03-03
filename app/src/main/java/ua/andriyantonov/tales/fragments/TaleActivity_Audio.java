package ua.andriyantonov.tales.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import ua.andriyantonov.tales.TalesSettings;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalePlay_Service;

public class TaleActivity_Audio extends Fragment implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    private TextView taleName,taleText,taleDuration,currTimePos;
    private Intent serviceIntent,switchStatus;
    private boolean isOnline;
    private boolean mBufferBroadcastIsRegistered;
    private ProgressDialog pdBuffer = null;
    public ImageButton btn_PlayResume,btn_Stop;
    public int isPlaying=0;

    //--SeekBar variables
    private SeekBar audioTaleSeekBar;
    private int seekProgress;
    private int seekMax;
    private static int audioTaleEnded = 0;
    boolean mSeekBarBroadcastIsRegistered;

    //--Declare seekBar action and intent
    public static String BROADCAST_SEEKBAR = "ua.andriyantonov.tales.seekbarprogress";
    private Intent seekBarIntent;

    public static String BROADCAST_switchStatus = "ua.andriyantonov.tales.switchStatus";

    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taleactivity_audio,container,false);
        serviceIntent = new Intent(getActivity(), TalePlay_Service.class);
        seekBarIntent = new Intent(BROADCAST_SEEKBAR);
        taleName = (TextView)rootView.findViewById(R.id.taleName);
        taleText = (TextView)rootView.findViewById(R.id.taleText);
        taleDuration = (TextView)rootView.findViewById(R.id.taleDuration);
        currTimePos = (TextView)rootView.findViewById(R.id.currTimePos);
        btn_PlayResume = (ImageButton)rootView.findViewById(R.id.btn_PlayResume);
        btn_Stop = (ImageButton)rootView.findViewById(R.id.btn_Stop);
        audioTaleSeekBar = (SeekBar)rootView.findViewById(R.id.audioTaleSeekBar);

        TalesSettings.getTaleName(getActivity());
        TalesSettings.getTaleText(getActivity());
        taleName.setText(TalesSettings.taleName);
        taleText.setText(TalesSettings.taleText);

        btn_PlayResume.setOnClickListener(this);
        btn_Stop.setOnClickListener(this);
        audioTaleSeekBar.setOnSeekBarChangeListener(this);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_PlayResume:
                switch (isPlaying){
                    case 0:
                        startTalePlay_Service();
                        isPlaying=1; //is playing
                        break;
                    case 1:
                        switchPlayPause();
                        isPlaying=2;// on pause
                        break;
                    case 2:
                        switchPlayPause();
                        isPlaying=1;// on pause
                        break;
                }
                break;
            case R.id.btn_Stop:
                stopTalePlay_Service();
                isPlaying=0;
        }
    }

    private void startTalePlay_Service(){
        checkConnectivity();
        if (isOnline){
            /** register broadcastReceiver for seekBar*/
            getActivity().registerReceiver(seekBarBroadcastReceiver,new IntentFilter(TalePlay_Service.BROADCAST_ACTION));
            mSeekBarBroadcastIsRegistered=true;
            btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
            getActivity().startService(serviceIntent);
        }else {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
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
    }

    private void switchPlayPause(){
        switchStatus = new Intent(BROADCAST_switchStatus);
        if (isPlaying==1) {
            switchStatus.putExtra("switchStatus", 1);
            btn_PlayResume.setImageResource(R.drawable.select_btn_play);
            Log.d("111", "pressed start");
        } else if (isPlaying==2){
            switchStatus.putExtra("switchStatus",2);
            btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
            Log.d("1111","pressed start");
        }
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(switchStatus);
        Log.d("111","BR was sent");
    }

    private void stopTalePlay_Service(){
        btn_PlayResume.setImageResource(R.drawable.select_btn_play);
        audioTaleSeekBar.setProgress(0);
        currTimePos.setText("00:00");
        getActivity().stopService(serviceIntent);
//        switchStatus = new Intent(BROADCAST_switchStatus);
//        switchStatus.putExtra("switchStatus",2);
//        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(switchStatus);

        onPause();
    }

    private void checkConnectivity(){
        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting() || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting())
            isOnline=true;
        else
            isOnline=false;
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
        String song_ended = serviceIntent.getStringExtra("song_ended");
        String audioMaxText = serviceIntent.getStringExtra("audioMaxText");
        String currTimePosText = serviceIntent.getStringExtra("currTimePosText");
        seekProgress=Integer.parseInt(taleAudioPosition);
        seekMax = Integer.parseInt(taleAudioMaxPosition);
        audioTaleEnded = Integer.parseInt(song_ended);
        audioTaleSeekBar.setMax(seekMax);
        audioTaleSeekBar.setProgress(seekProgress);
        taleDuration.setText(audioMaxText);
        currTimePos.setText(currTimePosText);
        if (audioTaleEnded==1){
            stopTalePlay_Service();
        }
    }

    /** Handle progress dialog for buffering*/
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
                pdBuffer = ProgressDialog.show(getActivity(),getResources().getString(R.string.pdBuffer_inProgress),
                        getResources().getString(R.string.pdBuffer_inBuffer),true);
        }
    }
    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            showPogrDialog(bufferIntent);
        }
    };

    @Override
    public void onPause(){
        if (mBufferBroadcastIsRegistered){
            getActivity().unregisterReceiver(broadcastBufferReceiver);
            mBufferBroadcastIsRegistered=false;
        }
        /**unregister broadcastReceiver for seekBar*/
        if (mSeekBarBroadcastIsRegistered){
            try {
                getActivity().unregisterReceiver(seekBarBroadcastReceiver);
                mSeekBarBroadcastIsRegistered=false;
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        super.onPause();
    }

    @Override
    public void onResume(){
        if (!mBufferBroadcastIsRegistered){
            getActivity().registerReceiver(broadcastBufferReceiver,new IntentFilter(
                    TalePlay_Service.BROADCAST_BUFFER));
            mBufferBroadcastIsRegistered=true;
        }
        super.onResume();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser){
            int seekPos = seekBar.getProgress();
            seekBarIntent.putExtra("seekPos",seekPos);
            getActivity().sendBroadcast(seekBarIntent);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {  }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {   }

}

