package ua.andriyantonov.tales.daos;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.activities.AudioActivity;
import ua.andriyantonov.tales.fragments.TaleListItem_audio;

/**
 * Manage the process of downloading audioTale, saving it to externalStorage,
 * sending broadcast to activity
 */

public class DownloadCurrent extends AsyncTask<String , Integer,String>{
    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private InputStream is = null;

    public DownloadCurrent(Context context){
        this.mContext = context;
    }

    /**
     * Manages downloading process(sets connection, path and name for downloaded file, downloading file)
     */
    @Override
    protected String doInBackground(String... params) {
        UpdateTalesData.loadTalesData(mContext);
        int talePosition = UpdateTalesData.sTalePosition;
        OutputStream os = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "server returned http " + connection.getResponseCode() + " " +
                        connection.getResponseMessage();
            }
            int fileLength = connection.getContentLength();
            is = connection.getInputStream();
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath()+"/"
                    +mContext.getResources().getString(R.string.app_name));
            sdPath.mkdirs();
            os = new FileOutputStream(sdPath+"/"
                    +mContext.getString(R.string.mainAudioTale_name)+ talePosition +".mp3");
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = is.read(data)) != -1) {
                total += count;
                // publishing the progress....
                if (fileLength > 0) { // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                    os.write(data, 0, count);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null)
                    os.close();
                if (is != null)
                    is.close();
            } catch (IOException ignored) {
            }
            if (connection != null)
                connection.disconnect();
            return null;
        }
    }


    /**
     * Makes screen always turned on (needed for proper downloading)
     * Shows progressDialog in audioActivity
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
        AudioActivity.sProgDialDownload.setButton(mContext.getString(R.string.pdBuffer_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    is.close();
                    dialog.cancel();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        AudioActivity.sProgDialDownload.setButton2(mContext.getString(R.string.pdBuffer_hide), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    TaleListItem_audio.sProgDialDownload.hide();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        AudioActivity.sProgDialDownload.show();
    }

    /**
     * Sets progressDialog for downloading process
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        AudioActivity.sProgDialDownload.setIndeterminate(false);
        AudioActivity.sProgDialDownload.setMax(100);
        AudioActivity.sProgDialDownload.setProgress(progress[0]);
    }

    /**
     * Makes screen work as usual after successful download.
     * Updates UI of AudioActivity
     */
    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        AudioActivity.sProgDialDownload.dismiss();
        UpdateTalesData.loadTalesData(mContext);
        AudioActivity.sBtn_taleDownload.setImageResource(R.drawable.btn_download_p);
        if (result != null)
            Toast.makeText(mContext,mContext.getResources().getString(R.string.download_error)+
                    result, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mContext,mContext.getResources().getString(R.string.download_success),
                    Toast.LENGTH_SHORT).show();
    }

}
