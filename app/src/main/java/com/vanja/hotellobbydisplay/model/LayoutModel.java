package com.vanja.hotellobbydisplay.model;

import java.util.List;

/**
 * Split-screen description for an item of {@code type == "LAYOUT"}.
 * LAYOUT is a bonus feature (APV-30); if it is not implemented, such items
 * are skipped safely.
 *
 * <p>Private fields with public getters; Gson sets them by reflection.</p>
 */
public class LayoutModel {

    /** Layout template name, e.g. "VIDEO_LEFT_TEXT_RIGHT". */
    private String template;

    /** The parts of the split screen. */
    private List<RegionModel> regions;

    public String getTemplate() {
        return template;
    }

    public List<RegionModel> getRegions() {
        return regions;
    }
}
