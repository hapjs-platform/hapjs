/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.bridge.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FetchTest extends AbstractTest {
    private static final String PNG_URL = "https://www.baidu.com/img/bd_logo1.png";
    private static final String WEB_URL = "https://www.zhihu.com/question/27042837";

    public FetchTest() {
        super(new Fetch());
    }

    @Test
    public void testFetch() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Fetch.PARAMS_KEY_URL, PNG_URL);
        Response response = invoke(Fetch.ACTION_FETCH, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        int code = content.getInt(Fetch.RESULT_KEY_CODE);
        String data = content.getString(Fetch.RESULT_KEY_DATA);
        Assert.assertEquals(200, code);
        Assert.assertTrue(data.startsWith("internal://"));

        params = new JSONObject();
        params.put(Fetch.PARAMS_KEY_METHOD, "POST");
        params.put(Fetch.PARAMS_KEY_URL, "http://dwz.cn/create.php");
        JSONObject dataParam = new JSONObject();
        dataParam.put("url", WEB_URL);
        params.put(Fetch.PARAMS_KEY_DATA, dataParam);
        JSONObject headerParam = new JSONObject();
        headerParam.put("Accept", "application/json");
        params.put(Fetch.PARAMS_KEY_HEADER, headerParam);
        response = invoke(Fetch.ACTION_FETCH, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        content = response.toJSON().getJSONObject("content");
        code = content.getInt(Fetch.RESULT_KEY_CODE);
        data = content.getString(Fetch.RESULT_KEY_DATA);
        Assert.assertEquals(200, code);
        JSONObject result = new JSONObject(data);
        Assert.assertEquals(data, 0, result.getInt("status"));
    }
}
