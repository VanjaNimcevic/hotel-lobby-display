package com.vanja.hotellobbydisplay.model;

/**
 * One region of a LAYOUT item's split screen (see {@link LayoutModel}).
 */
public class RegionModel {

    /** VIDEO or TEXT. */
    public String type;

    /** Media address, for a VIDEO region. */
    public String url;

    /** Text content, for a TEXT region. */
    public String text;
}
