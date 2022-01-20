/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.vo;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class HybridGeoCodeVo {
    public double latitude;
    public double longitude;

    public Map<String, Object> converDataToMap() {
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("latitude", latitude);
        resultMap.put("longitude", longitude);
        return resultMap;
    }

    public JSONObject converDataToJSON() throws JSONException {
        JSONObject js = new JSONObject();
        js.put("latitude", latitude);
        js.put("longitude", longitude);
        return js;
    }
}
