package com.vanja.hotellobbydisplay.model;

/**
 * One region of a LAYOUT item's split screen (see {@link LayoutModel}).
 *
 * <p>Private fields with public getters; Gson sets them by reflection.</p>
 */
public class RegionModel {

    /** VIDEO or TEXT. */
    private String type;

    /** Media address, for a VIDEO region. */
    private String url;

    /** Text content, for a TEXT region. */
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
