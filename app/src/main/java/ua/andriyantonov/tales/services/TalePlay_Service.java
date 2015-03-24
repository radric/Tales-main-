package ua.andriyantonov.tales.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.daos.UpdateTalesData;
import ua.andriyantonov.tales.activities.AudioActivity;

/**
 * Provides playing audio, sending Notification and broadcast to activity
 */


public class TalePlay_Service extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener{

    public static final String BROADCAST_ADIALOG = "ua.andriyantonov.tales.ADIALOG";
    public static final String BROADCAST_BUFFER = "ua.andriyantonov.tales.broadcastbuffer";
    public final static String BROADCAST_ACTION = "ua.andriyantonov.tales.action";

    private int mTaleAudioPos, mTaleAudioMaxDur;
    private static int sAudioTaleEnded;
    private final static int NOTIFICATION_ID=1;

    private boolean mIsPausedInCall;

    private final Handler mHandler = new Handler();
    private Intent mAfterCallIntent, mSeekIntent, mBufferIntent;
    private static MediaPlayer sPlayer = new MediaPlayer();
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;


    /**
     * Registers Intents and listeners for mPlayer
     */
    @Override
    public void onCreate(){
        mBufferIntent = new Intent(BROADCAST_BUFFER);
        mSeekIntent = new Intent(BROADCAST_ACTION);
        mAfterCallIntent = new Intent(BROADCAST_ADIALOG);
        sPlayer.setOnCompletionListener(this);
        sPlayer.setOnPreparedListener(this);
        sPlayer.setOnErrorListener(this);
        sPlayer.setOnBufferingUpdateListener(this);
        sPlayer.setOnSeekCompleteListener(this);
        sPlayer.setOnInfoListener(this);
    }

    /**
     * When Service start
     */
    @Override
    public int onStartCommand(final Intent intent,int flags,int startId){
        registerReceivers();
        initNotification();
        regTelephonyManager();
        prepareAudioFile();
        /**
         * Shows buffering progress bar if playing online
         */
        if (!UpdateTalesData.mCheckTaleExist.exists()){sendBufferingBroadcast();}
        /**
         * Setup seekbar handler
         */
        setupHandler();
        return START_NOT_STICKY;
    }

    /**
     * set up receiver for seekBar and PlayResume btns change
     */
    public void registerReceivers(){

        getApplication().registerReceiver(seekBarChangedBroadcastReceiver,
                new IntentFilter(AudioActivity.BROADCAST_SEEKBAR));
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(switchPlayPauseBroadcastReceiver,
                new IntentFilter(AudioActivity.BROADCAST_switchPlayStatus));
    }

