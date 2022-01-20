/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import org.hapjs.bridge.Extension;
import org.hapjs.bridge.annotation.ActionAnnotation;

public class Method {
    private String name;
    private boolean instanceMethod;
    private Extension.Mode mode;
    private Extension.Type type;
    private Extension.Access access;
    private Extension.Normalize normalize;
    private Extension.Multiple multiple;
    private String alias;
    private String[] permissions;
    private String[] subAttrs;
    private ActionAnnotation.ResidentType residentType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInstanceMethod() {
        return instanceMethod;
    }

    public void setInstanceMethod(boolean instanceMethod) {
        this.instanceMethod = instanceMethod;
    }

    public Extension.Mode getMode() {
        return mode;
    }

    public void setMode(Extension.Mode mode) {
        this.mode = mode;
    }

    public Extension.Type getType() {
        return type;
    }

    public void setType(Extension.Type type) {
        this.type = type;
    }

    public Extension.Access getAccess() {
        return access;
    }

    public void setAccess(Extension.Access access) {
        this.access = access;
    }

    public Extension.Normalize getNormalize() {
        return normalize;
    }

    public void setNormalize(Extension.Normalize normalize) {
        this.normalize = normalize;
    }

    public Extension.Multiple getMultiple() {
        return multiple;
    }

    public void setMultiple(Extension.Multiple multiple) {
        this.multiple = multiple;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String[] getPermissions() {
        return permissions;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
    }

    public String[] getSubAttrs() {
        return subAttrs;
    }

    public void setSubAttrs(String[] subAttrs) {
        this.subAttrs = subAttrs;
    }

    public ActionAnnotation.ResidentType getResidentType() {
        return residentType;
    }

    public void setResidentType(ActionAnnotation.ResidentType residentType) {
        this.residentType = residentType;
    }
}
