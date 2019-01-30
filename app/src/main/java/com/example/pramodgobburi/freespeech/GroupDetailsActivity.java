package com.example.pramodgobburi.freespeech;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
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
import android.widget.EditText;
import android.support.v7.widget.Toolbar;
import android.widget.HorizontalScrollView;
import android.widget.Toast;

import com.example.pramodgobburi.freespeech.HorizontalScrollMenuView;
import com.example.pramodgobburi.freespeech.VerticalScrollView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class GroupDetailsActivity extends AppCompatActivity {

    private String mChatId;
    private String mChatName;

    private HorizontalScrollMenuView currentUsersMenu;
    private VerticalScrollView friendsMenu;

    private String groupImageUri;
    private Toolbar mEditGroupToolbar;
    private EditText mEditGroupName;
    private CircleImageView mEditGroupImage;
    private FloatingActionButton mEditGroupSave;

    private ProgressDialog mProgressDialog;
    private FirebaseAuth mAuth;

    private DatabaseReference mUsersDatabase;
    private DatabaseReference mFriendsDatabase;
    private DatabaseReference mChatsDatabase;

    private String mCurrent_uid;
    private List<String> currentUserIds = new List<String>() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @NonNull
        @Override
        public Iterator<String> iterator() {
            return null;
        }

        @NonNull
        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @NonNull
        @Override
        public <T> T[] toArray(@NonNull T[] ts) {
            return null;
        }

        @Override
        public boolean add(String s) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(@NonNull Collection<?> collection) {
            return false;
        }

        @Override
        public boolean addAll(@NonNull Collection<? extends String> collection) {
            return false;
        }

        @Override
        public boolean addAll(int i, @NonNull Collection<? extends String> collection) {
            return false;
        }

        @Override
        public boolean removeAll(@NonNull Collection<?> collection) {
            return false;
        }

        @Override
        public boolean retainAll(@NonNull Collection<?> collection) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public String get(int i) {
            return null;
        }

        @Override
        public String set(int i, String s) {
            return null;
        }

        @Override
        public void add(int i, String s) {

        }

        @Override
        public String remove(int i) {
            return null;
        }

        @Override
        public int indexOf(Object o) {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            return 0;
        }

        @NonNull
        @Override
        public ListIterator<String> listIterator() {
            return null;
        }

        @NonNull
        @Override
        public ListIterator<String> listIterator(int i) {
            return null;
        }

        @NonNull
        @Override
        public List<String> subList(int i, int i1) {
            return null;
        }
    };
    private ArrayList<String> updateFriendsIds = new ArrayList<>();
    private ArrayList<String> memberIds = new ArrayList<>();
    private ArrayList<String> addedFriendsIds = new ArrayList<>();

    private StorageReference mImageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        mEditGroupName = (EditText) findViewById(R.id.edit_group_name);
        mEditGroupImage = (CircleImageView) findViewById(R.id.group_details_image);
        mEditGroupSave = (FloatingActionButton) findViewById(R.id.group_edit_save);

        currentUsersMenu = (HorizontalScrollMenuView) findViewById(R.id.group_edit_currentUsers);
        friendsMenu = (VerticalScrollView) findViewById(R.id.group_edit_friends_list);

        mChatId = getIntent().getStringExtra("group_chat_id");
        mChatName = getIntent().getStringExtra("group_chat_name");

        mAuth = FirebaseAuth.getInstance();
        mCurrent_uid = mAuth.getCurrentUser().getUid();
        mImageStorage = FirebaseStorage.getInstance().getReference();

        mEditGroupToolbar = (Toolbar) findViewById(R.id.group_edit_appBarLayout);
        setSupportActionBar(mEditGroupToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        getSupportActionBar().setTitle("Edit Group");

        mFriendsDatabase = FirebaseDatabase.getInstance().getReference().child("Friends").child(mCurrent_uid);
        mFriendsDatabase.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);
        mChatsDatabase = FirebaseDatabase.getInstance().getReference().child("Chat");
        
        if(mChatId == null) {
            Toast.makeText(this, "You were removed from the chat", Toast.LENGTH_SHORT).show();
        }


        mEditGroupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SelectImage();
            }
        });

        mEditGroupName.setText(mChatName);
        updateFriendsIds.add(mCurrent_uid);

        mChatsDatabase.child(mCurrent_uid).child(mChatId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                groupImageUri = dataSnapshot.child("thumb_img").getValue().toString();
                Picasso.with(GroupDetailsActivity.this).load(groupImageUri).placeholder(R.drawable.default_profile_image).into(mEditGroupImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        fetchData();


        mEditGroupSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(GroupDetailsActivity.this, "Successfully updated users", Toast.LENGTH_SHORT).show();

                currentUserIds.add(mCurrent_uid);

                mChatsDatabase.child(mCurrent_uid).child(mChatId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String mGroupName = dataSnapshot.child("name").getValue().toString();
                        String thumb_img = dataSnapshot.child("thumb_img").getValue().toString();


                        Map groupChatMap = new HashMap();
                        groupChatMap.put("name", mGroupName);
                        groupChatMap.put("thumb_img", thumb_img);
                        groupChatMap.put("users", updateFriendsIds);
                        groupChatMap.put("type", "group");

                        for (int i = 0; i < updateFriendsIds.size(); i++) {
                            Log.e("GroupDetailsActivity", updateFriendsIds.get(i));
                            mChatsDatabase.child(updateFriendsIds.get(i)).child(mChatId).updateChildren(groupChatMap);

                        }

                        updateFriendsIds.clear();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                Intent groupChatIntent = new Intent(GroupDetailsActivity.this, GroupChatActivity.class);
                groupChatIntent.putExtra("group_chat_id", mChatId);
                groupChatIntent.putExtra("group_chat_name", mChatName);
                startActivity(groupChatIntent);


            }

        });
    }

    private void fetchData() {
        mChatsDatabase.child(mCurrent_uid).child(mChatId).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()) {
                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};

                    currentUserIds = dataSnapshot.getValue(t);
                    currentUserIds.remove(mCurrent_uid);
                }
                else {
                    Intent mainIntent = new Intent(GroupDetailsActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void SelectImage() {

        String[] Permissions = {android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.MEDIA_CONTENT_CONTROL };
        if(!hasPermissions(this, Permissions)) {
            ActivityCompat.requestPermissions(this, Permissions, 1);
        }

        CropImage.activity()
                .setAspectRatio(1,1)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(GroupDetailsActivity.this);
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

                mProgressDialog = new ProgressDialog(GroupDetailsActivity.this);
                mProgressDialog.setTitle("Uploading Image");
                mProgressDialog.setMessage("Please wait...");
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();

                Uri resultUri = result.getUri();

                final File thumb_filePath = new File(resultUri.getPath());

                Bitmap thumb_bitmap = null;
                try {
                    thumb_bitmap = new Compressor(GroupDetailsActivity.this)
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
                                        groupImageUri = thumb_downloadUrl;
                                        updateDisplayImage();
                                        mProgressDialog.dismiss();

                                    }
                                }
                            });


                        }
                        else
                        {
                            mProgressDialog.dismiss();
                            Toast.makeText(GroupDetailsActivity.this, ""+task.getException().getMessage().toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
    
    private void updateDisplayImage() {
        Picasso.with(GroupDetailsActivity.this).load(groupImageUri).into(mEditGroupImage);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.e("GROUP DETAILS ACTIVITY", "APPLICATION STARTED");
        initMenu();
        //friendsMenu.addFriend("S12231234", "Random friend name", "default", false);

    }

    private void goToMain() {
        Intent mainIntent = new Intent(GroupDetailsActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }



    private void initMenu() {


        mChatsDatabase.child(mCurrent_uid).child(mChatId).child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists()) {

                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {
                    };
                    final List<String> Friends = dataSnapshot.getValue(t);

                    if (Friends != null) {
                        Friends.remove(mCurrent_uid);
                    } else {
                        goToMain();
                    }
                    for (int i = 0; i < Friends.size(); i++) {
                        final int finalI = i;
                        if (!(Friends.get(i) == null)) {
                            mUsersDatabase.child(Friends.get(i)).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    String user_name = dataSnapshot.child("name").getValue().toString();
                                    String user_thumb = dataSnapshot.child("thumb_image").getValue().toString();

                                    memberIds.add(Friends.get(finalI));
                                    Log.v("MEMBER IDS", memberIds.toString());
                                    updateFriendsIds.add(Friends.get(finalI));
                                    currentUsersMenu.addItem(Friends.get(finalI), user_name, user_thumb, R.drawable.default_profile_image);


                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });

                        }
                    }

                    Query query = mFriendsDatabase.orderByChild("name");
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for (DataSnapshot child : dataSnapshot.getChildren()) {
                                final String user_id = child.child("uid").getValue().toString();
                                Log.e("GROUP DETAILS ACTIVITY", user_id);
                                Log.e("CURRENT MEMBERS", memberIds.toString());
                                if (!memberIds.contains(user_id)) {
                                    Log.e("FRIEND IDS", "Member exists " + user_id);

                                    mUsersDatabase.child(user_id).addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            String thumb_image = dataSnapshot.child("thumb_image").getValue().toString();
                                            String name = dataSnapshot.child("name").getValue().toString();
                                            Log.e("GROUP DETAILS ACTIVITY", name);
                                            friendsMenu.addFriend(user_id, name, thumb_image, false);
                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                                    friendsMenu.setOnFriendClickListener(new VerticalScrollView.OnFriendClickListener() {
                                        @Override
                                        public void onFriendClick(FriendMenuItem menuItem, int position) {
                                            if (menuItem.getSelected()) {
                                                menuItem.setSelected(false);
                                                updateFriendsIds.remove(menuItem.getId());
                                                Toast.makeText(GroupDetailsActivity.this, "Box is unchecked" + updateFriendsIds.toString(), Toast.LENGTH_SHORT).show();
                                            } else {
                                                menuItem.setSelected(true);
                                                updateFriendsIds.add(menuItem.getId());

                                                Toast.makeText(GroupDetailsActivity.this, "Box is checked" + updateFriendsIds.toString(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    currentUsersMenu.setOnHSMenuClickListener(new HorizontalScrollMenuView.OnHSMenuClickListener() {
                        @Override
                        public void onHSMClick(final MenuItem menuItem, int position) {
                            final int view_id = position;
                            Toast.makeText(GroupDetailsActivity.this, "" + menuItem.getId(), Toast.LENGTH_SHORT).show();

                            CharSequence options[] = new CharSequence[]{"Direct Message", "Remove user from group"};
                            AlertDialog.Builder builder = new AlertDialog.Builder(GroupDetailsActivity.this);
                            builder.setTitle(menuItem.getText());
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (i == 0) {
                                        Intent chatIntent = new Intent(GroupDetailsActivity.this, ChatActivity.class);
                                        chatIntent.putExtra("user_id", menuItem.getId());
                                        chatIntent.putExtra("user_name", menuItem.getText());
                                        startActivity(chatIntent);
                                    }
                                    if (i == 1) {
                                        //Toast.makeText(GroupDetailsActivity.this, "Removed user " + menuItem.getId(), Toast.LENGTH_SHORT).show();

                                        updateFriendsIds.remove(menuItem.getId());
                                        Toast.makeText(GroupDetailsActivity.this, "Removed user from list" +view_id, Toast.LENGTH_SHORT).show();


                                        /*mChatsDatabase.child(mCurrent_uid).child(mChatId).child("users").addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {
                                                };
                                                final List<String> Friends = dataSnapshot.getValue(t);
                                                currentUserIds.add(mCurrent_uid);
                                                for(int j = 0; j < currentUserIds.size(); j++) {
                                                    if(currentUserIds.get(j).equals(menuItem.getId())) {
                                                        mChatsDatabase.child(currentUserIds.get(j)).child(mChatId).removeValue();
                                                    }
                                                    else {
                                                        for (int i = 0; i < Friends.size(); i++) {
                                                            if (menuItem.getId().equals(Friends.get(i))) {
                                                                    mChatsDatabase.child(currentUserIds.get(j)).child(mChatId).child("users").child(String.valueOf(i)).removeValue();
                                                            }
                                                        }
                                                    }

                                                }
                                                currentUserIds.remove(mCurrent_uid);
                                                Intent groupChatIntent = new Intent(GroupDetailsActivity.this, GroupChatActivity.class);
                                                groupChatIntent.putExtra("group_chat_id", mChatId);
                                                groupChatIntent.putExtra("group_chat_name", mChatName);
                                                startActivity(groupChatIntent);



                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });*/



                                    }
                                }
                            });
                            builder.show();

                        }
                    });
                }
                else {
                    goToMain();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.e("GROUP DETAILS ACTIVITY", "APPLICATION PAUSED");
        currentUsersMenu.removeItems();
    }



}
