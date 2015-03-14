package ua.andriyantonov.tales.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.andriyantonov.tales.AudioActivity;
import ua.andriyantonov.tales.UpdateTalesData;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalePlay_Service;

public class TaleListItem_audio extends Fragment implements AdapterView.OnItemClickListener {
    private Intent serviceIntent;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_tale_listitem_audio,container,false);

        serviceIntent = new Intent(getActivity(),TalePlay_Service.class);
        UpdateTalesData.loadStringListView(getActivity());
        String [] fileList = UpdateTalesData.fileList;

        ListView listView = (ListView) rootView.findViewById(R.id.taleType_1_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
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
        UpdateTalesData.loadTalesData(getActivity());
        if (position!=UpdateTalesData.talePosition){
            stopTalePlay_Service();
        }
        Intent intent = new Intent(getActivity(), AudioActivity.class);
        UpdateTalesData.saveTalesIntData(getActivity(), "talePosition", position);
        startActivity(intent);
    }

    private void stopTalePlay_Service(){
        try {
            getActivity().stopService(serviceIntent);
            UpdateTalesData.saveTalesIntData(getActivity(),UpdateTalesData.isPlaying_key,
                    UpdateTalesData.isPlaying=0);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
