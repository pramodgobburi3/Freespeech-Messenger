package com.example.pramodgobburi.freespeech;

public class FriendMenuItem {
    private String id;
    private String name;
    private String image;
    private boolean selected;

    public FriendMenuItem(String id, String name, String image, boolean selected) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.selected = selected;
    }

    public FriendMenuItem(String id, String name, String image) {
        this.id = id;
        this.name = name;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}

