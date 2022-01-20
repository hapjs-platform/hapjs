/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features.geolocation.vo;

import android.util.Log;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HybridReverseGeoCodeVo {
    private static final String TAG = "HybridReverseGeoCodeVo";
    public String address;
    public String street;
    // town字段该版本没有
    // public String town;
    // 区县
    public String district;
    public String city;
    public String province;
    public String countryName;
    public List<HybridPoiInfoVo> poiInfoList;

    public JSONObject converDataToJSON() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("address", address);
        object.put("street", street);
        // object.put("town", town);
        object.put("district", district);
        object.put("city", city);
        object.put("province", province);
        object.put("countryName", countryName);
        JSONArray jsonArray = new JSONArray();
        if (poiInfoList != null && poiInfoList.size() > 0) {
            for (HybridPoiInfoVo hybridPoiInfoVo : poiInfoList) {
                if (hybridPoiInfoVo == null) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("poiUid", hybridPoiInfoVo.poiUid);
                    jsonObject.put("poiName", hybridPoiInfoVo.poiName);
                    jsonObject.put("latitude", hybridPoiInfoVo.latitude);
                    jsonObject.put("longitude", hybridPoiInfoVo.longitude);
                    jsonObject.put("address", hybridPoiInfoVo.address);
                    jsonObject.put("city", hybridPoiInfoVo.city);
                    jsonObject.put("phone", hybridPoiInfoVo.phone);
                } catch (JSONException e) {
                    Log.e(TAG, "converDataToJSON: ", e);
                    continue;
                }
                jsonArray.put(jsonObject);
            }
        }
        object.put("poiInfoList", jsonArray);
        return object;
    }
}
