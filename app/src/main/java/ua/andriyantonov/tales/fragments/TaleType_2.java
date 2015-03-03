package ua.andriyantonov.tales.fragments;

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
import ua.andriyantonov.tales.TalesSettings;

public class TaleType_2 extends Fragment implements AdapterView.OnItemClickListener {

    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taletype_2,container,false);

        ListView listView = (ListView)rootView.findViewById(R.id.taleType_2_listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getActivity().getApplicationContext(),
                R.layout.listitem_names,
                getActivity().getResources().getStringArray(R.array.TaleType_2_array));
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(this);
        return rootView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Fragment fragment = new TaleActivity_Read();
        String backStackName = fragment.getClass().getName();
        Log.d("111",backStackName);
        TalesSettings.saveTaleItemPosition(getActivity(), "talePosition", position);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(backStackName)
                .setCustomAnimations(R.animator.show_fr,R.animator.remove_fr)
                .commit();
    }
}
