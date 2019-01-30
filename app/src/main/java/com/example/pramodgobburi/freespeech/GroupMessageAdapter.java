package com.example.pramodgobburi.freespeech;


import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupMessageAdapter extends RecyclerView.Adapter<GroupMessageAdapter.GroupMessageViewHolder> {

    private List<Messages> mMessagesList;
    private FirebaseAuth mAuth;

    private DatabaseReference mChatDatabase;

    public GroupMessageAdapter(List<Messages> mMessagesList) { this.mMessagesList = mMessagesList; }

    @Override
    public GroupMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);

        return new GroupMessageViewHolder(v);
    }


    public class GroupMessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public CircleImageView profileImage;
        public RelativeLayout dialogLayout;

        public GroupMessageViewHolder(View view){
            super(view);

            messageText = (TextView) view.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) view.findViewById(R.id.message_image_layout);
            dialogLayout = (RelativeLayout) view.findViewById(R.id.messsage_single_layout);
        }
    }

    @Override
    public void onBindViewHolder(final GroupMessageViewHolder holder, int position) {
        mAuth = FirebaseAuth.getInstance();

        final String current_uid = mAuth.getCurrentUser().getUid();

        Messages c = mMessagesList.get(position);

        final String from_user = c.getFrom();


        if(from_user.equals(current_uid)) {
            holder.messageText.setBackgroundResource(R.drawable.message_send_background_currentuser);
            holder.messageText.setTextColor(Color.BLACK);
            holder.dialogLayout.setGravity(Gravity.RIGHT);
            holder.profileImage.setVisibility(View.INVISIBLE);
        }
        else
        {
            holder.messageText.setBackgroundResource(R.drawable.message_text_background);
            holder.messageText.setTextColor(Color.WHITE);
            holder.dialogLayout.setGravity(Gravity.LEFT);

        }

        holder.messageText.setText(c.getMessage());

        DatabaseReference mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(from_user);

        mUserDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!from_user.equals(current_uid)) {
                    holder.profileImage.setVisibility(View.VISIBLE);
                }
                else
                {
                    holder.profileImage.setVisibility(View.INVISIBLE);
                }
                String image = dataSnapshot.child("thumb_image").getValue().toString();
                Picasso.with(holder.profileImage.getContext()).load(image)
                        .placeholder(R.drawable.default_profile_image).into(holder.profileImage);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    @Override
    public int getItemCount() {
        return mMessagesList.size();
    }
}
