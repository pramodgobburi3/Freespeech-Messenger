package com.example.pramodgobburi.freespeech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String mChatUser;
    private String mUserName;

    private FirebaseAuth mAuth;
    private String mCurrentUser;

    private Toolbar mChatToolbar;
    private DatabaseReference mRootRef;
    private StorageReference mImageStorage;

    private ImageView mCallBtn;
    private TextView mTitleVeiw;
    private TextView mLastSeenView;
    private CircleImageView mProfileImage;

    private ImageButton mChatAddBtn;
    private ImageButton mChatSendBtn;
    private EditText mChatMessageView;

    private DatabaseReference mMessageReference;

    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;

    private CircleImageView messageProfile;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;
    private static final int CAMERA_REQ = 2;
    Bitmap photo;

    static boolean active = false;

    //-------------------- SOLUTION FOR PAGINATION -----------------------
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevkey = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mChatUser = getIntent().getStringExtra("user_id");
        mUserName = getIntent().getStringExtra("user_name");
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        mCurrentUser = mAuth.getCurrentUser().getUid();

        mMessageReference = FirebaseDatabase.getInstance().getReference().child("messages").child(mCurrentUser);
        mImageStorage = FirebaseStorage.getInstance().getReference();


        mChatToolbar = (Toolbar) findViewById(R.id.chat_app_bar);
        setSupportActionBar(mChatToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

       getSupportActionBar().setTitle(mUserName);

        LayoutInflater inflater  = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.chat_custom_bar, null);

        actionBar.setCustomView(action_bar_view);

        active = true;

        // ----------- CUSTOM ACTION BAR ITEMS -------------------

        mCallBtn = (ImageView) action_bar_view.findViewById(R.id.custom_bar_call); 
        mTitleVeiw = (TextView) findViewById(R.id.custom_bar_title);
        mLastSeenView = (TextView) findViewById(R.id.custom_bar_seen);
        mProfileImage = (CircleImageView) findViewById(R.id.custom_bar_image);

        mChatAddBtn = (ImageButton) findViewById(R.id.chat_add_button);
        mChatSendBtn = (ImageButton) findViewById(R.id.chat_send_btn);
        mChatMessageView = (EditText) findViewById(R.id.chat_message);

        mAdapter = new MessageAdapter(messagesList);

        mMessagesList = (RecyclerView)  findViewById(R.id.messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        mLinearLayout = new LinearLayoutManager(this);

        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);

        mMessagesList.setAdapter(mAdapter);
        loadMessages();


        mTitleVeiw.setText(mUserName);



        mRootRef.child("Users").child(mChatUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("image").getValue().toString();

                if(online.equals("true")) {

                    mLastSeenView.setText("Online");

                }
                else
                {
                    long timeStamp = Long.parseLong(online);

                    GetTimeAgo timeAgo = new GetTimeAgo();

                    String time = timeAgo.getTimeAgo(timeStamp);

                    mLastSeenView.setText(time);
                }
                mProfileImage.setVisibility(View.VISIBLE);
                Picasso.with(ChatActivity.this).load(image).placeholder(R.drawable.default_profile_image).into(mProfileImage);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

       mRootRef.child("Chat").child(mCurrentUser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.hasChild(mChatUser)) {

                    Map chatAddMap = new HashMap();
                    chatAddMap.put("seen", false);

                    Map chatUserMap = new HashMap();
                    chatUserMap.put("Chat/"+mCurrentUser + "/" + mChatUser, chatAddMap);
                    chatUserMap.put("Chat/" + mChatUser + "/" + mCurrentUser, chatAddMap);

                    mRootRef.updateChildren(chatUserMap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError != null) {
                                Toast.makeText(ChatActivity.this, ""+databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // -------------------- SEND BUTTON ON CLICK LISTENER ------------------------

        mChatSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                sendMessage();
            }
        });

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;

                itemPos = 0;

                loadMoreMessages();
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(ChatActivity.this, mChatAddBtn); //the popup menu that shows Camera/Gallery
                popupMenu.getMenuInflater().inflate(R.menu.chat_camera, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if(menuItem.getItemId() == R.id.chat_camera_btn) { //popupmenu for Camera
                            selectCamera();

                        }

                        if(menuItem.getItemId() == R.id.Chat_gal_btn) { //popupmenu for Gallery
                            //Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);

                            selectImage();
                            //Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                            //galleryIntent.setType("image/*");
                            //startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), SELECT_GALLERY_PHOTO); //SELECT_GALLERY_PHOTO is value 1
                        }

                        return true;
                    }
                });

                popupMenu.show();
            }
        });
        
        mCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callIntent = new Intent(ChatActivity.this,CallActivity.class);
                callIntent.putExtra("user_id", mChatUser);
                callIntent.putExtra("user_name",mUserName);
                startActivity(callIntent);
            }
        });

    }

    private void selectCamera() {
          String[] Permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MEDIA_CONTENT_CONTROL };
        if(!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, 1);
        }
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQ);
    }


    private void selectImage() {
        String[] Permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MEDIA_CONTENT_CONTROL };
        if(!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, 1);
        }
            Intent galleryIntent = new Intent();
            galleryIntent.setType("image/*");
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

            startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), 1);

    }

    private boolean hasPermissions(Context context, String... permissions) {
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.M && context!=null && permissions!=null) {
            for(String permission:permissions) {
                if(ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void loadMoreMessages() {

        final DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUser).child(mChatUser);

        Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);
                String messageKey = dataSnapshot.getKey();

                if(!mPrevkey.equals(messageKey)) {
                    messagesList.add(itemPos++, message);
                }
                else
                {
                    mPrevkey = mLastKey;
                }


                if(itemPos == 1) {


                    mLastKey = messageKey;
                }


                //Log.d("TOTAL KEYS", "Last Key: " + mLastKey + " | Prev Key: " + mPrevkey +  " | Message Key:" + messageKey);


                mAdapter.notifyDataSetChanged();

                mMessagesList.scrollToPosition(messagesList.size()-1);

                mRefreshLayout.setRefreshing(false);

                mLinearLayout.scrollToPositionWithOffset(TOTAL_ITEMS_TO_LOAD, 0);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            final String current_user_ref = "messages/" + mCurrentUser + "/" + mChatUser;
            final String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUser;

            DatabaseReference user_message_push = mRootRef.child("messages").child(mCurrentUser).child(mChatUser).push();

            final String push_id = user_message_push.getKey();

            StorageReference filePath = mImageStorage.child("message_images").child(push_id + ".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        String download_url = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", download_url);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUser);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                        mChatMessageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null) {
                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }
            });
        }

        if(requestCode == CAMERA_REQ && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");

            final String current_user_ref = "messages/" + mCurrentUser + "/" + mChatUser;
            final String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUser;


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            final byte[] photo_byte = baos.toByteArray();

            DatabaseReference user_message_push = mRootRef.child("messages").child(mCurrentUser).child(mChatUser).push();
            final String push_id = user_message_push.getKey();
            StorageReference filePath = mImageStorage.child("message_images").child(push_id + ".jpg");
            filePath.putBytes(photo_byte).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful()) {
                        String download_url = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", download_url);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", mCurrentUser);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
                        messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

                        mChatMessageView.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null) {
                                    Log.d("CHAT_LOG", databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private void loadMessages() {

        final DatabaseReference messageRef = mRootRef.child("messages").child(mCurrentUser).child(mChatUser);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if(itemPos == 1) {
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevkey = messageKey;
                }

                messagesList.add(message);

                /*DatabaseReference isFriendsCheck = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrentUser).child(mChatUser);
                isFriendsCheck.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            DatabaseReference mChatDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(mAuth.getCurrentUser().getUid()).child(mChatUser.toString()).child("timestamp");
                            Log.d("TIMESTAMP", mChatDatabase.getRef().toString());
                            if (!mChatDatabase.getKey().equals(null)) {
                                mChatDatabase.setValue(dataSnapshot.child("time").getValue().toString());
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });*/

                DatabaseReference mChatDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(mAuth.getCurrentUser().getUid()).child(mChatUser.toString()).child("timestamp");
                DatabaseReference mChatDatabase2 = FirebaseDatabase.getInstance().getReference().child("Chat").child(mChatUser.toString()).child(mAuth.getCurrentUser().getUid()).child("timestamp");
                Log.d("TIMESTAMP", mChatDatabase.getRef().toString());
                if (!mChatDatabase.getKey().equals(null)) {
                    mChatDatabase.setValue(dataSnapshot.child("time").getValue().toString());
                    mChatDatabase2.setValue(dataSnapshot.child("time").getValue().toString());
                }

                mAdapter.notifyDataSetChanged();

                if(active) {
                    messageRef.child(dataSnapshot.getKey()).child("seen").setValue(true);
                }
                else
                {
                    messageRef.child(dataSnapshot.getKey()).child("seen").setValue(false);
                }


                mMessagesList.scrollToPosition(messagesList.size()-1);

                mRefreshLayout.setRefreshing(false);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {

        final String message = mChatMessageView.getText().toString();

        if(!TextUtils.isEmpty(message)) {

            String current_user_ref = "messages/"+mCurrentUser+"/"+mChatUser;
            String chat_user_ref = "messages/" + mChatUser + "/" + mCurrentUser;

            DatabaseReference user_message_push = mRootRef.child("messages").child(mCurrentUser)
                    .child(mChatUser).push();

            String push_id = user_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUser);

            Map messageUserMap = new HashMap();
            messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
            messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

            mRootRef.child("Chat").child(mCurrentUser).child(mChatUser).child("timestamp").setValue(ServerValue.TIMESTAMP);
            mRootRef.child("Chat").child(mChatUser).child(mCurrentUser).child("timestamp").setValue(ServerValue.TIMESTAMP);


            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                    if(databaseError != null) {
                        Log.d("CHAT_LOG ", databaseError.getMessage().toString());
                    }

                    mChatMessageView.getText().clear();

                }
            });

        }

    }
}
