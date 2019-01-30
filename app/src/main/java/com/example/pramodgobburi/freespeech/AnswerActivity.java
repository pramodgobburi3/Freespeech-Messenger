package com.example.pramodgobburi.freespeech;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.calling.Call;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Timer;
import java.util.TimerTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class AnswerActivity extends AppCompatActivity {

    private Call call;

    private ImageView btnAccept;
    private ImageView btnDecline;
    private CircleImageView callerPic;
    private TextView callerUsername;

    private String mCallerId;
    private String mCallerUsername;
    private DatabaseReference mUsersDatabase;

    private FirebaseUser mCurrentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);

        call = null;

        btnAccept = (ImageView) findViewById(R.id.accept);
        btnDecline = (ImageView) findViewById(R.id.decline);
        callerPic = (CircleImageView) findViewById(R.id.callerPic);
        callerUsername = (TextView) findViewById(R.id.callerUsername);

        mCallerId = getIntent().getStringExtra("caller_id");

        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        initUI();

        callTimeout();


        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ResultReceiver acceptedReceiver = getIntent().getParcelableExtra(CallService.KEY_RECEIVER);
                Bundle resultData = new Bundle();
                resultData.putString(CallService.KEY_MESSAGE, "Hello world!");
                acceptedReceiver.send(CallService.RESULT_OK, resultData);
                finish();
            }
        });



        btnDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                endReceivedCall();
                finish();
            }
        });
    }

    private void initUI () {
        mUsersDatabase.child(mCallerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imageUrl = dataSnapshot.child("image").getValue().toString();
                mCallerUsername = dataSnapshot.child("name").getValue().toString();
                Picasso.with(AnswerActivity.this).load(imageUrl).placeholder(R.drawable.default_profile_image).into(callerPic);
                callerUsername.setText(mCallerUsername);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void callTimeout() {

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                endReceivedCall();
            }
        },45000);

    }

    private void endReceivedCall()
    {
        ResultReceiver endReceiver = getIntent().getParcelableExtra(CallService.KEY_RECEIVER);
        Bundle resultData = new Bundle();
        resultData.putString(CallService.KEY_MESSAGE, "Hello world!");
        endReceiver.send(CallService.RESULT_BAD, resultData);

    }

    private void goToMainActivity()
    {
        Intent mainActivity = new Intent(AnswerActivity.this,MainActivity.class);
        startActivity(mainActivity);

    }


}


