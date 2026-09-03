package com.vanja.hotellobbydisplay.model;

/**
 * Extra per-type options for a playlist item (see docs/playlist-format.md).
 * All fields are optional; missing values stay {@code null} or {@code false}.
 */
public class MetadataModel {

    /** BANNER: "top", "center" or "bottom". */
    public String bannerPosition;

    /** WEB_PAGE: turn JavaScript on in the WebView. Defaults to false. */
    public boolean javascriptEnabled;

    /** IMAGE: "fitCenter" (no crop) or "centerCrop" (fill). */
    public String scaleType;
}
