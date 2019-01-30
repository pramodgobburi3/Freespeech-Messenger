package com.example.pramodgobburi.freespeech;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.calling.CallListener;

import java.util.List;


public class CallService extends Service {

    //Variables for Sinch Server
    private static final String APP_KEY = "f1e9c8bd-a697-42ad-bdc3-3b9b8720d904";
    private static final String APP_SECRET = "qaoaRmuZi0W7uuDF+DDm3w==";
    private static final String ENVIRONMENT = "sandbox.sinch.com";

    int mStartMode;       // indicates how to behave if the service is killed
    IBinder mBinder;      // interface for clients that bind
    boolean mAllowRebind; // indicates whether onRebind should be used



    private Call call;
    private SinchClient sinchClient;
    private String mRecipientId;
    private FirebaseAuth mAuth;
    private String mCurrentId;
    private DatabaseReference mUsersDatabase;



    public static final int RESULT_OK = -1;
    public static final int RESULT_BAD = 0;
    public static final String KEY_RECEIVER = "KEY_RECEIVER";
    public static final String KEY_MESSAGE = "KEY_MESSAGE";

    private MediaPlayer ringer;
    AudioManager audioManager;

    public boolean isCallOngoing;



    @Override
    public void onCreate() {
        // The service is being created
        super.onCreate();
        Log.v("CallService", "OnCreate Called");

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()

        //Log.v("callService", "The Service has started");

        //Minor Bug
        if(mCurrentId == null) {
            mCurrentId = intent.getStringExtra("mCallerId");
        }

        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(mCurrentId)
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT)
                .build();

        sinchClient.setSupportCalling(true);                    //Says "I want to have calling in this app"
        sinchClient.startListeningOnActiveConnection();         //Listens for incoming calls
        sinchClient.start();                                    //Starts the sinchClient

        sinchClient.getCallClient().addCallClientListener(new CallService.SinchCallClientListener());

        //ringer = MediaPlayer.create(getApplicationContext(), Settings.System.DEFAULT_RINGTONE_URI);
        ringer = MediaPlayer.create(getApplicationContext(), R.raw.trap_android);

        ringer.setLooping(true);


        return START_REDELIVER_INTENT;
    }
    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return null;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.v("CallService","Service is killed");
        //Intent broadCastIntent = new Intent("CallService.ended");
        //broadCastIntent.putExtra("mCurrentUserId", mCurrentId);
        //sendBroadcast(broadCastIntent);
        super.onDestroy();
    }

    public void transferToCallActivity()
    {
        Intent callIntent = new Intent(CallService.this, CallActivity.class);
        callIntent.putExtra("response", true);
        callIntent.putExtra("user_id",call.getRemoteUserId());
        callIntent.putExtra(KEY_RECEIVER,new CallReceiver());
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(callIntent);
    }


    private void stopRinger()
    {
        audioManager.setSpeakerphoneOn(false);
        ringer.stop();
    }


    private class SinchCallClientListener implements CallClientListener {
        @Override
        public void onIncomingCall(CallClient callClient, Call incomingCall) {
            //Pick up the call! Incoming Call Intent, Play ringtone

            if (incomingCall == null)
            {
                ringer.stop();
            }
            else {


                audioManager.setSpeakerphoneOn(true);
                ringer.start();
                call = incomingCall;

                Intent answerIntent = new Intent(CallService.this, AnswerActivity.class);
                answerIntent.putExtra("caller_id", incomingCall.getRemoteUserId());
                answerIntent.putExtra(KEY_RECEIVER, new CallReceiver());
                answerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(answerIntent);
            }
        }
    }




    private class CallReceiver extends ResultReceiver   {
        public CallReceiver() {
            super(null);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData)   {
            if(resultCode != RESULT_OK)
            {
                isCallOngoing = false;
                stopRinger();

                if (call != null) {
                    call.hangup();
                    call = null;
                }

                Intent mainIntent = new Intent(CallService.this, MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                //Call Activity
            }
            else
            {
                isCallOngoing = true;
                stopRinger();
                call.answer();
                //call.addCallListener(new SinchCallListener());
                transferToCallActivity();
            }
        }
    }



    private class SinchCallListener implements CallListener {


        @Override
        public void onCallEnded(Call endedCall) {
            //call ended by either party
            call = null;
            SinchError a = endedCall.getDetails().getError();

        }

        @Override       //incoming call was picked up
        public void onCallEstablished(Call establishedCall) {

        }
        @Override
        public void onCallProgressing(Call progressingCall) {
            //call is ringing
        }
        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            //send Call notifications
        }

    }

}




