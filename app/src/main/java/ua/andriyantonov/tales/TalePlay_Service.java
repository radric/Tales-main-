package ua.andriyantonov.tales;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ua.andriyantonov.tales.fragments.TaleActivity_Audio;

public class TalePlay_Service extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener{

    public MediaPlayer mPlayer = new MediaPlayer();
    private int switchInt;

    private final static int NOTIFICATION_ID=1;
    private boolean isPausedInCall;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    //------Variables for buffer broadcast
    public static final String BROADCAST_BUFFER = "ua.andriyantonov.tales.broadcastbuffer";
    private Intent bufferIntent;
    //------Variables for seekbar progress
    public int taleAudioPosition;
    public int taleAudioMaxDuration;
    private final Handler handler = new Handler();
    private static int audioTaleEnded;
    public final static String BROADCAST_ACTION = "ua.andriyantonov.tales.action";
    private Intent seekIntent;


    @Override
    public void onCreate(){
        bufferIntent = new Intent(BROADCAST_BUFFER);
        seekIntent = new Intent(BROADCAST_ACTION);

        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnBufferingUpdateListener(this);
        mPlayer.setOnSeekCompleteListener(this);
        mPlayer.setOnInfoListener(this);
        mPlayer.reset();
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        /**set up receiver for seekBar change and PlayResume btns*/
        getApplication().registerReceiver(seekBarChangedBroadcastReceiver, new IntentFilter(TaleActivity_Audio.BROADCAST_SEEKBAR));
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(switchPlayPauseBroadcastReceiver,new IntentFilter(TaleActivity_Audio.BROADCAST_switchStatus));

        initNotification();

        /** Manage incomingphone calls during playback
        *     public static final String BROADCAST_BUFFER = "ua.andriyantonov.tales.broadcastbuffer";
        e mp on incoming
        * Resume on hangup  */
        telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);

        /** register the listener with telephony manager */


