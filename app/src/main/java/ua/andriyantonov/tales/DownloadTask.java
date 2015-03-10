package ua.andriyantonov.tales;

import android.content.Context;
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

public class DownloadTask extends AsyncTask<String , Integer,String>{
    private Context context;
    private PowerManager.WakeLock mWakeLock;
    private int talePosition;

    public DownloadTask(Context context){
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        UpdateTalesData.loadTalesData(context);
        talePosition=UpdateTalesData.talePosition;
        InputStream is = null;
        OutputStream os = null;
        HttpURLConnection connection = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "server returned http " + connection.getResponseCode() + " " + connection.getResponseMessage();
            }
            // this will be useful to display download percentage
            // might be -1: server did not report the length
            int fileLength = connection.getContentLength();
            is = connection.getInputStream();
            File sdPath = Environment.getExternalStorageDirectory();
            sdPath = new File(sdPath.getAbsolutePath()+"/"
                    +context.getResources().getString(R.string.app_name));
            sdPath.mkdirs();
            os = new FileOutputStream(sdPath+"/"
                    +context.getResources().getString(R.string.mainAudioTale_name)+talePosition+".mp3");
            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = is.read(data)) != -1) {
                if (isCancelled()) {
                    is.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) ; // only if total length is known
                publishProgress((int) (total * 100 / fileLength));
                os.write(data, 0, count);
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

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
        AudioActivity.dlProgDialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        AudioActivity.dlProgDialog.setIndeterminate(false);
        AudioActivity.dlProgDialog.setMax(100);
        AudioActivity.dlProgDialog.setProgress(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        AudioActivity.dlProgDialog.dismiss();
        UpdateTalesData.loadTalesData(context);
        AudioActivity.btn_taleDownload.setImageResource(R.drawable.btn_download_p);
        if (result != null)
            Toast.makeText(context,context.getResources().getString(R.string.dl_error)+ result, Toast.LENGTH_LONG).show();
        else
            Toast.makeText(context,context.getResources().getString(R.string.dl_success), Toast.LENGTH_SHORT).show();
    }
}
