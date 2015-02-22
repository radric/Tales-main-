package ua.andriyantonov.tales.fragments;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import ua.andriyantonov.tales.R;

/**
 * Created by andriy on 19.02.15.
 */
public class TaleActivity_Audio extends Fragment {
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle saveInstanceBundle){
        final View rootView = inflater.inflate(R.layout.frg_taleactivity_audio,container,false);

        ImageButton btn_pl_Play = (ImageButton)rootView.findViewById(R.id.btn_pl_Play);
        ImageView btn_pl_Stop = (ImageButton)rootView.findViewById(R.id.btn_pl_Stop);


        AudioManager am = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
        TextView textView = (TextView)rootView.findViewById(R.id.taleName);

        Bundle bundle = this.getArguments();
        int intPosition = bundle.getInt("position");
        Log.d("","get position ="+intPosition);

        if (intPosition==0){
            Log.d("","set text");
            textView.setText("POLUSHILOS");
        }


        return rootView;
    }



    }

