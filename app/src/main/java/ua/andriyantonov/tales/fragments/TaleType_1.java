package ua.andriyantonov.tales.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.andriyantonov.tales.TalesSettings;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalePlay_Service;

public class TaleType_1 extends Fragment implements AdapterView.OnItemClickListener {
    private Intent serviceIntent;
    private int talePosition;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taletype_1,container,false);

        serviceIntent = new Intent(getActivity(),TalePlay_Service.class);
        TalesSettings.loadStringListView(getActivity());
        String [] fileList = TalesSettings.fileList;

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
        TalesSettings.loadTaleItemPosition(getActivity());
        FragmentManager fm = null;
        Fragment fragment = null;

//        if(position!=talePosition){
            stopTalePlay_Service();
            fragment = new TaleActivity_Audio();
            String backStackName = fragment.getClass().getName();
            TalesSettings.saveTaleItemPosition(getActivity(), "talePosition", position);
            Log.d("", "position " + position);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(backStackName)
                    .setCustomAnimations(R.animator.show_fr,R.animator.remove_fr)
                    .commit();
//            } else {
//            fm.popBackStack(backStackName,-1);
//        }
    }

    private void stopTalePlay_Service(){
        try {
            getActivity().stopService(serviceIntent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
