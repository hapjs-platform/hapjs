/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.build.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

public class MetaData<T> {
    public final List<T> elements;

    public MetaData() {
        elements = new ArrayList<>();
    }

    public List<T> getElements() {
        return elements;
    }

    public void addElement(T element) {
        elements.add(element);
    }

    public void removeElement(T element) {
        elements.remove(element);
    }

    public void addAllElement(MetaData<T> metadata) {
        elements.addAll(metadata.elements);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
