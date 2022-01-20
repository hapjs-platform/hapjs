/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.runtime;

import android.util.Log;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class LocaleResourcesParserTest {
    private static final String TAG = "LocaleTest";

    private static final String ZH_JSON =
            "{\"message\":{\"text\": \"纯文本内容\",\"empty\": \"\",\"object\": {\"text\": \"二级对象文本\"},\"array\": [\"元素-类型-字符串\",{\"key\": {\"text\": [\"元素-类型-数组\",\"默认\"]}},[\"元素-类型-数组\",\"默认\"]]}}";

    private static final String TEST_PURE = "test";
    private static final String TEST_SIMPLE = "${message.text}";
    private static final String TEST_EMPTY = "${message.empty}";
    private static final String TEST_MULTI_OBJECT = "${message.object.text}";
    private static final String TEST_ARRAY_1 = "${message.array[0]}";
    private static final String TEST_ARRAY_2 = "${message.array[1].key.text[1]}";
    private static final String TEST_ARRAY_3 = "${message.array[2][0]}";
    private static final String TEST_COMBO_1 = "测试${message.text}";
    private static final String TEST_COMBO_2 = "测试${message.text}${message.array[1].key.text[1]}测试";

    private static Map<String, JSONObject> sResources = new LinkedHashMap<>();

    static {
        try {
            sResources.put("zh", new JSONObject(ZH_JSON));
        } catch (JSONException e) {
            Log.e(TAG, "static initializer: ", e);
        }
    }

    @Test
    public void localeTest() {
        Log.d(TAG, "localeTest: " + sResources);
        testParser(TEST_PURE, TEST_PURE);
        testParser("纯文本内容", TEST_SIMPLE);
        testParser("", TEST_EMPTY);
        testParser("二级对象文本", TEST_MULTI_OBJECT);
        testParser("元素-类型-字符串", TEST_ARRAY_1);
        testParser("默认", TEST_ARRAY_2);
        testParser("元素-类型-数组", TEST_ARRAY_3);
        testParser("测试纯文本内容", TEST_COMBO_1);
        testParser("测试纯文本内容默认测试", TEST_COMBO_2);
    }

    private void testParser(String expected, String content) {
        String result = LocaleResourcesParser.getInstance().getText(content, sResources);
        Assert.assertEquals(expected, result);
    }
}
