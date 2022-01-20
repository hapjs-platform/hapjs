/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.io;

import android.os.AsyncTask;

public class ReaderTask<T> extends AsyncTask<Void, Void, T> {
    private Reader<T> mReader;
    private Source mSource;

    public ReaderTask(Reader<T> reader, Source source) {
        mReader = reader;
        mSource = source;
    }

    @Override
    protected T doInBackground(Void... voids) {
        return mReader.read(mSource);
    }

    public void executeInPool() {
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
