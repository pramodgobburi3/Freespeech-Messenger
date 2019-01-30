package com.example.pramodgobburi.freespeech;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class VerticalItemAdapter extends RecyclerView.Adapter<VerticalItemAdapter.ItemViewHolder> {

    private Context context;
    private ArrayList<FriendMenuItem> menuItems;

    public VerticalItemAdapter(Context context, ArrayList<FriendMenuItem> menuItems) {
        this.context = context;
        this.menuItems = menuItems;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.group_friend_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, final int position) {
        final FriendMenuItem menuItem = menuItems.get(position);

        holder.setFriendName(menuItem.getName());
        Log.e("VERTICAL ITEM ADAPTER", menuItem.getName());
        holder.setFriendImage(menuItem.getImage(), context);
        holder.setFriendCheck(menuItem.getSelected());

        holder.friendCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCheckboxClickListener.onCheckBoxClick(menuItem, position);
            }
        });
    }

    @Override
    public int getItemCount() {return menuItems.size();}

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView friendName;
        CircleImageView friendImage;
        CheckBox friendCheck;

        public ItemViewHolder(View itemView) {
            super(itemView);

            friendName = (TextView) itemView.findViewById(R.id.group_friend_name);
            friendImage = (CircleImageView) itemView.findViewById(R.id.group_friend_image);
            friendCheck = (CheckBox) itemView.findViewById(R.id.group_friend_checkbox);
        }

        public void setFriendImage(String url, Context context) {
            Picasso.with(context).load(url).placeholder(R.drawable.default_profile_image).into(friendImage);
        }

        public void setFriendName(String name) {
            friendName.setText(name);
        }

        public void setFriendCheck(boolean check) {
            friendCheck.setChecked(check);
        }
    }

    public interface OnCheckboxClickListener {
        void onCheckBoxClick(FriendMenuItem menuItem, int position);
    }

    private OnCheckboxClickListener onCheckboxClickListener;

    public void setOnCheckboxClickListener(OnCheckboxClickListener onCheckboxClickListener) {
        this.onCheckboxClickListener = onCheckboxClickListener;
    }

}
