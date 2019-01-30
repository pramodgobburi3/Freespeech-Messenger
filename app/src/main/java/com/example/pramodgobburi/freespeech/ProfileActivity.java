package com.example.pramodgobburi.freespeech;

import android.content.Intent;
import java.text.DateFormat;
import android.media.audiofx.BassBoost;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private TextView mDisplayName;
    private TextView mStatus, mFriendCount;
    private CircleImageView mUserImage;
    private Button mSendRequestBtn, mDeclineRequestBtn;

    private DatabaseReference mDatabaseReference;
    private DatabaseReference mUserRef;

    private DatabaseReference mFriendRequestDatabase;

    private DatabaseReference mFriendDatabase;

    private DatabaseReference mNotificationDatabase;
    private DatabaseReference mRootRef;

    private FirebaseUser mCurrentUser;

    private String mCurrent_state;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        final String user_id = getIntent().getStringExtra("user_id");

        /*if(user_id.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
        }*/

        mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);
        mFriendRequestDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase = FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference().child("notifications");
        mRootRef = FirebaseDatabase.getInstance().getReference();

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();

        mDisplayName = (TextView) findViewById(R.id.profile_displayName);
        mStatus = (TextView) findViewById(R.id.profile_status);
        mUserImage = (CircleImageView) findViewById(R.id.profile_image);
        mSendRequestBtn = (Button) findViewById(R.id.profile_friendRequest);
        mFriendCount = (TextView) findViewById(R.id.profile_friendsCount);
        mDeclineRequestBtn = (Button) findViewById(R.id.profile_declineRequest);

        mCurrent_state = "not_friends";


        mDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String user_displayName = dataSnapshot.child("name").getValue().toString();
                mDisplayName.setText(user_displayName);

                String user_status = dataSnapshot.child("status").getValue().toString();
                mStatus.setText(user_status);

                String image_url = dataSnapshot.child("image").getValue().toString();
                if(!image_url.equals("default"))
                    Picasso.with(ProfileActivity.this).load(image_url).placeholder(R.drawable.default_profile_image).into(mUserImage);

                //------------------ FRIENDS LIST/ REQUEST FEATURE -----------------

                mFriendRequestDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild(user_id)) {
                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received")) {
                                mCurrent_state = "req_received";
                                mSendRequestBtn.setText("Acccept Request");
                                mDeclineRequestBtn.setEnabled(true);
                            }

                            else if(req_type.equals("sent"))
                            {
                                mCurrent_state = "waiting";
                                mSendRequestBtn.setText("Cancel Request");
                            }
                        }
                        else {

                            mFriendDatabase.child(mCurrentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if(dataSnapshot.hasChild(user_id)) {
                                        mSendRequestBtn.setEnabled(true);
                                        mDeclineRequestBtn.setEnabled(false);
                                        mSendRequestBtn.setText("Unfriend User");
                                        mCurrent_state="friends";
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mSendRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

               mSendRequestBtn.setEnabled(false);

                // -------------------- NOT FRIENDS STATE ------------------------

                if(mCurrent_state.equals("not_friends")) {

                    DatabaseReference newNotificationRef = mRootRef.child("notifications").child(user_id).push();
                    String newNotificationId = newNotificationRef.getKey();

                    HashMap<String, String> notificationData = new HashMap<>();
                    notificationData.put("from", mCurrentUser.getUid());
                    notificationData.put("type", "request");

                    Map requestMap = new HashMap();

                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/timestamp", ServerValue.TIMESTAMP);
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/timestamp", ServerValue.TIMESTAMP);
                    requestMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id + "/request_type", "sent");
                    requestMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid() + "/request_type", "received");
                    requestMap.put("notifications/" + user_id + "/" + newNotificationId, notificationData);

                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            Toast.makeText(ProfileActivity.this, "Request sent successfully", Toast.LENGTH_SHORT).show();
                            mSendRequestBtn.setEnabled(true);
                            mSendRequestBtn.setText("Cancel Request");
                            mCurrent_state="waiting";
                            if(databaseError != null) {
                                Toast.makeText(ProfileActivity.this, "An error occurred: "+databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }

                        }
                    });


                }

                if(mCurrent_state.equals("waiting")) {
                    mFriendRequestDatabase.child(mCurrentUser.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mFriendRequestDatabase.child(user_id).child(mCurrentUser.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(ProfileActivity.this, "Request Cancelled", Toast.LENGTH_SHORT).show();
                                    mSendRequestBtn.setEnabled(true);
                                    mSendRequestBtn.setText("Add as friend");
                                    mCurrent_state="not_friends";
                                }
                            });
                        }
                    });
                }

                // ----------REQUEST RECEIVED STATE -----------------------------
                if(mCurrent_state.equals("req_received")) {

                    final String currentDate = DateFormat.getDateInstance().format(new Date());

                    mRootRef.child("Users").child(user_id).child("name").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String friendName = dataSnapshot.getValue().toString();

                            Map friendMap = new HashMap();

                            friendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/name", friendName.toLowerCase());
                            friendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/uid", user_id);

                            mRootRef.updateChildren(friendMap);

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    mRootRef.child("Users").child(mCurrentUser.getUid()).child("name").addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String currentName = dataSnapshot.getValue().toString();

                            Map friendMap = new HashMap();

                            friendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/name", currentName.toLowerCase());
                            friendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/uid", mCurrentUser.getUid());

                            mRootRef.updateChildren(friendMap);

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    Map friendMap = new HashMap();
                    friendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id + "/date", currentDate);
                    friendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid() + "/date", currentDate);



                    friendMap.put("Friend_req/" + mCurrentUser.getUid() + "/" + user_id, null);
                    friendMap.put("Friend_req/" + user_id + "/" + mCurrentUser.getUid(), null);

                    mRootRef.updateChildren(friendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError != null) {
                                Toast.makeText(ProfileActivity.this, "An error occurred: "+databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                mSendRequestBtn.setEnabled(true);
                                mDeclineRequestBtn.setEnabled(false);
                                mSendRequestBtn.setText("Unfriend User");
                                mCurrent_state="friends";
                            }
                        }
                    });
                }

                // ---------------- UNFRIEND CONTACT ------------------
                if(mCurrent_state.equals("friends")) {
                    Map unfriendMap = new HashMap();
                    unfriendMap.put("Friends/" + mCurrentUser.getUid() + "/" + user_id, null);
                    unfriendMap.put("Friends/" + user_id + "/" + mCurrentUser.getUid(), null);

                    unfriendMap.put("Chat/" + user_id + "/" + mCurrentUser.getUid(), null);
                    unfriendMap.put("Chat/" + mCurrentUser.getUid() + "/" + user_id, null);


                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            mRootRef.child("messages").child(mCurrentUser.getUid()).child(user_id).removeValue();
                            mRootRef.child("messages").child(user_id).child(mCurrentUser.getUid()).removeValue();
                            //mRootRef.child("Chat").child(mCurrentUser.getUid()).child(user_id).removeValue();
                            //mRootRef.child("Chat").child(user_id).child(mCurrentUser.getUid()).removeValue();


                            if(databaseError != null) {
                                Toast.makeText(ProfileActivity.this, "An error occurred: "+databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }
                            else
                            {

                                Toast.makeText(ProfileActivity.this, "Processed successfully", Toast.LENGTH_SHORT).show();
                                mSendRequestBtn.setEnabled(true);
                                mSendRequestBtn.setText("Add as friend");
                                mCurrent_state="not_friends";

                            }

                        }
                    });
                }

                //Toast.makeText(ProfileActivity.this, ""+mCurrent_state.toString(), Toast.LENGTH_SHORT).show();

            }
        });



    }
}
