package ua.andriyantonov.tales;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import java.io.IOException;
import ua.andriyantonov.tales.fragments.TaleActivity_Audio;

public class TalePlay_Service extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnInfoListener{

    private MediaPlayer mPlayer = new MediaPlayer();
    private String sntTaleAudioLink;

    private final static int NOTIFICATION_ID=1;
    private boolean isPausedInCall;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    //------Variables for buffer broadcast
    public static final String BROADCAST_BUFFER = "ua.andriyantonov.tales.broadcastbuffer";
    private Intent bufferIntent;
    //------Variables for seekbar progress
    public int taleAudioPosition;
    public int taleAudioMaxPosition;
    private final Handler handler = new Handler();
    private static int audioTaleEnded;
    public final static String BROADCAST_ACTION = "ua.andriyantonov.tales.action";
    private Intent seekIntent;
    private int playStatusInt=0;



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
                Log.d("","playstatus = " +playStatusInt );
                /**set up receiver for seekBar change*/
                getApplication().registerReceiver(seekBarChangedBroadcastReceiver, new IntentFilter(TaleActivity_Audio.BROADCAST_SEEKBAR));

                /** Manage incomingphone calls during playback
                 *     public static final String BROADCAST_BUFFER = "ua.andriyantonov.tales.broadcastbuffer";
                 e mp on incoming
                 * Resume on hangup  */
                telephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
                phoneStateListener = new PhoneStateListener(){
                    @Override
                    public void onCallStateChanged(int state,String incomingNumber){
                        switch (state){
                            case TelephonyManager.CALL_STATE_OFFHOOK:
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
                                        playTaleAudio();
                                    }
                                }
                                break;
                        }
                    }
                };

                /** register the listener with telephony manager */
                telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_CALL_STATE);


                initNotification();

                sntTaleAudioLink = intent.getExtras().getString("sentAudioLink");
                if (!mPlayer.isPlaying()){
                    try {
                        mPlayer.setDataSource(sntTaleAudioLink);

                        /** send message to activity to progress uploading dialog*/
                        sendBufferingBroadcast();
                        mPlayer.prepareAsync();
                    }catch (IllegalArgumentException e){
                        e.printStackTrace();
                    }catch (IllegalStateException e){
                        e.printStackTrace();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
                /** set up seekbar handler*/
                setupHandler();

        return START_NOT_STICKY;
    }


    public void firstStart(){

    }
    public void anyCaseStart(){

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
        cancelNotification();
        if (phoneStateListener!=null){
            telephonyManager.listen(phoneStateListener,PhoneStateListener.LISTEN_NONE);
        }

        handler.removeCallbacks(sendUpdatesToUI);

        getApplication().unregisterReceiver(seekBarChangedBroadcastReceiver);
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
    public void stopTaleAudio(){
        if (!mPlayer.isPlaying()){
            mPlayer.stop();
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

    private void LogTaleAudioPosition(){
        if(mPlayer.isPlaying()){
            taleAudioPosition = mPlayer.getCurrentPosition();
            taleAudioMaxPosition = mPlayer.getDuration();
            seekIntent.putExtra("counter",String.valueOf(taleAudioPosition));
            seekIntent.putExtra("audioMax",String .valueOf(taleAudioMaxPosition));
            seekIntent.putExtra("song_ended",String .valueOf(audioTaleEnded));
            sendBroadcast(seekIntent);
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
        stopTaleAudio();
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
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        Context context = getApplicationContext();
        CharSequence contentTitle = getResources().getString(R.string.contentTitle);
        CharSequence contentText = getResources().getString(R.string.contentText);
        Intent notifIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent contentIntent = PendingIntent.getActivity(context,0,notifIntent,0);
        notification.setLatestEventInfo(context,contentTitle,contentText,contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID,notification);
    }

    private void cancelNotification(){
        NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
