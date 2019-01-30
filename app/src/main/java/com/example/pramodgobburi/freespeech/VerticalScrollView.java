package com.example.pramodgobburi.freespeech;


import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import java.util.ArrayList;

import io.reactivex.annotations.Nullable;

public class VerticalScrollView extends LinearLayout {

    private AppCompatActivity context;
    private RecyclerView recyclerView;
    private VerticalItemAdapter itemAdapter;
    private ArrayList<FriendMenuItem> menuItems = new ArrayList<>();

    public VerticalScrollView(Context context) {
        super(context);
        this.context = (AppCompatActivity) context;
        init();
    }

    public void notifyDataChanged() {
        itemAdapter.notifyDataSetChanged();
    }


    public VerticalScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = (AppCompatActivity) context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.vscroll_menu, this, true);

        init();
    }

    private void init() {
        recyclerView = (RecyclerView) findViewById(R.id.verticalRecyclerItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        itemAdapter = new VerticalItemAdapter(context, menuItems);
        Log.e("VERTICAL SCROLL VIEW", menuItems.toString());
        itemAdapter.setOnCheckboxClickListener(new VerticalItemAdapter.OnCheckboxClickListener() {
            @Override
            public void onCheckBoxClick(FriendMenuItem menuItem, int position) {
                onFriendClickListener.onFriendClick(menuItem, position);
            }
        });

        recyclerView.setAdapter(itemAdapter);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void addFriend(String id, String name, String image_url, boolean isChecked) {
        menuItems.add(new FriendMenuItem(id, name, image_url, isChecked));
    }


    public void removeItems() {
        menuItems.clear();
    }

    public int numItems() { return menuItems.size();}

    public FriendMenuItem getFriend(int position) { return menuItems.get(position);}

    public interface OnFriendClickListener {
        void onFriendClick(FriendMenuItem menuItem, int position);
    }

    private OnFriendClickListener onFriendClickListener;

    public void setOnFriendClickListener(OnFriendClickListener onFriendClickListener) {
        this.onFriendClickListener = onFriendClickListener;
    }


}
