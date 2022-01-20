/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.model;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ComponentInfo {
    private static final String KEY_NAME = "name";
    private static final String KEY_CLASS = "class";
    private String mName;
    private String mClassName;

    public static ComponentInfo parse(JSONObject jsonObject) {
        ComponentInfo componentInfo = new ComponentInfo();
        componentInfo.mName = jsonObject.optString(KEY_NAME);
        componentInfo.mClassName = jsonObject.optString(KEY_CLASS);
        return componentInfo;
    }

    public static List<ComponentInfo> parse(JSONArray componentObjects) {
        List<ComponentInfo> componentInfos = new ArrayList<ComponentInfo>();
        if (componentObjects != null) {
            for (int i = 0; i < componentObjects.length(); i++) {
                componentInfos.add(parse(componentObjects.optJSONObject(i)));
            }
        }
        return componentInfos;
    }

    public String getName() {
        return mName;
    }

    public String getClassName() {
        return mClassName;
    }
}
