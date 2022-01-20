/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.widgets.input.phone;

import java.util.List;

public interface PhoneStorageProvider {
    String NAME = "phoneStorage";

    boolean add(String number);

    boolean delete(String number);

    List<String> getAll();
}
