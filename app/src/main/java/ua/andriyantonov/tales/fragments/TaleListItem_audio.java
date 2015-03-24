package ua.andriyantonov.tales.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import ua.andriyantonov.tales.activities.AudioActivity;
import ua.andriyantonov.tales.activities.SettingsActivity;
import ua.andriyantonov.tales.daos.DownloadAll;
import ua.andriyantonov.tales.daos.UpdateTalesData;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.services.TalePlay_Service;

/**
 * Shows ListItem of audioTales. When some item was chosen, starts activity with current tale.
 */

public class TaleListItem_audio extends Fragment implements AdapterView.OnItemClickListener {

    private static boolean mIsOnline;
    public static ProgressDialog sProgDialDownload=null;
    private int mTaleQuantity;
    private String[] mAllTalesUrl;
    private File mFolder;


    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_tale_listitem_audio,container,false);

        setHasOptionsMenu(true);

        UpdateTalesData.loadStringListView(getActivity());
        String [] fileList = UpdateTalesData.sFileList;

        ListView listView = (ListView) rootView.findViewById(R.id.taleType_1_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity().getApplicationContext(),
                R.layout.listitem_names,
                fileList
        );
        listView.setAdapter(adapter);
        mFolder = new File(UpdateTalesData.sFolderPath);
        sProgDialDownload = new ProgressDialog(getActivity());
        mTaleQuantity = listView.getCount();

        listView.setOnItemClickListener(this);
        
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        UpdateTalesData.loadTalesData(getActivity());
        if (position!=UpdateTalesData.sTalePosition){
            stopTalePlay_Service();
        }
        Intent intent = new Intent(getActivity(), AudioActivity.class);
        UpdateTalesData.saveTalesIntData(getActivity(), "talePosition", position);
        startActivity(intent);
    }

    /**
     * Stops Service and saves "isPlaying" flag for AudioActivity
     */
    private void stopTalePlay_Service(){
        Intent serviceIntent = new Intent(getActivity(),TalePlay_Service.class);
        try {
            getActivity().stopService(serviceIntent);
            UpdateTalesData.saveTalesIntData(getActivity(),UpdateTalesData.ISPLAYING_KEY,0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.audio, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_settings:
                Intent intent = new Intent(getActivity(),SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.action_download:
                if (UpdateTalesData.sFolderPath.isEmpty()|!mFolder.exists()){
                    checkConnectivity();
                    if (mIsOnline){
                        allDownLoadDialog();
                    }else {
                        doIfOffline();
                    }
                } else {
                    downloadAndDeleteDialog();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void downloadAll(){
        getArrayUrl();
        setProgDialDownload();
        final DownloadAll downloadAll = new DownloadAll(getActivity());
        downloadAll.execute(mAllTalesUrl);
        sProgDialDownload.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadAll.cancel(true);
            }
        });

    }

    private void getArrayUrl(){
        ArrayList<String> allUrls= new ArrayList<>();
        for (int i=0;i<mTaleQuantity;i++){
            UpdateTalesData.saveTalesIntData(getActivity(),"talePosition",i);
            UpdateTalesData.loadTalesData(getActivity());
            if (!UpdateTalesData.mCheckTaleExist.exists()){
                allUrls.add(UpdateTalesData.sData_HTTP);
            }
        }
        mTaleQuantity = allUrls.size();
        mAllTalesUrl = new String[allUrls.size()];
        mAllTalesUrl = allUrls.toArray(mAllTalesUrl);

    }

    private void deleteAll(){
        if (mFolder.isDirectory()){
            String[] children = mFolder.list();
            for (int i = 0; i < children.length; i++) {
                new File(mFolder, children[i]).delete();
            }
        }
        mFolder.delete();
        Toast.makeText(getActivity(), getString(R.string.ddd_del_success),
                Toast.LENGTH_SHORT).show();
    }

    private void setProgDialDownload(){
        sProgDialDownload.setTitle(getString(R.string.all_pdBuffer_message)+mTaleQuantity);
        sProgDialDownload.setMessage(getString(R.string.all_pdBuffer_inBuffer));
        sProgDialDownload.setIndeterminate(true);
        sProgDialDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        sProgDialDownload.setCancelable(true);
    }

    /**
     * Shows alertDialog if user want to download all audioTale from url
     */
    public void allDownLoadDialog(){
        AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
        ad.setMessage(getString(R.string.all_dl_message));
        ad.setButton(getString(R.string.all_dl_btnPos),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadAll();
                    }
                });
        ad.setButton2(getResources().getString(R.string.all_dl_btnNeg),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        ad.setCancelable(true);
        ad.show();
    }

    /**
     * Shows alertDialog if user want to download or deleteall audioTale from url
     */
    public void downloadAndDeleteDialog(){
        AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
        ad.setMessage(getString(R.string.ddd_message));
        ad.setButton(getString(R.string.ddd_download),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkConnectivity();
                        if (mIsOnline){
                            downloadAll();
                        }else {
                            doIfOffline();
                        }
                    }
                });
        ad.setButton2(getResources().getString(R.string.ddd_delete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAll();
                    }
                });
        ad.setCancelable(true);
        ad.show();
    }

    public void checkConnectivity(){
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        mIsOnline = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting() || cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .isConnectedOrConnecting();
    }

    private void doIfOffline(){
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
        alertDialog.setTitle("Відсутній доступ до мережі...");
        alertDialog.setMessage("Увімкніть будь-ласка мережу та спробуйте знову");
        alertDialog.setButton("Добре", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
    }

}
