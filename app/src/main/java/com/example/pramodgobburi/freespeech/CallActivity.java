package com.example.pramodgobburi.freespeech;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.calling.CallClientListener;
import com.squareup.picasso.Picasso;

import java.util.List;




public class CallActivity extends AppCompatActivity {

    //Variables to access Sinch Server
    private static final String APP_KEY = "f1e9c8bd-a697-42ad-bdc3-3b9b8720d904";
    private static final String APP_SECRET = "qaoaRmuZi0W7uuDF+DDm3w==";
    private static final String ENVIRONMENT = "sandbox.sinch.com";

    private Call call;
    private SinchClient sinchClient;

    private String mRecipientUser;
    private String mRecipientId;
    private String mCallerId;
    private FirebaseAuth mAuth;
    private String mCurrentUser;
    private DatabaseReference mUsersDatabase;


    private ImageView btncall;
    private ImageView btnhangUp;
    private ImageView btnmuteMic;
    private ImageView btnspeakerPhone;

    private ImageView recipientImg;
    private TextView callStatus;
    private TextView callRecipient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        mRecipientId = getIntent().getStringExtra("user_id");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mUsersDatabase.child(mRecipientId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mRecipientUser = dataSnapshot.child("name").getValue().toString();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });



        mAuth = FirebaseAuth.getInstance();

        mCallerId = mAuth.getCurrentUser().getUid();
        mCurrentUser = mAuth.getCurrentUser().getDisplayName();


        sinchClient = Sinch.getSinchClientBuilder()
                .context(this)
                .userId(mCallerId)
                .applicationKey(APP_KEY)
                .applicationSecret(APP_SECRET)
                .environmentHost(ENVIRONMENT)
                .build();

        sinchClient.setSupportCalling(true);                    //Says "I want to have calling in this app"
        sinchClient.startListeningOnActiveConnection();         //Listens for incoming calls
        sinchClient.start();                                    //Starts the sinchClient



        initUI();

        btncall = (ImageView) findViewById(R.id.call);
        btnhangUp = (ImageView) findViewById(R.id.hangUp);
        btnmuteMic = (ImageView) findViewById(R.id.muteMic);
        btnspeakerPhone = (ImageView) findViewById(R.id.speaker);
        callStatus = (TextView) findViewById(R.id.callStatus);
        callRecipient = (TextView) findViewById(R.id.callRecipient);
        recipientImg = (ImageView) findViewById(R.id.callPicture);

        callStatus.setText("");


/*

        //Delays call for service to start
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                if(call == null) {
                    String[] Permissions = {Manifest.permission.RECORD_AUDIO};

                    if(!hasPermissions(CallActivity.this, Permissions)) {
                        ActivityCompat.requestPermissions(CallActivity.this, Permissions, 1);
                    }

                    call = sinchClient.getCallClient().callUser(mRecipientId);
                    call.addCallListener(new SinchCallListener());
                    btncall.setEnabled(false);
                }
                else
                {
                    btncall.setEnabled(true);
                    btnhangUp.setEnabled(false);
                }

            }
        }, 100);

*/

        //When call is clicked
        btncall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Move to OnCreate
                if(call == null) {
                    String[] Permissions = {Manifest.permission.RECORD_AUDIO};

                    if(!hasPermissions(CallActivity.this, Permissions)) {
                        ActivityCompat.requestPermissions(CallActivity.this, Permissions, 1);
                    }

                    call = sinchClient.getCallClient().callUser(mRecipientId);
                    call.addCallListener(new SinchCallListener());
                    btncall.setEnabled(true);
                    btnhangUp.setEnabled(false);
                }
                else
                {
                    btncall.setEnabled(true);
                    btnhangUp.setEnabled(true);
                }

            }
        });

        //When hangup is clicked
        btnhangUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(call == null)
                {
                    btnhangUp.setEnabled(true);
                    endReceivedCall();
                }
                else {
                    call.hangup();

                    btnhangUp.setEnabled(false);
                    btncall.setEnabled(true);
                }

                //callStatus.setText("Call Ended");
            }
        });

        //When mute mic is clicked, Toggle Mic Muting
        btnmuteMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                if(audioManager.isMicrophoneMute())
                {
                    audioManager.setMicrophoneMute(false);
                    btnmuteMic.getDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP);
                }
                else
                {
                    audioManager.setMicrophoneMute(true);
                    btnmuteMic.getDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
                }

            }
        });

        //When speaker phone is clicked, Toggle Speaker Phone
        btnspeakerPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

                if(audioManager.isSpeakerphoneOn())
                {
                    audioManager.setSpeakerphoneOn(false);
                    btnspeakerPhone.getDrawable().setColorFilter(Color.BLACK,PorterDuff.Mode.SRC_ATOP);
                }
                else
                {
                    audioManager.setSpeakerphoneOn(true);
                    btnspeakerPhone.getDrawable().setColorFilter(Color.GREEN,PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

    }

    private void initUI () {
        mUsersDatabase.child(mRecipientId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imageUrl = dataSnapshot.child("image").getValue().toString();
                Picasso.with(CallActivity.this).load(imageUrl).placeholder(R.drawable.default_profile_image).into(recipientImg);
                callRecipient.setText(mRecipientUser);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Fix, causes occasional crashes
    private void endReceivedCall()
    {
        ResultReceiver endReceiver = getIntent().getParcelableExtra(CallService.KEY_RECEIVER);
        Bundle resultData = new Bundle();
        resultData.putString(CallService.KEY_MESSAGE, "Hello world!");
        if(endReceiver != null) {
            endReceiver.send(CallService.RESULT_BAD, resultData);
        }
    }




    private class SinchCallListener implements CallListener  {


        @Override
        public void onCallEnded(Call endedCall) {
            //call ended by either party
            call = null;
            SinchError a = endedCall.getDetails().getError();
            btncall.setEnabled(true);

            //Volume buttons controlling the ringer, when not in a call
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            callStatus.setText("Call Ended");

        }

        @Override       //incoming call was picked up
        public void onCallEstablished(Call establishedCall) {

            //Volume buttons control phone call volume
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            callStatus.setText("Connected");
        }
        @Override
        public void onCallProgressing(Call progressingCall) {
            //call is ringing
            callStatus.setText("Ringing");
        }
        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            //send Call notifications
        }

    }



    public static boolean hasPermissions(Context context, String... permissions) {

        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M && context!=null && permissions!=null) {
            for(String permission:permissions) {
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}


