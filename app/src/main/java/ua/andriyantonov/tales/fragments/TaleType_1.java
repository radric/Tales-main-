package ua.andriyantonov.tales.fragments;

import ua.andriyantonov.tales.fragments.TaleActivity_Audio;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ua.andriyantonov.tales.R;

public class TaleType_1 extends Fragment {

    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taletype_1,container,false);

        ListView listView = (ListView) rootView.findViewById(R.id.taleType_1_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity().getApplicationContext(),
                R.layout.listitem_tales,
                getResources().getStringArray(R.array.TaleType_1_array));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Bundle bundle = new Bundle();
                Fragment fragment = new TaleActivity_Audio();
                int ft;
                switch (position){
                    case 0:
                        bundle.putInt("possition",position);
                        Log.d("","setArguments = "+position);
                        fragment.setArguments(bundle);
                        ft = getFragmentManager().beginTransaction()
                                .replace(R.id.container, fragment)
                                .addToBackStack(null)
                                .commit();
                }
            }
        });



        return rootView;
    }
}
