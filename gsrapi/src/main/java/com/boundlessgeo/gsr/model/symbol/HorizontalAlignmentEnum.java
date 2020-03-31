/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.symbol;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 *
 * @author Juan Marin, OpenGeo
 *
 */
public enum HorizontalAlignmentEnum {

    LEFT("left"), RIGHT("right"), CENTER("center"), JUSTIFY("justify");
    private final String alignment;

    @JsonValue
    public String getAlignment() {
        return alignment;
    }

    HorizontalAlignmentEnum(String align) {
        this.alignment = align;
    }
}
