package ua.andriyantonov.tales.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ua.andriyantonov.tales.R;
import ua.andriyantonov.tales.TalesSettings;

/**
 * Created by andriy on 19.02.15.
 */
public class TaleActivity_Read extends Fragment {

    TextView taleName,taleText;

    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle saveInstanceState){
        final View rootView = inflater.inflate(R.layout.frg_taleactivity_read,container,false);
        taleName = (TextView)rootView.findViewById(R.id.taleName);
        taleText = (TextView)rootView.findViewById(R.id.taleText);

        TalesSettings.getTaleName(getActivity());
        TalesSettings.getTaleText(getActivity());
        taleName.setText(TalesSettings.taleName);
        taleText.setText(TalesSettings.taleText);

        return rootView;
    }
}
