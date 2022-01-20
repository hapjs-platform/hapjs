/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.bridge;

import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExtensionMetaData {
    public static final String JSON_KEY_SUBATTRS = "subAttrs";
    private static final String TAG = "ExtensionMetaData";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_INSTANTIATE = "instantiable";
    private static final String JSON_KEY_METHODS = "methods";
    private static final String JSON_KEY_MODE = "mode";
    private static final String JSON_KEY_TYPE = "type";
    private static final String JSON_KEY_INSTANCE_METHOD = "instanceMethod";
    private static final String JSON_KEY_ACCESS = "access";
    private static final String JSON_KEY_NORMALIZE = "normalize";
    private static final String JSON_KEY_MULTIPLE = "multiple";
    private static final String JSON_KEY_ALIAS = "alias";
    private static final boolean INSTANCE_METHOD_DEFAULT = false;
    private static final Extension.Type TYPE_DEFAULT = Extension.Type.FUNCTION;
    private static final Extension.Access ACCESS_DEFAULT = Extension.Access.NONE;
    private static final Extension.Normalize NORMALIZE_DEFAULT = Extension.Normalize.JSON;
    private static final Extension.Multiple MULTIPLE_DEFAULT = Extension.Multiple.SINGLE;

    private String name;
    private String module;
    private Map<String, Method> methods = new HashMap<>();

    public ExtensionMetaData(String name, String module) {
        this.name = name;
        this.module = module;
    }

    public String getName() {
        return name;
    }

    public String getModule() {
        return module;
    }

    public String[] getMethods() {
        return methods.keySet().toArray(new String[methods.size()]);
    }

    public void addMethod(String name, Extension.Mode mode) {
        addMethod(name, mode, null);
    }

    public void addMethod(String name, Extension.Mode mode, String[] permissions) {
        addMethod(name, INSTANCE_METHOD_DEFAULT, mode, TYPE_DEFAULT, ACCESS_DEFAULT, null,
                permissions);
    }

    public void addMethod(
            String name,
            boolean instanceMethod,
            Extension.Mode mode,
            Extension.Type type,
            Extension.Access access,
            String alias,
            String[] permissions) {
        addMethod(name, instanceMethod, mode, type, access, NORMALIZE_DEFAULT, alias, permissions);
    }

    public void addMethod(
            String name,
            boolean instanceMethod,
            Extension.Mode mode,
            Extension.Type type,
            Extension.Access access,
            Extension.Normalize normalize,
            String alias,
            String[] permissions) {
        addMethod(
                name, instanceMethod, mode, type, access, normalize, MULTIPLE_DEFAULT, alias,
                permissions);
    }

    public void addMethod(
            String name,
            boolean instanceMethod,
            Extension.Mode mode,
            Extension.Type type,
            Extension.Access access,
            Extension.Normalize normalize,
            Extension.Multiple multiple,
            String alias,
            String[] permissions) {
        addMethod(
                name, instanceMethod, mode, type, access, normalize, multiple, alias, permissions,
                null);
    }

    public void addMethod(
            String name,
            boolean instanceMethod,
            Extension.Mode mode,
            Extension.Type type,
            Extension.Access access,
            Extension.Normalize normalize,
            Extension.Multiple multiple,
            String alias,
            String[] permissions,
            String[] subAttrs) {
        methods.put(
                name,
                new Method(
                        name,
                        instanceMethod,
                        mode,
                        type,
                        access,
                        normalize,
                        multiple,
                        alias,
                        permissions,
                        subAttrs));
    }

    public void addProxyMethod(String name, String proxyName, Extension.Mode mode) {
        Method proxyMethod = methods.get(proxyName);
        if (proxyMethod != null) {
            addMethod(
                    name,
                    proxyMethod.instanceMethod,
                    mode,
                    proxyMethod.type,
                    proxyMethod.access,
                    proxyMethod.normalize,
                    proxyMethod.multiple,
                    proxyMethod.alias,
                    proxyMethod.permissions,
                    proxyMethod.subAttrs);
        }
    }

    public void removeMethods(List<String> list) {
        for (String method : list) {
            methods.remove(method);
        }
    }

    public boolean hasMethod(String name) {
        return methods.get(name) != null;
    }

    public Extension.Mode getInvocationMode(String name) {
        Method method = methods.get(name);
        return method == null ? null : method.mode;
    }

    public String[] getPermissions(String name) {
        Method method = methods.get(name);
        return method == null ? null : method.permissions;
    }

    public JSONObject toJSON() throws JSONException {
        return toJSON(null);
    }

    public JSONObject toJSON(String fullName) throws JSONException {
        JSONArray methodsJSON = new JSONArray();
        boolean instantiable = false;
        for (Method method : methods.values()) {
            JSONObject methodJSON = new JSONObject();
            methodJSON.put(JSON_KEY_NAME, method.name);
            methodJSON.put(JSON_KEY_MODE, method.mode.ordinal());
            if (method.instanceMethod != INSTANCE_METHOD_DEFAULT) {
                methodJSON.put(JSON_KEY_INSTANCE_METHOD, method.instanceMethod);
            }
            if (method.type != null && method.type != TYPE_DEFAULT) {
                methodJSON.put(JSON_KEY_TYPE, method.type.ordinal());
            }
            if (method.access != null && method.access != ACCESS_DEFAULT) {
                methodJSON.put(JSON_KEY_ACCESS, method.access.ordinal());
            }
            if (method.normalize != null && method.normalize != NORMALIZE_DEFAULT) {
                methodJSON.put(JSON_KEY_NORMALIZE, method.normalize.ordinal());
            }
            if (method.multiple != null && method.multiple != MULTIPLE_DEFAULT) {
                methodJSON.put(JSON_KEY_MULTIPLE, method.multiple.ordinal());
            }
            if (method.alias != null && !method.alias.isEmpty()) {
                methodJSON.put(JSON_KEY_ALIAS, method.alias);
            }
            if (method.subAttrs != null && method.subAttrs.length > 0) {
                JSONArray subAttrArray = new JSONArray();
                for (String subAttr : method.subAttrs) {
                    subAttrArray.put(subAttr);
                }
                methodJSON.put(JSON_KEY_SUBATTRS, subAttrArray);
            }
            methodsJSON.put(methodJSON);

            if (TextUtils.equals(method.name, Extension.ACTION_INIT)) {
                instantiable = true;
            }
        }
        JSONObject result = new JSONObject();
        result.put(JSON_KEY_NAME, fullName != null ? fullName : name);
        result.put(JSON_KEY_METHODS, methodsJSON);
        result.put(JSON_KEY_INSTANTIATE, instantiable);
        return result;
    }

    public ExtensionMetaData alias(String name) {
        ExtensionMetaData extensionMetaData = new ExtensionMetaData(name, module);
        extensionMetaData.methods = methods;
        return extensionMetaData;
    }

    public void validate() {
        String errmsg = getErrorMessage();
        if (errmsg != null) {
            JSONObject json = new JSONObject();
            try {
                json = toJSON();
            } catch (JSONException e) {
                Log.e(TAG, "fail to toJSON", e);
            }
            throw new IllegalArgumentException(errmsg + ": " + json.toString());
        }
    }

    private String getErrorMessage() {
        boolean instantiable = false;
        boolean hasInstanceMethod = false;
        for (Method method : methods.values()) {
            if (Extension.ACTION_INIT.equals(method.name)) {
                instantiable = true;
            }
            if (method.instanceMethod) {
                hasInstanceMethod = true;
            }
        }

        if (!instantiable && hasInstanceMethod) {
            return "feature is not instantiable but has instanceMethod";
        }
        return null;
    }

    private static class Method {
        final String name;
        final boolean instanceMethod;
        final Extension.Mode mode;
        final Extension.Type type;
        final Extension.Access access;
        final Extension.Normalize normalize;
        final Extension.Multiple multiple;
        final String alias;
        final String[] permissions;
        final String[] subAttrs;

        public Method(
                String name,
                boolean instanceMethod,
                Extension.Mode mode,
                Extension.Type type,
                Extension.Access access,
                Extension.Normalize normalize,
                Extension.Multiple multiple,
                String alias,
                String[] permissions,
                String[] subAttrs) {
            this.name = name;
            this.instanceMethod = instanceMethod;
            this.mode = mode;
            this.type = type == null ? TYPE_DEFAULT : type;
            this.access = access == null ? ACCESS_DEFAULT : access;
            this.normalize = normalize == null ? NORMALIZE_DEFAULT : normalize;
            this.multiple = multiple == null ? MULTIPLE_DEFAULT : multiple;
            this.alias = alias;
            this.permissions = permissions;
            this.subAttrs = subAttrs;
            validate();
        }

        private void validate() {
            String errmsg = getErrorMessage();
            if (errmsg != null) {
                throw new IllegalArgumentException(errmsg + ": " + toString());
            }
        }

        private String getErrorMessage() {
            if (name == null || name.isEmpty()) {
                return "the name of method must not be empty";
            }
            if (mode == null) {
                return "the mode of method must not be null";
            }
            switch (type) {
                case FUNCTION:
                    if (access != Extension.Access.NONE) {
                        return "the access of function must be none";
                    }
                    if (mode == Extension.Mode.SYNC
                            && (permissions != null && permissions.length > 0)) {
                        return "the permissions of sync function must be empty";
                    }
                    break;
                case EVENT:
                    if (!name.startsWith("__on")) {
                        return "the name of event must start with '__on'";
                    }
                    if (mode != Extension.Mode.CALLBACK) {
                        return "the mode of event must be callback";
                    }
                    if (access != Extension.Access.NONE) {
                        return "the access of event must be none";
                    }
                    if (alias == null || !alias.startsWith("on")) {
                        return "the alias of event must start with 'on'";
                    }
                    if (!alias.equals(alias.toLowerCase(Locale.ROOT))) {
                        return "the alias of event must be all lower case characters";
                    }
                    break;
                case ATTRIBUTE:
                    if (mode != Extension.Mode.SYNC) {
                        return "the mode of attribute must be sync";
                    }
                    if (alias == null || alias.isEmpty()) {
                        return "the alias of attribute must not be empty";
                    }
                    if (permissions != null && permissions.length > 0) {
                        return "the permissions of attribute must be empty";
                    }
                    switch (access) {
                        case NONE:
                            return "the access of attribute must not be none";
                        case READ:
                            if (!name.startsWith("__get")) {
                                return "the name of attribute must start with '__get'";
                            }
                            break;
                        case WRITE:
                            if (!name.startsWith("__set")) {
                                return "the name of attribute must start with '__set'";
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }

            if (Extension.ACTION_INIT.equals(name)) {
                if (instanceMethod) {
                    throw new IllegalArgumentException("constructor must NOT be instanceMethod");
                } else if (mode != Extension.Mode.SYNC) {
                    return "constructor must be SYNC";
                } else if (type != Extension.Type.FUNCTION) {
                    return "constructor must be FUNCTION";
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("Method(")
                    .append("name=")
                    .append(name)
                    .append(", instanceMethod=")
                    .append(instanceMethod)
                    .append(", mode=")
                    .append(mode)
                    .append(", type=")
                    .append(type)
                    .append(", access=")
                    .append(access)
                    .append(", normalize=")
                    .append(normalize)
                    .append(", multiple=")
                    .append(multiple)
                    .append(", alias=")
                    .append(alias)
                    .append(", permissions=")
                    .append(Arrays.toString(permissions))
                    .append(", subAttrs=")
                    .append(Arrays.toString(subAttrs))
                    .append(")")
                    .toString();
        }
    }
}
