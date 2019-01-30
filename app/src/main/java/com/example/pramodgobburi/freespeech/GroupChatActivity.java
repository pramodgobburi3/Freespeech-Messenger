package com.example.pramodgobburi.freespeech;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatActivity extends AppCompatActivity {

    private List<String> Friends;
    private String mChatId;
    private String mChatName;

    private FirebaseAuth mAuth;
    private String mCurrentUser;

    private Toolbar mGroupChatToolbar;
    private DatabaseReference mRootRef;
    private DatabaseReference mMessageReference;
    private DatabaseReference mChatsReference;

    private TextView mTitleView;
    private CircleImageView mChatImage;
    private Button mChatInfoBtn;

    private ImageButton mChatAddBtn;
    private ImageButton mchatSendBtn;
    private EditText mChatMessageView;

    private RecyclerView mMessagesList;
    private SwipeRefreshLayout mRefreshLayout;
    private StorageReference mImageStorage;

    private CircleImageView messageProfile;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private GroupMessageAdapter mAdapter;

    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private int mCurrentPage = 1;

    private static final int CAMERA_REQ = 2;
    Bitmap photo;

    //-------------------- SOLUTION FOR PAGINATION -----------------------
    private int itemPos = 0;
    private String mLastKey = "";
    private String mPrevkey = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser().getUid();
        mChatId = getIntent().getStringExtra("group_chat_id");
        mChatName = getIntent().getStringExtra("group_chat_name");


        mChatsReference = FirebaseDatabase.getInstance().getReference().child("Chat").child(mCurrentUser).child(mChatId);


        mRootRef = FirebaseDatabase.getInstance().getReference();
        mMessageReference = FirebaseDatabase.getInstance().getReference().child("messages").child(mChatId);

        mImageStorage = FirebaseStorage.getInstance().getReference();

        mGroupChatToolbar = (Toolbar) findViewById(R.id.group_chat_app_bar);
        setSupportActionBar(mGroupChatToolbar);

        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowCustomEnabled(true);

        getSupportActionBar().setTitle(mChatName);

        LayoutInflater inflater  = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = inflater.inflate(R.layout.group_custom_bar, null);

        actionbar.setCustomView(action_bar_view);

        mTitleView = (TextView) findViewById(R.id.custom_bar_title);
        mChatImage = (CircleImageView) findViewById(R.id.custom_bar_image);
        mChatInfoBtn = (Button) findViewById(R.id.custom_bar_info);

        mChatAddBtn = (ImageButton) findViewById(R.id.group_chat_add_button);
        mchatSendBtn = (ImageButton) findViewById(R.id.group_chat_send_btn);
        mChatMessageView = (EditText) findViewById(R.id.group_chat_message);

        mAdapter = new GroupMessageAdapter(messagesList);

        mLinearLayout = new LinearLayoutManager(this);
        mMessagesList = (RecyclerView) findViewById(R.id.group_messages_list);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.group_message_swipe_layout);

        mMessagesList.setHasFixedSize(true);
        mMessagesList.setLayoutManager(mLinearLayout);
        mMessagesList.setAdapter(mAdapter);
        loadMessages();

        mTitleView.setText(mChatName);

        //Pull the group chat image from chat database
        mChatsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String image = dataSnapshot.child("thumb_img").getValue().toString();

                mChatImage.setVisibility(View.VISIBLE);
                Picasso.with(GroupChatActivity.this).load(image).placeholder(R.drawable.default_profile_image).into(mChatImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mchatSendBtn.setOnClickListener(new View.OnClickListener() {
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

        mChatInfoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent chatInformation = new Intent(GroupChatActivity.this, GroupDetailsActivity.class);
                chatInformation.putExtra("group_chat_id", mChatId);
                chatInformation.putExtra("group_chat_name", mChatName);
                startActivityForResult(chatInformation, 1);
            }
        });

        mChatAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu = new PopupMenu(GroupChatActivity.this, mChatAddBtn); //the popup menu that shows Camera/Gallery
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
        final DatabaseReference messageRef = mRootRef.child("messages").child(mChatId);

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

    private void loadMessages() {
        final DatabaseReference messageRef = mRootRef.child("messages").child(mChatId);

        Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Messages message = dataSnapshot.getValue(Messages.class);

                itemPos++;

                if(itemPos ==1) {
                    String messageKey = dataSnapshot.getKey();
                    mLastKey = messageKey;
                    mPrevkey = messageKey;
                }

                messagesList.add(message);

                mAdapter.notifyDataSetChanged();

                mChatsReference.child("seen").setValue(true);

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
            String group_chat_ref = "messages/" + mChatId;

            DatabaseReference group_message_push = mRootRef.child("messages").child(mChatId).push();

            String push_id = group_message_push.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUser);

            mChatsReference.child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};


                    Friends = dataSnapshot.getValue(t);
                    Friends.remove(mCurrentUser);
                    for(int i = 0; i < Friends.size(); i++) {
                        mRootRef.child("Chat").child(Friends.get(i)).child(mChatId).child("seen").setValue(false);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });


            mMessageReference.child(push_id).updateChildren(messageMap, new DatabaseReference.CompletionListener() {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_REQ && resultCode == Activity.RESULT_OK) {
            photo = (Bitmap) data.getExtras().get("data");

            final DatabaseReference group_message_push = mRootRef.child("messages").child(mChatId).push();
            final String push_id = group_message_push.getKey();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            photo.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            final byte[] photo_byte = baos.toByteArray();


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


                        mChatMessageView.setText("");

                        mMessageReference.child(push_id).updateChildren(messageMap, new DatabaseReference.CompletionListener() {
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
            });
        }
    }
}
