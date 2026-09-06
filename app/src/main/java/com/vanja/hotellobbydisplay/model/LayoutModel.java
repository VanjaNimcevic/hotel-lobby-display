package com.vanja.hotellobbydisplay.model;

import java.util.List;

/** Split-screen description for a LAYOUT item (bonus feature, APV-30). */
public class LayoutModel {

    private String template;
    private List<RegionModel> regions;

    public String getTemplate() {
        return template;
    }

    public List<RegionModel> getRegions() {
        return regions;
    }
}
