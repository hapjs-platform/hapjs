/*
 * Copyright (c) 2021-present, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionMetaData {
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
    private static final String JSON_KEY_SUBATTRS = "subAttrs";

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

    public void addMethod(String name,
                          boolean instanceMethod,
                          String mode,
                          String type,
                          String access,
                          String normalize,
                          String multiple,
                          String alias,
                          String[] permissions,
                          String[] subAttrs) {
        methods.put(name,
                new Method(name, instanceMethod, mode, type, access, normalize, multiple, alias,
                        permissions, subAttrs));
    }


    public void removeMethods(List<String> list) {
        for (String method : list) {
            methods.remove(method);
        }
    }

    public boolean hasMethod(String name) {
        return methods.get(name) != null;
    }


    public String[] getPermissions(String name) {
        Method method = methods.get(name);
        return method == null ? null : method.permissions;
    }

    public Map<String, Object> toJSON() {
        return toJSON(null);
    }

    public Map<String, Object> toJSON(String fullName) {
        List methodsJSON = new ArrayList();
        boolean instantiable = false;
        for (Method method : methods.values()) {
            Map methodJSON = new HashMap<>();
            methodJSON.put(JSON_KEY_NAME, method.name);
            methodJSON.put(JSON_KEY_MODE, Extension.getOrdinalBy(method.mode));
            if (method.instanceMethod != INSTANCE_METHOD_DEFAULT) {
                methodJSON.put(JSON_KEY_INSTANCE_METHOD, method.instanceMethod);
            }
            if (method.type != null && !method.type.equals(TYPE_DEFAULT.toString())) {
                methodJSON.put(JSON_KEY_TYPE, Extension.getOrdinalBy(method.type));
            }
            if (method.access != null && !method.access.equals(ACCESS_DEFAULT.toString())) {
                methodJSON.put(JSON_KEY_ACCESS, Extension.getOrdinalBy(method.access));
            }
            if (method.normalize != null
                    && !method.normalize.equals(NORMALIZE_DEFAULT.toString())) {
                methodJSON.put(JSON_KEY_NORMALIZE, Extension.getOrdinalBy(method.normalize));
            }
            if (method.multiple != null && !method.multiple.equals(MULTIPLE_DEFAULT.toString())) {
                methodJSON.put(JSON_KEY_MULTIPLE, Extension.getOrdinalBy(method.multiple));
            }
            if (method.alias != null && !method.alias.isEmpty()) {
                methodJSON.put(JSON_KEY_ALIAS, method.alias);
            }
            if (method.subAttrs != null && method.subAttrs.length > 0) {
                List<String> subAttrArray = new ArrayList<>(method.subAttrs.length);
                Collections.addAll(subAttrArray, method.subAttrs);
                methodJSON.put(JSON_KEY_SUBATTRS, subAttrArray);
            }
            methodsJSON.add(methodJSON);

            if (Extension.ACTION_INIT.equals(method.name)) {
                instantiable = true;
            }
        }
        Map<String, Object> result = new HashMap<>();
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
        String errMsg = getErrorMessage();
        if (errMsg != null) {
            Map json = new HashMap();
            try {
                json = toJSON();
            } catch (Exception e) {
                e.printStackTrace();
            }
            throw new IllegalArgumentException(errMsg + ": " + json.toString());
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

    public interface Extension {
        String ACTION_INIT = "__init__";

        static int getOrdinalBy(String obj) {
            int ordinal = 0;
            if (Mode.SYNC.name().equals(obj)) {
                ordinal = Mode.SYNC.ordinal();
            } else if (Mode.ASYNC.name().equals(obj)) {
                ordinal = Mode.ASYNC.ordinal();
            } else if (Mode.CALLBACK.name().equals(obj)) {
                ordinal = Mode.CALLBACK.ordinal();
            } else if (Mode.SYNC_CALLBACK.name().equals(obj)) {
                ordinal = Mode.SYNC_CALLBACK.ordinal();
            } else if (Type.FUNCTION.name().equals(obj)) {
                ordinal = Type.FUNCTION.ordinal();
            } else if (Type.ATTRIBUTE.name().equals(obj)) {
                ordinal = Type.ATTRIBUTE.ordinal();
            } else if (Type.EVENT.name().equals(obj)) {
                ordinal = Type.EVENT.ordinal();
            } else if (Access.NONE.name().equals(obj)) {
                ordinal = Access.NONE.ordinal();
            } else if (Access.READ.name().equals(obj)) {
                ordinal = Access.READ.ordinal();
            } else if (Access.WRITE.name().equals(obj)) {
                ordinal = Access.WRITE.ordinal();
            } else if (Normalize.RAW.name().equals(obj)) {
                ordinal = Normalize.RAW.ordinal();
            } else if (Normalize.JSON.name().equals(obj)) {
                ordinal = Normalize.JSON.ordinal();
            } else if (NativeType.INSTANCE.name().equals(obj)) {
                ordinal = NativeType.INSTANCE.ordinal();
            } else if (Multiple.SINGLE.name().equals(obj)) {
                ordinal = Multiple.SINGLE.ordinal();
            } else if (Multiple.MULTI.name().equals(obj)) {
                ordinal = Multiple.MULTI.ordinal();
            }
            return ordinal;
        }

        /**
         * Invocation mode.
         */
        enum Mode {
            /**
             * Synchronous invocation. When calling actions in such mode, caller
             * will get response until invocation finished.
             */
            SYNC,
            /**
             * Asynchronous invocation. When calling actions in such mode, caller
             * will get an empty response immediately, but wait in a different
             * thread to get response until invocation finished.
             */
            ASYNC,
            /**
             * Callback invocation. When calling actions in such mode, caller will
             * get an empty response immediately, but receive response through
             * callback when invocation finished.
             */
            CALLBACK,
            /**
             * Synchronous invocation. When calling actions in such mode, caller
             * will get response until invocation finished, but receive response
             * through callback later.
             */
            SYNC_CALLBACK,
        }

        enum Type {
            FUNCTION,
            ATTRIBUTE,
            EVENT
        }

        enum Access {
            NONE,
            READ,
            WRITE
        }

        enum Normalize {
            RAW,
            JSON
        }

        enum NativeType {
            INSTANCE
        }

        enum Multiple {
            SINGLE,
            MULTI
        }
    }

    private static class Method {
        final String name;
        final boolean instanceMethod;
        final String mode;
        final String type;
        final String access;
        final String normalize;
        final String multiple;
        final String alias;
        final String[] permissions;
        final String[] subAttrs;

        public Method(String name,
                      boolean instanceMethod,
                      String mode,
                      String type,
                      String access,
                      String normalize,
                      String multiple,
                      String alias,
                      String[] permissions,
                      String[] subAttrs) {
            this.name = name;
            this.instanceMethod = instanceMethod;
            this.mode = mode;
            this.type = type == null ? TYPE_DEFAULT.toString() : type;
            this.access = access == null ? ACCESS_DEFAULT.toString() : access;
            this.normalize = normalize == null ? NORMALIZE_DEFAULT.toString() : normalize;
            this.multiple = multiple == null ? MULTIPLE_DEFAULT.toString() : multiple;
            this.alias = alias;
            this.permissions = permissions;
            this.subAttrs = subAttrs;
        }


        @Override
        public String toString() {
            return new StringBuilder().append("Method(")
                    .append("name=").append(name)
                    .append(", instanceMethod=").append(instanceMethod)
                    .append(", mode=").append(mode)
                    .append(", type=").append(type)
                    .append(", access=").append(access)
                    .append(", normalize=").append(normalize)
                    .append(", multiple=").append(multiple)
                    .append(", alias=").append(alias)
                    .append(", permissions=").append(Arrays.toString(permissions))
                    .append(", subAttrs=").append(Arrays.toString(subAttrs))
                    .append(")")
                    .toString();
        }
    }

}

