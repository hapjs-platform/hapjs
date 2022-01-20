/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.component;

import java.util.Map;
import org.hapjs.component.utils.map.CombinedMap;
import org.hapjs.component.utils.map.SharedMap;
import org.junit.Assert;
import org.junit.Test;

public class CombinedMapTest {
    @Test
    public void test1() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.put("key2", "value2");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.put("key2", "value2");
        combinedMap2.setSharedMap(sharedMap);

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 2);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 0);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 2);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 0);

        Assert.assertEquals(combinedMap1.get("key2"), "value2");
    }

    @Test
    public void test2() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.put("key2", "value2");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.put("key2", "value2-2");
        combinedMap2.setSharedMap(sharedMap);

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 1);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 1);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 1);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 1);

        Assert.assertEquals(combinedMap1.get("key2"), "value2");
        Assert.assertEquals(combinedMap2.get("key2"), "value2-2");
    }

    @Test
    public void test3() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.put("key2", "value2");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.setSharedMap(sharedMap);

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 1);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 1);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 1);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 1);

        Assert.assertEquals(combinedMap1.get("key2"), "value2");
        Assert.assertEquals(combinedMap2.get("key2"), "DEFAULT");
        Assert.assertEquals(combinedMap2.getDiffMap().get("key2"), "DEFAULT");
    }

    @Test
    public void test32() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.put("key2", "value2");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.put("key2", "value2");
        combinedMap2.setSharedMap(sharedMap);

        combinedMap2.remove("key2");

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 1);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 1);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 1);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 1);

        Assert.assertEquals(combinedMap1.get("key2"), "value2");
        Assert.assertEquals(combinedMap2.get("key2"), "DEFAULT");
        Assert.assertEquals(combinedMap2.getDiffMap().get("key2"), "DEFAULT");
    }

    @Test
    public void test4() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.put("key2", "value2");
        combinedMap2.setSharedMap(sharedMap);

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 1);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 1);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 1);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 1);

        Assert.assertEquals(combinedMap2.get("key2"), "value2");
        Assert.assertEquals(combinedMap1.get("key2"), "DEFAULT");
        Assert.assertEquals(combinedMap1.getDiffMap().get("key2"), "DEFAULT");
    }

    @Test
    public void test42() {
        SharedMap<String, String> sharedMap = new SharedMap<>("DEFAULT");

        CombinedMap<String, String> combinedMap1 = new CombinedMap<>();
        combinedMap1.put("key1", "value1");
        combinedMap1.setSharedMap(sharedMap);

        CombinedMap<String, String> combinedMap2 = new CombinedMap<>();
        combinedMap2.put("key1", "value1");
        combinedMap2.setSharedMap(sharedMap);

        combinedMap2.put("key2", "value2");

        assertMapSizeEquals(combinedMap1, 2);
        assertMapSizeEquals(combinedMap1.getSameMap(), 1);
        assertMapSizeEquals(combinedMap1.getDiffMap(), 1);

        assertMapSizeEquals(combinedMap2, 2);
        assertMapSizeEquals(combinedMap2.getSameMap(), 1);
        assertMapSizeEquals(combinedMap2.getDiffMap(), 1);

        Assert.assertEquals(combinedMap2.get("key2"), "value2");
        Assert.assertEquals(combinedMap1.get("key2"), "DEFAULT");
        Assert.assertEquals(combinedMap1.getDiffMap().get("key2"), "DEFAULT");
    }

    private void assertMapSizeEquals(Map<String, String> map, int size) {
        Assert.assertEquals(map.size(), size);

        for (Map.Entry<String, String> e : map.entrySet()) {
            Assert.assertNotEquals(e, null);
            Assert.assertNotEquals(e.getKey(), null);
            Assert.assertNotEquals(e.getValue(), null);
        }
    }
}
