package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * Split-screen description for an item of {@code type == "LAYOUT"}.
 * LAYOUT is a bonus feature (APV-30); if it is not implemented, such items
 * are skipped safely.
 */
public class LayoutModel {

    /** Layout template name, e.g. "VIDEO_LEFT_TEXT_RIGHT". */
    public String template;

    /** The parts of the split screen. */
    public List<RegionModel> regions;
}
