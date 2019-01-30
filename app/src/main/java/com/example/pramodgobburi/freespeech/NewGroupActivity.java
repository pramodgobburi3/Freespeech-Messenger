package com.example.pramodgobburi.freespeech;

import android.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class NewGroupActivity extends AppCompatActivity {

    private String imageUrl = "default";
    private Toolbar mAddGroupToolbar;
    private EditText mAddGroupName;
    private Button mAddGroupImage;
    private RecyclerView mAddGroupFriendsList;
    private FloatingActionButton mAddGroupNextBtn;

    private ProgressDialog mProgressDialog;
    
    private FirebaseAuth mAuth;
    
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mUsersDatabase;
    private DatabaseReference mChatsDatabase;
    
    private String mCurrent_uid;
    private ArrayList<String> friendIds = new ArrayList<>();

    private StorageReference mImageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);

        mAddGroupFriendsList = (RecyclerView) findViewById(R.id.group_friends_list);
        mAddGroupNextBtn = (FloatingActionButton) findViewById(R.id.group_add_next);
        mAddGroupName = (EditText) findViewById(R.id.add_group_name);
        mAddGroupImage = (Button) findViewById(R.id.add_group_image);
        mAuth = FirebaseAuth.getInstance();
        
        mCurrent_uid = mAuth.getCurrentUser().getUid();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mAddGroupToolbar = (Toolbar) findViewById(R.id.group_add_appBarLayout);
        setSupportActionBar(mAddGroupToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        getSupportActionBar().setTitle("Add Group");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);
        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_uid);
        mFriendsDatabase.keepSynced(true);
        mChatsDatabase = FirebaseDatabase.getInstance().getReference().child("Chat");

        mAddGroupFriendsList.setHasFixedSize(true);
        mAddGroupFriendsList.setLayoutManager(new LinearLayoutManager(NewGroupActivity.this));

        mAddGroupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectImage();
            }
        });

        friendIds.add(mCurrent_uid);


    }

    private void SelectImage() {

        String[] Permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MEDIA_CONTENT_CONTROL };
        if(!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, 1);
        }

        CropImage.activity()
                .setAspectRatio(1,1)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(NewGroupActivity.this);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mProgressDialog = new ProgressDialog(NewGroupActivity.this);
                mProgressDialog.setTitle("Uploading Image");
                mProgressDialog.setMessage("Please wait...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();

                Uri resultUri = result.getUri();

                final File thumb_filePath = new File(resultUri.getPath());

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(NewGroupActivity.this)
                            .setMaxHeight(200)
                            .setMaxWidth(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                final byte[] thumb_byte = baos.toByteArray();




                StorageReference filepath = mImageStorage.child("profile_images").child(mCurrent_uid+".jpg");
                final StorageReference thumb_filepath = mImageStorage.child("profile_images").child("thumbs").child(mCurrent_uid+".jpg");
                filepath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                        if(task.isSuccessful()) {

                            final String download_url = task.getResult().getDownloadUrl().toString();

                            UploadTask uploadTask = thumb_filepath.putBytes(thumb_byte);
                            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {
                                    String thumb_downloadUrl = thumb_task.getResult().getDownloadUrl().toString();
                                    if(thumb_task.isSuccessful()) {

                                        imageUrl = thumb_downloadUrl;
                                        mProgressDialog.dismiss();

                                    }
                                }
                            });


                        }
                        else
                        {
                            mProgressDialog.dismiss();
                            Toast.makeText(NewGroupActivity.this, ""+task.getException().getMessage().toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }


    @Override
    public void onStart() {
        super.onStart();

        Query query = mFriendsDatabase.orderByChild("name");


        FirebaseRecyclerAdapter<Friends, AddGroupFriendsViewHolder> groupFriendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, AddGroupFriendsViewHolder>(
                Friends.class,
                R.layout.group_friend_layout,
                AddGroupFriendsViewHolder.class,
                query
        ) {
            @Override
            protected void populateViewHolder(final AddGroupFriendsViewHolder viewHolder, Friends model, int position) {

                final String list_user_id = getRef(position).getKey();

                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumb_image").getValue().toString();
                        

                        viewHolder.setName(userName);
                        viewHolder.setFriendImage(userThumb, NewGroupActivity.this);

                        viewHolder.mFriendCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                if(b == true) {
                                    viewHolder.mFriendCheckbox.setChecked(true);
                                    friendIds.add(list_user_id);
                                    Log.e("FRIEND_LIST", friendIds.toString());
                                }
                                else if(b == false) {
                                    viewHolder.mFriendCheckbox.setChecked(false);
                                    friendIds.remove(list_user_id);
                                    Log.e("FRIEND_LIST", friendIds.toString());
                                }
                            }
                        });

                        /*viewHolder.mFriendCheckbox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Toast.makeText(NewGroupActivity.this, "Checkbox clicked", Toast.LENGTH_SHORT).show();
                                if(!viewHolder.mFriendCheckbox.isChecked()) {
                                    Toast.makeText(NewGroupActivity.this, "The checkbox is not checked", Toast.LENGTH_SHORT).show();
                                    viewHolder.mFriendCheckbox.setChecked(true);
                                    friendIds.add(list_user_id);
                                    Log.e("FRIEND_LIST", friendIds.toString());
                                }
                                else
                                {
                                    Toast.makeText(NewGroupActivity.this, "The checkbox is checked", Toast.LENGTH_SHORT).show();
                                    viewHolder.mFriendCheckbox.setChecked(true);
                                    friendIds.remove(list_user_id);
                                }
                            }
                        });*/
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        mAddGroupFriendsList.setAdapter(groupFriendsRecyclerViewAdapter);

            mAddGroupNextBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    String chat_push_id = "GC" + mChatsDatabase.child(mCurrent_uid).push().getKey();

                    Map groupChatMap = new HashMap();
                    groupChatMap.put("name", mAddGroupName.getText().toString());
                    groupChatMap.put("thumb_img", imageUrl);
                    groupChatMap.put("users", friendIds);
                    groupChatMap.put("type", "group");

                    mChatsDatabase.child(mCurrent_uid).child(chat_push_id).updateChildren(groupChatMap);

                    for(int i= 0; i < friendIds.size(); i++) {

                        Log.d("FRIENDIDS", friendIds.get(i));
                        mChatsDatabase.child(friendIds.get(i)).child(chat_push_id).updateChildren(groupChatMap);

                    }

                    Log.d("NEW GROUP ACTIVITY", friendIds.toString());
                    Intent groupChatIntent = new Intent(NewGroupActivity.this, GroupChatActivity.class);
                    groupChatIntent.putExtra("group_chat_id", chat_push_id);
                    groupChatIntent.putExtra("group_chat_name", mAddGroupName.getText().toString());
                    startActivity(groupChatIntent);
                    finish();
                }
            });


        
    }

    public static class AddGroupFriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;
        CheckBox mFriendCheckbox;
        
        public AddGroupFriendsViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            mFriendCheckbox = (CheckBox) mView.findViewById(R.id.group_friend_checkbox);
        }
        
        public void setName(String name) {
            TextView friendNameView = (TextView) mView.findViewById(R.id.group_friend_name);
            friendNameView.setText(name);
        }
        
        public void setFriendImage(String thumb_image, Context context) {
            CircleImageView friendImageView = (CircleImageView) mView.findViewById(R.id.group_friend_image);

            Picasso.with(context).load(thumb_image).placeholder(R.drawable.default_profile_image).into(friendImageView);
        }
        
    }
    
}


