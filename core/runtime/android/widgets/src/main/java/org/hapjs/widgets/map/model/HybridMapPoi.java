/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import java.util.HashMap;
import java.util.Map;

public class HybridMapPoi {
    private Poi poi;

    public void setPoi(Poi poi) {
        this.poi = poi;
    }

    public Map<String, Object> converToMap() {
        Map<String, Object> params = new HashMap<>();
        if (poi != null) {
            params.putAll(poi.converToMap());
        }
        return params;
    }

    public static class Poi {
        public String poiId;
        public String poiName;
        public double latitude;
        public double longitude;

        public Map<String, Object> converToMap() {
            Map<String, Object> poiMap = new HashMap<>(4);
            poiMap.put("poiId", poiId);
            poiMap.put("poiName", poiName);
            poiMap.put("latitude", latitude);
            poiMap.put("longitude", longitude);
            return poiMap;
        }
    }
}
