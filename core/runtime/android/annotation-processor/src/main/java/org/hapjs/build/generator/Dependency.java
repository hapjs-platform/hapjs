/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import java.util.ArrayList;
import java.util.List;

public class Dependency {
    private String key;
    private String classname;
    private List<String> superClasses;

    public Dependency() {
        superClasses = new ArrayList<>();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
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
}
