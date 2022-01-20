/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import java.io.IOException;
import java.io.InputStream;

public interface Source {
    InputStream open() throws IOException;
}
