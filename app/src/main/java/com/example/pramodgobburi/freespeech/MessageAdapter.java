package com.example.pramodgobburi.freespeech;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Pramod Gobburi on 10/21/2017.
 */

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> mMessageList;
    private FirebaseAuth mAuth;


    private DatabaseReference mUserDatabase;

    public MessageAdapter(List<Messages> mMessageList) {
        this.mMessageList = mMessageList;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_single_layout, parent, false);

        return new MessageViewHolder(v);

    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public CircleImageView profileImage;
        public RelativeLayout dialogLayout;
        public ImageView messageImage;

        public MessageViewHolder(View view) {

            super(view);

            messageText = (TextView) view.findViewById(R.id.message_text_layout);
            profileImage = (CircleImageView) view.findViewById(R.id.message_image_layout);
            dialogLayout = (RelativeLayout) view.findViewById(R.id.messsage_single_layout);
            messageImage = (ImageView) view.findViewById(R.id.message_image);
        }
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        mAuth = FirebaseAuth.getInstance();

        final String current_uid = mAuth.getCurrentUser().getUid();

        Messages c = mMessageList.get(position);

        final String from_user = c.getFrom();
        String message_type = c.getType();


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

        //holder.messageText.setText(c.getMessage());

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

        if(message_type.equals("text")) {
            holder.messageText.setText(c.getMessage());
            holder.messageImage.setVisibility(View.INVISIBLE);
        }
        else if(message_type.equals("image")) {
            holder.messageText.setVisibility(View.INVISIBLE);

            Picasso.with(holder.profileImage.getContext()).load(c.getMessage()).resize(holder.messageImage.getMaxWidth(), holder.messageImage.getMaxHeight()).placeholder(R.drawable.default_profile_image).resize(holder.messageImage.getMaxWidth(), holder.messageImage.getMaxHeight()).into(holder.messageImage);
        }



    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }



}
