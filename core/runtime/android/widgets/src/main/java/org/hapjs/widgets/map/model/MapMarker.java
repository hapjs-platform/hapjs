/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

public class MapMarker extends BaseMapMarker {

    public static final String GEOLOCATION_MARKER_TYPE_CENTER = "geolocation_marker_type_center";
    public static final String GEOLOCATION_MARKER_TYPE_TARGET = "geolocation_marker_type_target";

    public String title;
    public String iconPath;
    public float opacity;
    public float rotate;
    public int width = Integer.MAX_VALUE;
    public int height = Integer.MAX_VALUE;
    public int realWidth = Integer.MAX_VALUE;
    public int realHeight = Integer.MAX_VALUE;
    public MapAnchor anchor;
    public String callout;
    public String label;
    public int zIndex;
    public String geoMarkerType;

    public MapMarker() {
        id = DEFAULT_ID;
        opacity = 1;
        rotate = 0;
        anchor = new MapAnchor();
        anchor.x = 0.5f;
        anchor.y = 1;
    }

    public boolean isPositionInvalid() {
        return (offsetX == Integer.MAX_VALUE) || (offsetY == Integer.MAX_VALUE);
    }

    public boolean isGeoMarker() {
        return GEOLOCATION_MARKER_TYPE_CENTER.equals(geoMarkerType)
                || GEOLOCATION_MARKER_TYPE_TARGET.equals(geoMarkerType);
    }

    public boolean isGeoMarkerInvalid() {
        if (GEOLOCATION_MARKER_TYPE_CENTER.equals(geoMarkerType)) {
            return isPositionInvalid();
        } else if (GEOLOCATION_MARKER_TYPE_TARGET.equals(geoMarkerType)) {
            return isInvalid();
        }
        return true;
    }

    public static class MapAnchor {
        public float x;
        public float y;
    }
}
