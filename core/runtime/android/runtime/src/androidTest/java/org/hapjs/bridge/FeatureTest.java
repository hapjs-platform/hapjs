/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class FeatureTest {
    @Test
    public void testAddMethod() throws Exception {
        ExtensionMetaData feature = new ExtensionMetaData("system.test", "test");
        feature.addMethod("a", Extension.Mode.SYNC);
        feature.addMethod("b", Extension.Mode.ASYNC);
        feature.addMethod("c", Extension.Mode.CALLBACK);
        try {
            feature.addMethod("d", null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod("", Extension.Mode.SYNC);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(null, Extension.Mode.SYNC);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod("e", Extension.Mode.SYNC, new String[] {"perm"});
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        feature.addMethod("f", Extension.Mode.ASYNC, new String[] {"perm"});
        feature.addMethod("g", Extension.Mode.CALLBACK, new String[] {"perm"});
        try {
            feature.addMethod(
                    "h", Extension.Mode.SYNC, Extension.Type.ATTRIBUTE, Extension.Access.NONE, "",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "h", Extension.Mode.SYNC, Extension.Type.ATTRIBUTE, Extension.Access.READ, "",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "h", Extension.Mode.SYNC, Extension.Type.ATTRIBUTE, Extension.Access.READ, "h",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        feature.addMethod(
                "__getH", Extension.Mode.SYNC, Extension.Type.ATTRIBUTE, Extension.Access.READ, "h",
                null);
        try {
            feature.addMethod(
                    "__getH",
                    Extension.Mode.SYNC,
                    Extension.Type.ATTRIBUTE,
                    Extension.Access.READ,
                    "h",
                    new String[] {"perm"});
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "__getH",
                    Extension.Mode.SYNC,
                    Extension.Type.ATTRIBUTE,
                    Extension.Access.WRITE,
                    "h",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        feature.addMethod(
                "__setH", Extension.Mode.SYNC, Extension.Type.ATTRIBUTE, Extension.Access.WRITE,
                "h", null);
        feature.addMethod(
                "__onI", Extension.Mode.CALLBACK, Extension.Type.EVENT, Extension.Access.NONE,
                "onI", null);
        try {
            feature.addMethod(
                    "onI", Extension.Mode.CALLBACK, Extension.Type.EVENT, Extension.Access.NONE,
                    "onI", null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "__onI", Extension.Mode.SYNC, Extension.Type.EVENT, Extension.Access.NONE,
                    "onI", null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "__onI",
                    Extension.Mode.CALLBACK,
                    Extension.Type.EVENT,
                    Extension.Access.READ,
                    "onI",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "__onI",
                    Extension.Mode.CALLBACK,
                    Extension.Type.EVENT,
                    Extension.Access.NONE,
                    "_onI",
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            feature.addMethod(
                    "__onI",
                    Extension.Mode.CALLBACK,
                    Extension.Type.EVENT,
                    Extension.Access.NONE,
                    null,
                    null);
            Assert.assertTrue(false);
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
}
