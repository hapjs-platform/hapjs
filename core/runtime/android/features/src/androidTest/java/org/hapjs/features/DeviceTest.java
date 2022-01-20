/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.bridge.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DeviceTest extends AbstractTest {
    public DeviceTest() {
        super(new Device());
    }

    @Test
    public void testGetInfo() throws JSONException {
        Response response = invoke(Device.ACTION_GET_INFO, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject result = response.toJSON().getJSONObject("content");
        int platformVersion = result.getInt(Device.RESULT_PLATFORM_VERSION_CODE);
        Assert.assertEquals(org.hapjs.runtime.BuildConfig.platformVersion, platformVersion);

        response = invoke(Device.ACTION_GET_ID, null);
        Assert.assertEquals(Response.CODE_ILLEGAL_ARGUMENT, response.getCode());

        JSONArray typeObj = new JSONArray();
        typeObj.put(Device.TYPE_DEVICE);
        typeObj.put(Device.TYPE_MAC);
        typeObj.put(Device.TYPE_USER);
        JSONObject params = new JSONObject();
        params.put(Device.PARAM_TYPE, typeObj);
        response = invoke(Device.ACTION_GET_ID, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        result = response.toJSON().getJSONObject("content");
        Assert.assertFalse(result.getString(Device.RESULT_DEVICE).isEmpty());
        Assert.assertFalse(result.getString(Device.RESULT_MAC).isEmpty());
        Assert.assertFalse(result.getString(Device.RESULT_USER).isEmpty());
    }
}
