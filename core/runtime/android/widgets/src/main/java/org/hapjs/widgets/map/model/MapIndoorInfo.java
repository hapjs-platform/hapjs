/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.map.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapIndoorInfo {
    public boolean enter;
    public IndoorInfo indoorInfo;

    public MapIndoorInfo() {
    }

    public Map<String, Object> converToMap() {
        Map<String, Object> params = new HashMap<>(2);
        params.put("isEnter", enter);
        if (indoorInfo != null) {
            params.put("indoorInfo", indoorInfo.converToMap());
        }
        return params;
    }

    public void setIndoorInfo(IndoorInfo indoorInfo) {
        this.indoorInfo = indoorInfo;
    }

    public boolean isEnter() {
        return enter;
    }

    public void setEnter(boolean enter) {
        this.enter = enter;
    }

    public static class IndoorInfo {
        public String curFloor;
        public String poiId;
        public ArrayList<String> floorNames;

        public IndoorInfo() {
        }

        public Map<String, Object> converToMap() {
            Map<String, Object> indoorInfo = new HashMap<>(3);
            indoorInfo.put("curFloor", curFloor);
            indoorInfo.put("poiId", poiId);
            indoorInfo.put("floorNames", floorNames);
            return indoorInfo;
        }

        public void setCurFloor(String curFloor) {
            this.curFloor = curFloor;
        }

        public void setPoiId(String poiId) {
            this.poiId = poiId;
        }

        public void setFloorNames(ArrayList<String> floorNames) {
            this.floorNames = floorNames;
        }
    }
}
