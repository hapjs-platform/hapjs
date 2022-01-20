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
public class CalendarTest extends AbstractTest {

    public CalendarTest() {
        super(new Calendar());
    }

    @Test
    public void testSetGet() throws JSONException {
        JSONObject params = new JSONObject();
        Response response;
        //                params.put(Calendar.PARAM_START_DATE, System.currentTimeMillis());
        //                params.put(Calendar.PARAM_END_DATE, System.currentTimeMillis()+200*60000);
        //                Response response = invoke(Calendar.ACTION_SELECT, params);
        //                Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //                int count = ((JSONArray) response.getContent()).length();
        params = new JSONObject();
        params.put(Calendar.PARAM_TITLE, "test");
        params.put(Calendar.PARAM_START_DATE, System.currentTimeMillis() + 15 * 60000);
        params.put(Calendar.PARAM_END_DATE, System.currentTimeMillis() + 100 * 60000);
        params.put(Calendar.PARAM_RRULE, "FREQ=YEARLY;COUNT=1");
        params.put(Calendar.PARAM_ALL_DAY, false);
        params.put(Calendar.PARAM_REMIND_TIME, new JSONArray().put(3));
        response = invoke(Calendar.ACTION_INSERT, params);
        long insertID = (long) response.getContent();
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //
        //                params = new JSONObject();
        //                params.put(Calendar.PARAM_START_DATE, System.currentTimeMillis());
        //                params.put(Calendar.PARAM_END_DATE, System.currentTimeMillis()+200*60000);
        //                response = invoke(Calendar.ACTION_SELECT, params);
        //                Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //                Assert.assertTrue(((JSONArray) response.getContent()).length() > count);
        //
        //                params = new JSONObject();
        //                params.put(Calendar.PARAM_ID,insertID);
        //                params.put(Calendar.PARAM_UPDATE_VALUES, new
        // JSONObject().put(Calendar.PARAM_DESCRIPTION, "testtest").put(Calendar.PARAM_REMIND_TIME,new
        // JSONArray().put(1).put(5)));
        //                response = invoke(Calendar.ACTION_UPDATE, params);
        //                Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //                Assert.assertTrue(((int) response.getContent()) == 1);
        //
        //                params = new JSONObject();
        //                params.put(Calendar.PARAM_ID,insertID);
        //                response = invoke(Calendar.ACTION_DELETE, params);
        //                Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //                Assert.assertTrue(((int) response.getContent()) == 1);
        //
        //                params = new JSONObject();
        //                params.put(Calendar.PARAM_ID, insertID);
        //                response = invoke(Calendar.ACTION_SELECT, params);
        //                Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        //                Assert.assertEquals(0, ((JSONArray) response.getContent()).length());
    }
}
