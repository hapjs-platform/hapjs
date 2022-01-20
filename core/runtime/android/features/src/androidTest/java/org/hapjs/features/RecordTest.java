/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.bridge.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RecordTest extends AbstractTest {
    private int mOrgMaxDuration;

    public RecordTest() {
        super(new Record());
    }

    @Before
    public void setup() {
        super.setup();
        mOrgMaxDuration = Record.DEF_DURATION_MAX;
        Record.DEF_DURATION_MAX = 5 * 1000;
    }

    @Test
    public void testStartRecord() throws JSONException {
        Response response = invoke(Record.ACTION_START_RECORD, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Record.RESULT_URI));
    }

    @Test
    public void testStopRecord() throws JSONException {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Response response = invoke(Record.ACTION_STOP_RECORD, null);
                        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
                    }
                })
                .start();

        Response response = invoke(Record.ACTION_START_RECORD, null);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Record.RESULT_URI));
    }

    @After
    public void tearDown() {
        Record.DEF_DURATION_MAX = mOrgMaxDuration;
    }
}
