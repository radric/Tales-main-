package ua.andriyantonov.tales.daos;

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

import ua.andriyantonov.tales.R;

/**
 * Saves and loads SharedPreferences data
 */

public class UpdateTalesData extends Activity{

    public final static String ISPLAYING_KEY="isPlaying";
    public static String sData_HTTP, sTaleName, sTaleText, sFolderPath,TaleType="TaleType_";
    public static String[] sFileList;

    public static int sTalePosition,sListItemPosition,sIsPlaying,sAudioTaleEnded;

    private static AssetManager mAssetMan;
    public static File mCheckTaleExist, sFolderExist;
    private static SharedPreferences mShp;


    /**
     * Gets tale text depending of taleType and chosen tale
     */
    public static void getTaleText(Context context){
        loadTalesData(context);
        mAssetMan = context.getAssets();
        try {
            String fileName=null;
            if (sTalePosition>=0&sTalePosition<10){
                fileName="text_00";
            } else if (sTalePosition>=10&sTalePosition<100){
                fileName="text_0";
            }
            else if (sTalePosition>=100){
                fileName="text_";
            }
            InputStream is = mAssetMan.open(TaleType + sListItemPosition +
                    "/"+fileName + sTalePosition + ".txt");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            sTaleText = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Gets tale name depending of taleType and chosen tale
     */
    public static void getTaleName(Context context){
        loadTalesData(context);
        try {
            String fileName=null;
            if (sTalePosition>=0&sTalePosition<10){
                fileName="name_00";
            } else if (sTalePosition>=10&sTalePosition<100){
                fileName="name_0";
            }
            else if (sTalePosition>=100){
                fileName="name_";
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets()
                    .open(TaleType+sListItemPosition+"/names/"+fileName+sTalePosition+".txt")));
            String line;
            while ((line=br.readLine())!=null){
                sTaleName = line;
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Loads all SharedPreference's data.
     */
    public static void loadTalesData(Context context){
        mShp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        sTalePosition = mShp.getInt("talePosition", -1);
        sListItemPosition = mShp.getInt("mListItemPosition", -1);
        sData_HTTP="http://tales.parseapp.com/audioTales/"+
                context.getString(R.string.mainAudioTale_name)+sTalePosition+".mp3";
        sFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+
                context.getString(R.string.app_name);
        sFolderExist = new File(sFolderPath);
        mCheckTaleExist = new File(sFolderPath+"/"+
                context.getString(R.string.mainAudioTale_name)+sTalePosition+".mp3");
        sIsPlaying=mShp.getInt(ISPLAYING_KEY,-1);
        sAudioTaleEnded = mShp.getInt("audioTaleEnded",0);
    }

    /**
     * Loads all available tales of chosen taleType into listView
     */
    public static void loadStringListView(Context context){
        loadTalesData(context);
        mAssetMan = context.getAssets();
        try {
            sFileList = mAssetMan.list(TaleType+sListItemPosition+"/names");
            if (sFileList!=null){
                for (int i=0; i<sFileList.length; i++){
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets()
                                .open(TaleType+sListItemPosition+"/names/"+sFileList[i])));
                        String line;
                        while ((line=br.readLine())!=null){
                            sTaleName = line;
                            sFileList[i] =sTaleName;
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

    /**
     * Saves int via SharedPreferences constructor
     */
    public static void saveTalesIntData(Context context,String key,int value){
        mShp = context.getSharedPreferences("MySHP", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mShp.edit();
        editor.putInt(key, value);
        editor.apply();
    }

}
