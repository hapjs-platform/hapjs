/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import java.util.ArrayList;
import java.util.List;

public class Inherited {
    private String classname;
    private List<String> superClasses;

    public Inherited() {
        superClasses = new ArrayList<>();
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

    public Extension[] resolveExtension(List<Extension> extensions) {
        for (String superClass : superClasses) {
            for (Extension extension : extensions) {
                if (extension.getClassname().equals(superClass)) {
                    Extension result = (Extension) extension.clone();
                    result.setClassname(classname);
                    result.setSuperClasses(superClasses);
                    return new Extension[] {extension, result};
                }
            }
        }
        return null;
    }

    public Widget[] resolveWidget(List<Widget> widgets) {
        for (String superClass : superClasses) {
            for (Widget widget : widgets) {
                if (widget.getClassname().equals(superClass)) {
                    Widget result = (Widget) widget.clone();
                    result.setClassname(classname);
                    result.setSuperClasses(superClasses);
                    result.setNeedDeleteSuperClasses(true);
                    return new Widget[] {widget, result};
                }
            }
        }
        return null;
    }
}
