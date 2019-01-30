package com.example.pramodgobburi.freespeech;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {

    private RecyclerView mRequestsList;
    private View mRequestView;

    private TextView mRequestsNone;

    private DatabaseReference mUserDatabase;
    private DatabaseReference mRequestsDatabase;

    private String mCurrent_Uid;

    private FirebaseAuth mAuth;

    public RequestsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
         mRequestView = inflater.inflate(R.layout.fragment_requests, container, false);

        mRequestsList = (RecyclerView) mRequestView.findViewById(R.id.requests_list);
        mRequestsNone = (TextView) mRequestView.findViewById(R.id.requests_none);

        mAuth = FirebaseAuth.getInstance();
        mCurrent_Uid = mAuth.getCurrentUser().getUid();

        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mRequestsDatabase = FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mRequestsDatabase.keepSynced(true);

        mRequestsList.setHasFixedSize(true);
        mRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return mRequestView;

    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = mRequestsDatabase.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).orderByChild("request_type").equalTo("received");
        final FirebaseRecyclerAdapter<Requests, RequestsViewHolder> requestsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Requests, RequestsViewHolder>(
                Requests.class,
                R.layout.request_single_layout,
                RequestsViewHolder.class,
                query
        ) {
            @Override
            protected void populateViewHolder(final RequestsViewHolder viewHolder, Requests model, final int position) {
                final String request_id = getRef(position).getKey();
                Log.d("REQUEST_ID", request_id);


                    mUserDatabase.child(request_id).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            String friend_name = dataSnapshot.child("name").getValue().toString();
                            String friend_image = dataSnapshot.child("thumb_image").getValue().toString();

                            mRequestsNone.setVisibility(View.INVISIBLE);
                            viewHolder.setName(friend_name);
                            viewHolder.setUserImage(friend_image, getContext());

                            viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent profileInent = new Intent(getContext(), ProfileActivity.class);
                                    profileInent.putExtra("user_id", request_id);
                                    startActivity(profileInent);
                                }
                            });
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                if(request_id.equals(null)) {
                    mRequestsNone.setVisibility(View.VISIBLE);
                }




                }


            };

             mRequestsList.setAdapter(requestsRecyclerViewAdapter);

        }



    public static class RequestsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public RequestsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;
        }

        public void setName(String name) {
            TextView userNameView = (TextView) mView.findViewById(R.id.request_single_name);
            userNameView.setText(name);
        }

        public void setUserImage(String thumb_image, Context context) {
            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.request_single_image);

            Picasso.with(context).load(thumb_image).placeholder(R.drawable.default_profile_image).into(userImageView);
        }
    }

}
