package ua.andriyantonov.tales;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

public class TalesPlayer_Service {

    private static String data_HTTP;
    private static MediaPlayer mediaPlayer =null;

    public static void loadAudioTale(int talePosition){
        switch (talePosition){
            case 0:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_1.mp3";
                break;
            case 1:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_2.mp3";
                break;
            case 2:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_3.mp3";
                break;
            case 3:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_4.mp3";
                break;
            case 4:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_5.mp3";
                break;
            case 5:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_6.mp3";
                break;
            case 6:
                data_HTTP="http://tales.parseapp.com/audioTales/audioTale_7.mp3";
                break;
        }
    }

    public static void AudioTalesPlayer (Activity activity, int position){
        loadAudioTale(position);

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(data_HTTP);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            mediaPlayer.prepareAsync();
        }catch (IOException e){
            e.printStackTrace();

        }
    }

}
