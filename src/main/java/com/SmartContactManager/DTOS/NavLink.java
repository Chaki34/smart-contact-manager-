package com.SmartContactManager.DTOS;

public class NavLink {
    private String title;
    private String url;

    public NavLink(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
}

