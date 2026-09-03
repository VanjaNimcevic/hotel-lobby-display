package com.vanja.hotellobbydisplay.model;

/**
 * Extra per-type options for a playlist item (see docs/playlist-format.md).
 * All fields are optional; missing values stay {@code null} or {@code false}.
 *
 * <p>Private fields with public getters; Gson sets them by reflection.</p>
 */
public class MetadataModel {

    /** BANNER: "top", "center" or "bottom". */
    private String bannerPosition;

    /** WEB_PAGE: turn JavaScript on in the WebView. Defaults to false. */
    private boolean javascriptEnabled;

    /** IMAGE: "fitCenter" (no crop) or "centerCrop" (fill). */
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
