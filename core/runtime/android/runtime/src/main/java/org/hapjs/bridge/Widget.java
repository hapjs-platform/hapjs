/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.content.Context;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hapjs.component.Component;
import org.hapjs.component.ComponentCreator;
import org.hapjs.component.Container;
import org.hapjs.component.bridge.RenderEventCallback;
import org.hapjs.runtime.HapEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Widget implements ComponentCreator {
    private static final String KEY_NAME = "name";
    private static final String KEY_TYPES = "types";
    private static final String KEY_METHODS = "methods";

    private static final String COMPONENT_KEY_DELIMITER = "::";
    private String name;
    private Class<? extends Component> clazz;
    private Map<String, Type> types = new HashMap<>();
    private Set<String> methods = new HashSet<>();
    private Constructor<? extends Component> constructor;

    public Widget(String name, Class<? extends Component> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public static String getComponentKey(String name, String type) {
        if (type == null) {
            return name + COMPONENT_KEY_DELIMITER;
        } else {
            return name + COMPONENT_KEY_DELIMITER + type;
        }
    }

    public String getName() {
        return name;
    }

    public Class<? extends Component> getClazz() {
        return clazz;
    }

    public void addType(String name, String isDefault) {
        types.put(name, new Type(name, "true".equals(isDefault)));
    }

    public List<Type> getTypes() {
        List<Type> list = new ArrayList<>();
        list.addAll(types.values());
        return list;
    }

    public void removeTypes(List<String> list) {
        for (String type : list) {
            types.remove(type);
        }
    }

    public void addMethod(String method) {
        methods.add(method);
    }

    public Set<String> getMethods() {
        return methods;
    }

    public void removeMethods(List<String> methods) {
        this.methods.removeAll(methods);
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(KEY_NAME, name);
        if (!types.isEmpty()) {
            JSONArray typesJSON = new JSONArray();
            for (Type type : types.values()) {
                typesJSON.put(type.name);
            }
            json.put(KEY_TYPES, typesJSON);
        }
        if (!methods.isEmpty()) {
            JSONArray methodsJSON = new JSONArray();
            for (String method : methods) {
                methodsJSON.put(method);
            }
            json.put(KEY_METHODS, methodsJSON);
        }
        return json;
    }

    public List<String> getComponentKeys() {
        List<String> keys = new ArrayList<>();
        if (types.isEmpty()) {
            keys.add(getComponentKey(name, null));
        } else {
            for (Type type : types.values()) {
                keys.add(getComponentKey(name, type.name));
                if (type.isDefault) {
                    keys.add(getComponentKey(name, null));
                }
            }
        }
        return keys;
    }

    @Override
    public Component createComponent(
            HapEngine hapEngine,
            Context context,
            Container parent,
            int ref,
            RenderEventCallback callback,
            Map<String, Object> componentInfo,
            Map<String, Object> savedState) {
        try {
            return getConstructor()
                    .newInstance(hapEngine, context, parent, ref, callback, savedState);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create element", e);
        }
    }

    private Constructor<? extends Component> getConstructor()
            throws ClassNotFoundException, NoSuchMethodException {
        if (constructor == null) {
            constructor =
                    clazz.getConstructor(
                            HapEngine.class,
                            Context.class,
                            Container.class,
                            int.class,
                            RenderEventCallback.class,
                            Map.class);
        }
        return constructor;
    }

    private static class Type {
        final String name;
        final boolean isDefault;

        Type(String name, boolean isDefault) {
            this.name = name;
            this.isDefault = isDefault;
        }
    }
}
