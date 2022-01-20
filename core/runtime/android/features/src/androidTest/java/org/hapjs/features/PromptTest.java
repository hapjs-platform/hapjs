/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.features;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.hapjs.annotation.ManualTest;
import org.hapjs.bridge.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class PromptTest extends AbstractTest {
    public PromptTest() {
        super(new Prompt());
    }

    @Test
    public void testShowToast() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Prompt.PARAM_KEY_MESSAGE, "message");
        Response response = invoke(Prompt.ACTION_SHOW_TOAST, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
    }

    @Test
    @ManualTest
    public void testShowDialog() throws JSONException {
        JSONObject params = new JSONObject();
        params.put(Prompt.PARAM_KEY_TITLE, "title");
        params.put(Prompt.PARAM_KEY_MESSAGE, "message");
        JSONArray buttons = new JSONArray();
        JSONObject positiveButton = new JSONObject();
        positiveButton.put(Prompt.PARAM_KEY_TEXT, "positive");
        buttons.put(positiveButton);
        JSONObject negativeButton = new JSONObject();
        negativeButton.put(Prompt.PARAM_KEY_TEXT, "negative");
        buttons.put(negativeButton);
        JSONObject neutralButton = new JSONObject();
        neutralButton.put(Prompt.PARAM_KEY_TEXT, "neutral");
        buttons.put(neutralButton);
        params.put(Prompt.PARAM_KEY_BUTTONS, buttons);
        Response response = invoke(Prompt.ACTION_SHOW_DIALOG, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Prompt.PARAM_KEY_INDEX));
    }

    @Test
    @ManualTest
    public void testShowContextMenu() throws JSONException {
        JSONObject params = new JSONObject();
        JSONArray itemList = new JSONArray();
        for (int i = 0; i < 5; ++i) {
            itemList.put("item " + i);
        }
        params.put(Prompt.PARAM_KEY_ITEM_LIST, itemList);
        params.put(Prompt.PARAM_KEY_ITEM_COLOR, "#666666");
        Response response = invoke(Prompt.ACTION_SHOW_CONTEXT_MENU, params);
        Assert.assertEquals(Response.CODE_SUCCESS, response.getCode());
        JSONObject content = response.toJSON().getJSONObject("content");
        Assert.assertTrue(content.has(Prompt.PARAM_KEY_INDEX));
    }
}
