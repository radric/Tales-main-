package ua.andriyantonov.tales.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import ua.andriyantonov.tales.TalesPlayer_Service;
import ua.andriyantonov.tales.R;

public class TaleActivity_Audio extends Fragment{
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taleactivity_audio,container,false);
        Bundle bundle = this.getArguments();
        final int talePosition = bundle.getInt("talePosition");




        TextView textView = (TextView)rootView.findViewById(R.id.taleName);
        ImageButton btn_pl_Play =(ImageButton)rootView.findViewById(R.id.btn_pl_Play);


        btn_pl_Play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                TalesPlayer_Service.AudioTalesPlayer(getActivity(), talePosition);
            }
        });


        return rootView;
    }


}

