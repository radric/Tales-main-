package ua.andriyantonov.tales.fragments;

import android.content.Intent;
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

import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.activities.ReadActivity;
import ua.andriyantonov.tales.activities.SettingsActivity;
import ua.andriyantonov.tales.services.TalePlay_Service;
import ua.andriyantonov.tales.daos.UpdateTalesData;

/**
 * Shows ListItem of textTales. When some item was chosen, starts activity with current tale.
 */
public class TaleListItem_read extends Fragment implements AdapterView.OnItemClickListener {

    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_tale_listitem_read,container,false);

        setHasOptionsMenu(true);
        UpdateTalesData.loadStringListView(getActivity());
        String [] fileList = UpdateTalesData.sFileList;

        ListView listView = (ListView)rootView.findViewById(R.id.taleType_2_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity().getApplicationContext(),
                R.layout.listitem_names,
                fileList
        );
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), ReadActivity.class);
        UpdateTalesData.saveTalesIntData(getActivity(), "talePosition", position);
        stopTalePlay_Service();
        startActivity(intent);
    }

    /**
     * Stops Service and saves "isPlaying" flag for ReadActivity
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
        inflater.inflate(R.menu.read, menu);
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
        }
        return super.onOptionsItemSelected(item);
    }
}
