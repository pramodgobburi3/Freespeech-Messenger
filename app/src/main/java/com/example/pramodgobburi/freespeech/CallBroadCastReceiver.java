package com.example.pramodgobburi.freespeech;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by GPTV on 4/5/2018.
 */

public class CallBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("CallBroadCastReceiver", "onReceived");
        Intent broadCastIntent = new Intent(context,CallService.class);
        broadCastIntent.putExtra("mCallerId", intent.getStringExtra("mCurrentUserId"));
        context.startService(broadCastIntent);
    }
}