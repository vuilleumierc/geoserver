package com.boundlessgeo.gsr.model.renderer;

import com.boundlessgeo.gsr.model.label.Label;

import java.util.List;

/**
 * Wraper class for renderer
 */
public class DrawingInfo {
    public final Renderer renderer;
    // transparency - not supported
    public List<Label> labelingInfo;

    public DrawingInfo(Renderer renderer) {
        this.renderer = renderer;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public List<Label> getLabelingInfo() {
        return labelingInfo;
    }

    public void setLabelingInfo(List<Label> labelingInfo) {
        this.labelingInfo = labelingInfo;
    }
}
