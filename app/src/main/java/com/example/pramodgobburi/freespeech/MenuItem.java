package com.example.pramodgobburi.freespeech;

/**
 * Created by Pramod Gobburi on 3/26/2018.
 */

public class MenuItem {
    private String id;
    private String icon;
    private String text;
    private int default_icon;
    private boolean selected;
    private int numNotifications;
    private boolean notifications;


    public MenuItem(String id, String icon, int default_icon, String text) {
        this.icon = icon;
        this.text = text;
        this.id = id;
    }


    public MenuItem(String id, String icon, int default_icon, String text, boolean selected) {
        this.id = id;
        this.icon = icon;
        this.default_icon = default_icon;
        this.text = text;
        this.selected = selected;
    }



    public MenuItem(String id, String icon, int default_icon, String text, int numNotifications, boolean notifications) {
        this.id = id;
        this.icon = icon;
        this.default_icon = default_icon;
        this.text = text;
        this.numNotifications = numNotifications;
        this.notifications = notifications;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIcon() {
        return icon;
    }

    public void setDefaultIcon(int defaultIcon) {
        this.default_icon = defaultIcon;
    }

    public int getDefualtIcon() {
        return default_icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getNumNotifications() {
        return numNotifications;
    }

    public void setNumNotifications(int numNotifications) {
        this.numNotifications = numNotifications;
    }

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }
}
