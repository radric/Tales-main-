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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import ua.andriyantonov.tales.LoadTale;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalePlay_Service;

public class TaleActivity_Audio extends Fragment implements SeekBar.OnSeekBarChangeListener{
    TextView textView;
    Intent serviceIntent;
    private boolean isOnline;
    private boolean mBufferBroadcastIsRegistered;
    private ProgressDialog pdBuffer = null;
    public String data_HTTP;
    ImageButton btn_PlayResume,btn_Stop;
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


    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taleactivity_audio,container,false);
        Bundle bundle = getArguments();
        final int talePosition = bundle.getInt("talePosition");

        serviceIntent = new Intent(getActivity(), TalePlay_Service.class);
        seekBarIntent = new Intent(BROADCAST_SEEKBAR);
        textView = (TextView)rootView.findViewById(R.id.taleName);
        btn_PlayResume = (ImageButton)rootView.findViewById(R.id.btn_PlayResume);
        btn_Stop = (ImageButton)rootView.findViewById(R.id.btn_Stop);
        audioTaleSeekBar = (SeekBar)rootView.findViewById(R.id.audioTaleSeekBar);

        btn_PlayResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (isPlaying){
                    case 0:
                        btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
                        isPlaying=1; //is playing
                        break;
                    case 1:
                        btn_PlayResume.setImageResource(R.drawable.select_btn_play);
                        isPlaying=2;// on pause
                        break;
                    case 2:
                        btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
                        isPlaying=1; //is playing
                        break;
                }
                startTalePlay_Service();
            }
        });

        btn_Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTalePlay_Service();
                audioTaleSeekBar.setProgress(0);
                isPlaying=0;
            }
        });

        audioTaleSeekBar.setOnSeekBarChangeListener(this);

        LoadTale.getAudioTaleHTTP(talePosition);
        data_HTTP = LoadTale.data_HTTP;
        return rootView;
    }

    private void startTalePlay_Service(){
        checkConnectivity();
        if (isOnline){
            //stopTalePlay_Service();
        serviceIntent.putExtra("sentAudioLink",data_HTTP);
        try {
            getActivity().startService(serviceIntent);
        } catch (Exception e){
            e.printStackTrace();
        }
            /** register broadcastReceiver for seekBar*/
            getActivity().registerReceiver(seekBarBroadcastReceiver,new IntentFilter(TalePlay_Service.BROADCAST_ACTION));
            mSeekBarBroadcastIsRegistered=true;
        }else {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
            alertDialog.setTitle("Відсутній доступ до мережі...");
            alertDialog.setMessage("Увімкніть будь-ласка мережу та спробуйте знову");
            alertDialog.setButton("Добре", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alertDialog.setIcon(R.drawable.ic_launcher);
            btn_PlayResume.setImageResource(R.drawable.select_btn_pause);
            alertDialog.show();
        }
    }

    private void stopTalePlay_Service(){
        try {
            getActivity().stopService(serviceIntent);
        } catch (Exception e){
            e.printStackTrace();
        }
        /** unregister broadcastReceiver for seekBar*/
        if (mSeekBarBroadcastIsRegistered){
            try {
                getActivity().unregisterReceiver(seekBarBroadcastReceiver);
                mSeekBarBroadcastIsRegistered = false;
            } catch (Exception e){
                e.printStackTrace();
            }
        }
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
        seekProgress=Integer.parseInt(taleAudioPosition);
        seekMax = Integer.parseInt(taleAudioMaxPosition);
        audioTaleEnded = Integer.parseInt(song_ended);
        audioTaleSeekBar.setMax(seekMax);
        audioTaleSeekBar.setProgress(seekProgress);
        if (audioTaleEnded==1){
            btn_PlayResume.setImageResource(R.drawable.select_btn_play);
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
    public void onStartTrackingTouch(SeekBar seekBar) {

    }
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}

