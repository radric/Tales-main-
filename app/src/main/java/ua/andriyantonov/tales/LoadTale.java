package ua.andriyantonov.tales;
import android.media.MediaPlayer;

public class LoadTale {

    public static String data_HTTP;

    public static void getAudioTaleHTTP(int talePosition){
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
}
