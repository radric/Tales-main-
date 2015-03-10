package ua.andriyantonov.tales;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class UpdateTalesData extends Activity{

    public static String data_HTTP, taleName, taleText;
    public static int talePosition,mListItemPosition,isPlaying,audioTaleEnded;
    public static String[] fileList;
    public static Context context;
    public static File checkTaleExist;

    private static InputStream is;
    private static AssetManager assetM;
    private static int size;
    private static byte[] buffer;
    private static SharedPreferences shp;


    public static void getTaleText(Context context){
        loadTalesData(context);
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
        loadTalesData(context);
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

    public static void loadTalesData(Context context){
        shp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        talePosition = shp.getInt("talePosition", -1);
        mListItemPosition = shp.getInt("mListItemPosition",-1);
        data_HTTP="http://tales.parseapp.com/audioTales/"+
                context.getResources().getString(R.string.mainAudioTale_name)+talePosition+".mp3";

        checkTaleExist = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+
        context.getResources().getString(R.string.app_name)+"/"+
                context.getResources().getString(R.string.mainAudioTale_name)+talePosition+".mp3");

        isPlaying=shp.getInt("isPlaying",0);
        audioTaleEnded = shp.getInt("audioTaleEnded",0);
    }
    public static void loadStringListView(Context context){
        loadTalesData(context);
        assetM = context.getAssets();
        try {
            fileList = assetM.list("TaleType_"+mListItemPosition+"/names");
            if (fileList==null){
                // if dir empty
            } else {
                for (int i=0; i<fileList.length; i++){
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets()
                                .open("TaleType_"+mListItemPosition+"/names/"+fileList[i])));
                        String line;
                        while ((line=br.readLine())!=null){
                            taleName = line;
                            fileList[i] =taleName;
                        }
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void saveTalesIntData(Context context,String key,int value){
        shp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(key, value);
        editor.apply();
    }
    public static void saveTalesStringData(Context context,String key,String value){
        shp = context.getSharedPreferences("MySHP",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putString(key, value);
        editor.apply();
    }
    public static void saveTalesBooleanData(Context context,String key,boolean value){
        shp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putBoolean(key,value);
        editor.apply();
    }

}
