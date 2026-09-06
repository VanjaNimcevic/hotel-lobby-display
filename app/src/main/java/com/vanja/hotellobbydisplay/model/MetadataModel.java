package com.vanja.hotellobbydisplay.model;

/** Optional per-type options for a playlist item. */
public class MetadataModel {

    /** BANNER: "top", "center" or "bottom". */
    private String bannerPosition;
    /** WEB_PAGE: enable JavaScript in the WebView. */
    private boolean javascriptEnabled;
    /** IMAGE: "fitCenter" or "centerCrop". */
    private String scaleType;

    public String getBannerPosition() {
        return bannerPosition;
    }

    public boolean isJavascriptEnabled() {
        return javascriptEnabled;
    }

    public String getScaleType() {
        return scaleType;
    }
}
