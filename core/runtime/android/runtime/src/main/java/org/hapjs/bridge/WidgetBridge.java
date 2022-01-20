/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import org.json.JSONArray;
import org.json.JSONException;

public class WidgetBridge extends ExtensionBridge {
    private static final String TAG = "WidgetBridge";

    public WidgetBridge(ClassLoader loader) {
        super(loader);
    }

    public static String getWidgetMetaDataJSONString() {
        return MetaDataSet.getInstance().getWidgetMetaDataJSONString(false);
    }

    @Override
    protected ExtensionMetaData getExtensionMetaData(String bridge) {
        return MetaDataSet.getInstance().getWidgetMetaData(bridge);
    }

    public JSONArray toJSON() {
        try {
            JSONArray bridgesJSON = new JSONArray();
            for (ExtensionMetaData extensionMetaData :
                    MetaDataSet.getInstance().getWidgetMetaDataMap().values()) {
                bridgesJSON.put(extensionMetaData.toJSON());
            }
            return bridgesJSON;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
