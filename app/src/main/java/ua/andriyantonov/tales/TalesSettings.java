package ua.andriyantonov.tales;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ua.andriyantonov.tales.fragments.TaleActivity_Audio;

public class TalesSettings extends Activity{

    public static String data_HTTP;
    public static String taleName, taleText;
    private static InputStream is;
    private static AssetManager assetM;
    private static int size;
    private static byte[] buffer;
    private static SharedPreferences shp;
    private static int talePosition,mListItemPosition;

    public static void loadTaleItemPosition(Context context){
        shp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        talePosition = shp.getInt("talePosition",-1);
        mListItemPosition = shp.getInt("mListItemPosition",-1);
        data_HTTP="http://tales.parseapp.com/audioTales/audioTale_"+talePosition+".mp3";
    }

    public static void getTaleText(Context context){
        loadTaleItemPosition(context);
        assetM = context.getAssets();
        try {
            is = assetM.open("TaleType_"+mListItemPosition+"/text_"+talePosition+".txt");
            size = is.available();
            buffer = new byte[size];
            is.read(buffer);
            is.close();
            taleText = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static void getTaleName(Context context){
        loadTaleItemPosition(context);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets()
                    .open("TaleType_"+mListItemPosition+"/names/name_"+talePosition+".txt")));
            String line;
            while ((line=br.readLine())!=null){
                taleName = line;
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void saveTaleItemPosition(Context context,String key,int value){
        shp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(key,value);
        editor.apply();
    }

    public static void loadTalesListView(Context context, View view,ListView listView){
    }


}
