/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Widget implements Cloneable {

    private static final String KEY_NAME = "name";
    private static final String KEY_TYPES = "types";
    private static final String KEY_METHODS = "methods";

    private String name;
    private String classname;
    private List<Type> types;
    private List<String> methods;
    private List<String> superClasses;
    private boolean needDeleteSuperClasses;

    public Widget() {
        this.types = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.superClasses = new ArrayList<>();
    }

    public Widget(String name, String classname) {
        this();
        this.name = name;
        this.classname = classname;
    }

    public boolean getNeedDeleteSuperClasses() {
        return needDeleteSuperClasses;
    }

    public void setNeedDeleteSuperClasses(boolean needDeleteSuperClasses) {
        this.needDeleteSuperClasses = needDeleteSuperClasses;
    }

    public List<String> getSuperClasses() {
        return superClasses;
    }

    public void setSuperClasses(List<String> superClasses) {
        this.superClasses = superClasses;
    }

    public void addSuperClass(String superClassname) {
        superClasses.add(superClassname);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public List<Type> getTypes() {
        return types;
    }

    public void setTypes(List<Type> types) {
        this.types = types;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    public void addType(Type type) {
        types.add(type);
    }

    public void addMethod(String method) {
        methods.add(method);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> toJSON() {
        Map<String, Object> json = new HashMap<>();
        json.put(KEY_NAME, name);
        if (!types.isEmpty()) {
            List<String> typesJSON = new ArrayList<>(types.size());
            for (Type type : types) {
                typesJSON.add(type.getName());
            }
            json.put(KEY_TYPES, typesJSON);
        }
        if (!methods.isEmpty()) {
            List<String> methodsJSON = new ArrayList<>(types.size());
            methodsJSON.addAll(methods);
            json.put(KEY_METHODS, methodsJSON);
        }
        return json;
    }
}
