package ua.andriyantonov.tales.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.andriyantonov.tales.LoadTale;
import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalePlay_Service;

public class TaleType_1 extends Fragment implements AdapterView.OnItemClickListener {
    private Intent serviceIntent;

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taletype_1,container,false);

        serviceIntent = new Intent(getActivity(),TalePlay_Service.class);

        ListView listView = (ListView) rootView.findViewById(R.id.taleType_1_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity().getApplicationContext(),
                R.layout.listitem_tales,
                getResources().getStringArray(R.array.TaleType_1_array));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);

        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LoadTale.loadTaleItemPosition(getActivity());
   //     if(position!=talePosition){
            stopTalePlay_Service();
            Fragment fragment = new TaleActivity_Audio();
            LoadTale.saveTaleItemPosition(getActivity(),"talePosition", position);
            Log.d("", "position " + position);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
      //      }
    }

    private void stopTalePlay_Service(){
        try {
            getActivity().stopService(serviceIntent);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
