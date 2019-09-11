package com.example.user.project_app;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import com.github.pires.obd.enums.ObdProtocols;
import java.util.ArrayList;



public class MyConfigActivity extends PreferenceActivity  {


    public static final String PROTOCOLS_LIST_KEY = "obd_protocols_preference";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.my_preferences);

        ArrayList<CharSequence> protocolStrings = new ArrayList<>();
        ListPreference listProtocols = (ListPreference) getPreferenceScreen()
                .findPreference(PROTOCOLS_LIST_KEY);

        for (ObdProtocols protocol : ObdProtocols.values()) {
            protocolStrings.add(protocol.name());
        }
        listProtocols.setEntries(protocolStrings.toArray(new CharSequence[0]));
        listProtocols.setEntryValues(protocolStrings.toArray(new CharSequence[0]));


    }


}
