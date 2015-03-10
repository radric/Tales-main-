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

import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.ReadActivity;
import ua.andriyantonov.tales.UpdateTalesData;

public class TaleListItem_read extends Fragment implements AdapterView.OnItemClickListener {

    public View onCreateView(LayoutInflater inflater,ViewGroup container, Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_tale_listitem_read,container,false);

        UpdateTalesData.loadStringListView(getActivity());
        String [] fileList = UpdateTalesData.fileList;

        ListView listView = (ListView)rootView.findViewById(R.id.taleType_2_listView);
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
        Intent intent = new Intent(getActivity(), ReadActivity.class);
        UpdateTalesData.saveTalesIntData(getActivity(), "talePosition", position);
        startActivity(intent);
    }
}