    /**
     * Registers the listener for telephony manager.
     * Manages incoming phone calls during playback.
     */
    public void regTelephonyManager(){
        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state,String incomingNumber){
                switch (state){
                    case  TelephonyManager.CALL_STATE_OFFHOOK:
                        break;
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (sPlayer!=null){
                            pauseTaleAudio();
                            mIsPausedInCall=true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (sPlayer!=null){
                            if (mIsPausedInCall){
                                mIsPausedInCall=false;
                                pauseTaleAudio();
                                sendAfterCallADialogBroadcast();
                            }
                        }
                        break;
                }
            }
        };
        mTelephonyManager.listen(mPhoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Checks if the tale was already downloaded and mp3 file existed on devise storage
     * if it was - playing mp3 from storage
     * if not - buffering and playing from cloudService
     */
    public void prepareAudioFile(){

        UpdateTalesData.loadTalesData(getApplicationContext());
        int talePosition = UpdateTalesData.sTalePosition;
        String dataSource;
        if (UpdateTalesData.mCheckTaleExist.exists()){
            dataSource = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+
                    getResources().getString(R.string.app_name)+"/"+
                    getResources().getString(R.string.mainAudioTale_name)+ talePosition +".mp3";
        } else {
            dataSource =UpdateTalesData.sData_HTTP;
        }
        /** set data source for player and get prepared*/
        if (!sPlayer.isPlaying()){
            try {
                sPlayer.setDataSource(dataSource);
                /** send message to activity to progress uploading dialog*/
                sPlayer.prepareAsync();
            }catch (IllegalArgumentException | IllegalStateException | IOException e){
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        if (sPlayer!=null){
            if (sPlayer.isPlaying()){
                sPlayer.stop();
            }
            sPlayer.reset();
        }
        stopSelf();
        if (mPhoneStateListener!=null){
            mTelephonyManager.listen(mPhoneStateListener,PhoneStateListener.LISTEN_NONE);
        }
        mHandler.removeCallbacks(sendUpdatesToUI);
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
        if (!sPlayer.isPlaying()){
            sPlayer.start();
        }
    }
    public void pauseTaleAudio(){
        if (sPlayer.isPlaying()){
            sPlayer.pause();
        }
    }

    private void setupHandler(){
        mHandler.removeCallbacks(sendUpdatesToUI);
        mHandler.postDelayed(sendUpdatesToUI, 0);
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        @Override
        public void run() {
            LogTaleAudioPosition();
            mHandler.postDelayed(this,1000);
        }
    };

    /**
     * Prepares playing data and sending broadcast to Activity
     */
    private void LogTaleAudioPosition(){
        if(sPlayer.isPlaying()){
            mTaleAudioPos = sPlayer.getCurrentPosition();
            mTaleAudioMaxDur = sPlayer.getDuration();
            mSeekIntent.putExtra("counter",String.valueOf(mTaleAudioPos));
            mSeekIntent.putExtra("audioMax",String .valueOf(mTaleAudioMaxDur));
            mSeekIntent.putExtra("song_ended",String .valueOf(sAudioTaleEnded));
            String maxDurationText = convertFormat(mTaleAudioMaxDur);
            mSeekIntent.putExtra("audioMaxText",maxDurationText);
            String currTimePosText = convertFormat(mTaleAudioPos);
            mSeekIntent.putExtra("currTimePosText",currTimePosText);
           sendBroadcast(mSeekIntent);
        }
    }

    /**
     * Converts player time from miliseconds to current format
     */
    public String convertFormat(long miliSeconds){
        long s = TimeUnit.MILLISECONDS.toSeconds(miliSeconds)%60;
        long m = TimeUnit.MILLISECONDS.toMinutes(miliSeconds)%60;
        return String .format("%02d:%02d",m,s);
    }

    /**
     * Gets btns  position from activity
     */
    private BroadcastReceiver switchPlayPauseBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switchPlayPause(intent);
        }
    };
    /**
     * Switch play or pause depending of position.
     */
    public void switchPlayPause(Intent intent){
        int switchPlayStatus = intent.getIntExtra("switchPlayStatus", -1);
        if (switchPlayStatus ==1){
            pauseTaleAudio();
        } else if (switchPlayStatus ==2){
            playTaleAudio();
        }
    }

    /**
     * Receives seekbar position if it has been changed by the user in activity
     */
    private BroadcastReceiver seekBarChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekBarPosition(intent);
        }
    };
    /**
     * Updates seekbar position if it has been changed by the user in activity
     */
    public void updateSeekBarPosition(Intent intent){
        int seekPos = intent.getIntExtra("seekPos",0);
        if(sPlayer.isPlaying()){
            mHandler.removeCallbacks(sendUpdatesToUI);
            sPlayer.seekTo(seekPos);
            setupHandler();
        }
    }

    /**
     * Sends broadcast to activity that audio is being prepared and buffering started
     */
    public void sendBufferingBroadcast(){
        mBufferIntent.putExtra("buffering","1");
        sendBroadcast(mBufferIntent);
    }

    /**
     * Sends broadcast to activity that audio is prepared and ready to start playing
     * Finish buffering.
     */
    public void sendBufferCompleteBroadcast(){
        mBufferIntent.putExtra("buffering","0");
        sendBroadcast(mBufferIntent);
    }

    /**
     * Sends broadcast to activity that call was finished
     */
    public void sendAfterCallADialogBroadcast(){
        mAfterCallIntent.putExtra("aDialogIntent",0);
        sendBroadcast(mAfterCallIntent);
    }

    /**
     *Sends broadcast when playing audio completed
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        sAudioTaleEnded=1;
        UpdateTalesData.saveTalesIntData(getApplicationContext(),"audioTaleEnded",sAudioTaleEnded);
        sendBroadcast(mSeekIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!sPlayer.isPlaying()){
            playTaleAudio();
        }
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }


    /**
     * Sets notification
     */
    private void initNotification(){
        Context context = getApplicationContext();
        Intent notifIntent = new Intent(this, AudioActivity.class);
        notifIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0,notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Resources res = context.getResources();
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_lnotification)
                .setLargeIcon(BitmapFactory.decodeResource(res,R.mipmap.ic_lnotification))
                .setTicker(res.getString(R.string.tickerText))
                .setWhen(2)
                .setAutoCancel(false)
                .setContentTitle(res.getString(R.string.contentTitle))
                .setContentText(UpdateTalesData.sTaleName);
        Notification notification = builder.build();
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFICATION_ID, notification);



    }

    private void cancelNotification(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

    }

//    private void initNotification(){
//        CharSequence tikerText = getResources().getString(R.string.tickerText);
//        NotificationManager mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
//        Notification notification = new Notification(R.mipmap.ic_lnotification,tikerText,System.currentTimeMillis());
//        Context context = getApplicationContext();
//        CharSequence contentTitle = getResources().getString(R.string.contentTitle);
//        CharSequence contentText = UpdateTalesData.sTaleName;
//        Intent notifIntent = new Intent(this, AudioActivity.class);
//        notifIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent contentIntent = PendingIntent.getActivity(context,0,notifIntent,PendingIntent.FLAG_UPDATE_CURRENT);
//        notification.setLatestEventInfo(context,contentTitle,contentText,contentIntent);
//        Log.d("", "" + notifIntent.getExtras());
//        mNotificationManager.notify(NOTIFICATION_ID, notification);
//    }
}
