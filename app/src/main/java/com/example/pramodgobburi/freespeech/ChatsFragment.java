package com.example.pramodgobburi.freespeech;


import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.constraint.solver.SolverVariable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.RatingCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatsFragment extends Fragment {

    private RecyclerView mChatsList;
    private View mChatView;
    private TextView mChatsNone;
    private FloatingActionButton mGroupChatBtn;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mMessagesDatabase;
    private DatabaseReference mChatsDatabase;

    private FirebaseAuth mAuth;

    private String mCurrent_uid;

    public ChatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        mChatView = inflater.inflate(R.layout.fragment_chats, container, false);

        mChatsList = (RecyclerView) mChatView.findViewById(R.id.chats_list);
        mChatsNone = (TextView) mChatView.findViewById(R.id.chats_none);
        mGroupChatBtn = (FloatingActionButton)mChatView.findViewById(R.id.chats_new_group);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_uid = mAuth.getCurrentUser().getUid();

        mMessagesDatabase = FirebaseDatabase.getInstance().getReference().child("messages");

        mChatsDatabase = FirebaseDatabase.getInstance().getReference().child("Chat").child(mCurrent_uid);
        mChatsDatabase.keepSynced(true);

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mChatsList.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        mChatsList.setLayoutManager(layoutManager);



        return mChatView;

    }

    @Override
    public void onStart() {
        super.onStart();
        
        mGroupChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Taking you to the new group activity", Toast.LENGTH_SHORT).show();
                Intent newGroupChatIntent = new Intent(getContext(), NewGroupActivity.class);
                startActivity(newGroupChatIntent);

            }
        });

        Query query = mChatsDatabase.orderByChild("timestamp");

        FirebaseRecyclerAdapter<Chats, ChatsViewHolder> chatsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Chats, ChatsViewHolder>(
                Chats.class,
                R.layout.chat_single_layout,
                ChatsViewHolder.class,
                query
        ) {
            @Override
            protected void populateViewHolder(final ChatsViewHolder viewHolder, Chats model, int position) {
                final String friend_id = getRef(position).getKey();
                Log.d("FRIEND_ID", friend_id);

                final int unread_count = 0;
                mChatsDatabase.child(friend_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.hasChild("type")&&(dataSnapshot.child("type").getValue().toString().equals("group"))) {
                            mMessagesDatabase.child(friend_id).addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                    Log.d("CHATS FRAGMENT", "A new message has been added" + dataSnapshot.getValue());
                                    if(dataSnapshot.child("type").getValue().equals("text")) {
                                        String last_message = dataSnapshot.child("message").getValue().toString();
                                        Log.d("LAST_MESSAGE", last_message);

                                        viewHolder.setLastMessage(last_message);
                                    }

                                    if(!mCurrent_uid.equals(dataSnapshot.child("from").getValue())) {

                                        mChatsDatabase.child(friend_id).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                if (dataSnapshot.hasChild("seen")&&(dataSnapshot.child("seen").getValue().equals(false))) {
                                                    Log.d("NOT SEEN", "TRUE");
                                                    viewHolder.setTextToBold();
                                                } else if (dataSnapshot.hasChild("seen")&&(dataSnapshot.child("seen").getValue().equals(true))) {
                                                    viewHolder.setTextToRegular();
                                                }

                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });

                                    }

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

                           mChatsDatabase.child(friend_id).addValueEventListener(new ValueEventListener() {
                               @Override
                               public void onDataChange(DataSnapshot dataSnapshot) {
                                   final String name = dataSnapshot.child("name").getValue().toString();
                                   String user_thumb = dataSnapshot.child("thumb_img").getValue().toString();

                                   mChatsNone.setVisibility(View.INVISIBLE);

                                   viewHolder.setName(name);
                                   viewHolder.setUserImage(user_thumb,getContext());


                                   viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           //Toast.makeText(getContext(), "Should take to group chat activity", Toast.LENGTH_SHORT).show();
                                           Intent groupChatIntent = new Intent(getContext(), GroupChatActivity.class);
                                           groupChatIntent.putExtra("group_chat_id", friend_id);
                                           Log.d("NAME", name);
                                           groupChatIntent.putExtra("group_chat_name", name);
                                           startActivity(groupChatIntent);
                                       }
                                   });
                               }

                               @Override
                               public void onCancelled(DatabaseError databaseError) {

                               }
                           });
                        }
                        else
                        {
                            mMessagesDatabase.child(mCurrent_uid).child(friend_id).addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(DataSnapshot dataSnapshot, String s) {


                                    if(dataSnapshot.child("type").equals("text")) {
                                        String last_message = dataSnapshot.child("message").getValue().toString();

                                        viewHolder.setLastMessage(last_message);


                                    }



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

                            mMessagesDatabase.child(friend_id).child(mCurrent_uid).addChildEventListener(new ChildEventListener() {
                                @Override
                                public void onChildAdded(DataSnapshot dataSnapshot, String s) {


                                    String last_message = dataSnapshot.child("message").getValue().toString();
                                    viewHolder.setLastMessage(last_message);

                                    if(friend_id.equals(dataSnapshot.child("from").getValue())) {


                                        if (dataSnapshot.child("seen").getValue().equals(false)) {
                                            Log.d("NOT SEEN", "TRUE");
                                            viewHolder.setTextToBold();
                                        } else if (dataSnapshot.child("seen").getValue().equals(true)) {
                                            viewHolder.setTextToRegular();
                                        }

                                    }
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

                            mUserDatabase.child(friend_id).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    final String name = dataSnapshot.child("name").getValue().toString();
                                    String user_thumb = dataSnapshot.child("thumb_image").getValue().toString();

                                    mChatsNone.setVisibility(View.INVISIBLE);

                                    viewHolder.setName(name);
                                    viewHolder.setUserImage(user_thumb,getContext());

                                    if(dataSnapshot.hasChild("online")) {
                                        String userOnline = (String) dataSnapshot.child("online").getValue().toString();
                                        viewHolder.setUserOnline(userOnline);

                                    }

                                    viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("user_id", friend_id);
                                            chatIntent.putExtra("user_name", name);
                                            startActivity(chatIntent);
                                        }
                                    });

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
        };

        mChatsList.setAdapter(chatsRecyclerViewAdapter);


    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public ChatsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setLastMessage(String message) {
            TextView lastMessageView = (TextView) mView.findViewById(R.id.chat_last_message);

            lastMessageView.setText(message);
        }

        public void setName(String name) {
            TextView userNameView = (TextView) mView.findViewById(R.id.chat_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String thumb_image, Context context) {
            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.chat_single_image);

            Picasso.with(context).load(thumb_image).placeholder(R.drawable.default_profile_image).into(userImageView);
        }

        public void setUserOnline(String online_status) {
            ImageView userOnlineView = (ImageView) mView.findViewById(R.id.chat_single_online_icon);

            if(online_status.equals("true")){
                userOnlineView.setVisibility(View.VISIBLE);
            }
            else
            {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }

        public void setTextToBold() {
            TextView lastMessageView = (TextView) mView.findViewById(R.id.chat_last_message);
            lastMessageView.setTypeface(lastMessageView.getTypeface(), Typeface.BOLD);

            TextView nameView = (TextView) mView.findViewById(R.id.chat_single_name);
            nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);
        }

        public void setTextToRegular() {
            TextView lastMessageView = (TextView) mView.findViewById(R.id.chat_last_message);
            lastMessageView.setTypeface(lastMessageView.getTypeface(), Typeface.NORMAL);

            TextView nameView = (TextView) mView.findViewById(R.id.chat_single_name);
            nameView.setTypeface(nameView.getTypeface(), Typeface.NORMAL);
        }
    }

}