        phoneStateListener = new PhoneStateListener(){
                    @Override
                    public void onCallStateChanged(int state,String incomingNumber){
            switch (state){
                case  TelephonyManager.CALL_STATE_OFFHOOK:
                break;
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mPlayer!=null){
                    pauseTaleAudio();
                    isPausedInCall=true;
                    }
                    break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        /** need to make alertDialog and ask "do you want to resume?"  */
                        if (mPlayer!=null){
                        if (isPausedInCall){
                        isPausedInCall=false;
                            pauseTaleAudio();
                            }
                        }
                        break;
            }
            }
                };
        telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);

        if (!mPlayer.isPlaying()){
            try {
                TalesSettings.loadTaleItemPosition(getApplicationContext());
                mPlayer.setDataSource(TalesSettings.data_HTTP);
                /** send message to activity to progress uploading dialog*/
                mPlayer.prepareAsync();
            }catch (IllegalArgumentException e){
                e.printStackTrace();
            }catch (IllegalStateException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
                sendBufferingBroadcast();
                /** set up seekbar handler*/
                setupHandler();
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mPlayer!=null){
            if (mPlayer.isPlaying()){
                mPlayer.stop();
            }
            mPlayer.release();
        }
        stopSelf();
        if (phoneStateListener!=null){
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

        handler.removeCallbacks(sendUpdatesToUI);
        cancelNotification();
        getApplication().unregisterReceiver(seekBarChangedBroadcastReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(switchPlayPauseBroadcastReceiver);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) { }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Toast.makeText(this,"MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK" +extra,
                        Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Toast.makeText(this,"MEDIA_ERROR_SERVER_DIED" +extra,
                        Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Toast.makeText(this,"MEDIA_ERROR_UNKNOWN" +extra,
                        Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        sendBufferCompleteBroadcast();
        playTaleAudio();
    }

    public void playTaleAudio(){
        if (!mPlayer.isPlaying()){
            mPlayer.start();
        }
    }
    public void pauseTaleAudio(){
        if (mPlayer.isPlaying()){
            mPlayer.pause();
        }
    }

    private void setupHandler(){
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000);
    }
    private Runnable sendUpdatesToUI = new Runnable() {
        @Override
        public void run() {
            LogTaleAudioPosition();
            handler.postDelayed(this,1000);
        }
    };

    public void afterCallDialog(){
        AlertDialog ad = new AlertDialog.Builder(getApplication()).create();
        ad.setMessage(getResources().getString(R.string.ad_afterCall_message));
        ad.setButton(getResources().getString(R.string.ad_afterCall_btnPos),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                playTaleAudio();
            }
        });
        ad.setButton(getResources().getString(R.string.ad_afterCall_btnNeg), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        ad.setCancelable(true);
        ad.show();
    }

    private void LogTaleAudioPosition(){
        if(mPlayer.isPlaying()){
            taleAudioPosition = mPlayer.getCurrentPosition();
            taleAudioMaxDuration = mPlayer.getDuration();
            seekIntent.putExtra("counter",String.valueOf(taleAudioPosition));
            seekIntent.putExtra("audioMax",String .valueOf(taleAudioMaxDuration));
            seekIntent.putExtra("song_ended",String .valueOf(audioTaleEnded));
            String maxDurationText = convertFormat(taleAudioMaxDuration);
            seekIntent.putExtra("audioMaxText",maxDurationText);

            String currTimePosText = convertFormat(taleAudioPosition);
            seekIntent.putExtra("currTimePosText",currTimePosText);

            sendBroadcast(seekIntent);
        }
    }

    public String convertFormat(long miliSeconds){
        long s = TimeUnit.MILLISECONDS.toSeconds(miliSeconds)%60;
        long m = TimeUnit.MILLISECONDS.toMinutes(miliSeconds)%60;
        return String .format("%02d:%02d",m,s);
    }

    /** receive player position (play or pause) if it has been changed by the user in fragment*/
    private BroadcastReceiver switchPlayPauseBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switchPlayPause(intent);
        }
    };
    public void switchPlayPause(Intent intent){
        switchInt = intent.getIntExtra("switchStatus",-1);
        if (switchInt==1){
            pauseTaleAudio();
        } else if (switchInt==2){
            playTaleAudio();
        }
    }

    /** receive seekbar position if it has been changed by the user in fragment*/
    private BroadcastReceiver seekBarChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekBarPosition(intent);
        }
    };
    public void updateSeekBarPosition(Intent intent){
        int seekPos = intent.getIntExtra("seekPos",0);
        if(mPlayer.isPlaying()){
            handler.removeCallbacks(sendUpdatesToUI);
            mPlayer.seekTo(seekPos);
            setupHandler();
        }
    }

    /** send message to activity that audio is being prepared and buffering started*/
    public void sendBufferingBroadcast(){
        bufferIntent.putExtra("buffering","1");
        sendBroadcast(bufferIntent);
    }

    /** send message to activity that audio is prepared and ready to start playing*/
    public void sendBufferCompleteBroadcast(){
        bufferIntent.putExtra("buffering","0");
        sendBroadcast(bufferIntent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopSelf();
        audioTaleEnded=1;
        seekIntent.putExtra("song_ended",String .valueOf(audioTaleEnded));
        sendBroadcast(seekIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!mPlayer.isPlaying()){
            playTaleAudio();
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    private void initNotification(){
        CharSequence tikerText = getResources().getString(R.string.tickerText);
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_launcher,tikerText,System.currentTimeMillis());
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        Context context = getApplicationContext();
        CharSequence contentTitle = getResources().getString(R.string.contentTitle);
        CharSequence contentText = TalesSettings.taleName;
        Intent notifIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        notifIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notifIntent.putExtra("showAudioFrag",true);
        PendingIntent contentIntent = PendingIntent.getActivity(context,0,notifIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(context,contentTitle,contentText,contentIntent);
        Log.d("", "" + notifIntent.getExtras());
        mNotificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void cancelNotification(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
