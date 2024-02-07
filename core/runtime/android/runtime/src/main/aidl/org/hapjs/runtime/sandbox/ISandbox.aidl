/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.runtime.sandbox;

import android.os.ParcelFileDescriptor;

import org.hapjs.runtime.sandbox.ILogListener;
import org.hapjs.runtime.sandbox.ILogProvider;

interface ISandbox {
    void init(in Map configs);
    ParcelFileDescriptor[] createChannel(in ParcelFileDescriptor[] readSide);
    void setLogProvider(ILogProvider logProvider);
    void setLogListener(ILogListener listener);
}