package com.vanja.hotellobbydisplay.model;

/** One region of a LAYOUT item's split screen. */
public class RegionModel {

    /** VIDEO or TEXT. */
    private String type;
    private String url;
    private String text;

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getText() {
        return text;
    }
}
