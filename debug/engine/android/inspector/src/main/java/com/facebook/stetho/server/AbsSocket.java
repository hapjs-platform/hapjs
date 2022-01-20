/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.facebook.stetho.server;

import android.net.Credentials;
import android.net.LocalSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public abstract class AbsSocket {

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;

    public abstract void close() throws IOException;

    public Credentials getPeerCredentials() throws IOException {
        return null;
    }

    static class LocalSocketImpl extends AbsSocket {
        LocalSocket mSocket;

        LocalSocketImpl(LocalSocket socket) {
            mSocket = socket;
        }

        public InputStream getInputStream() throws IOException {
            return mSocket.getInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return mSocket.getOutputStream();
        }

        public void close() throws IOException {
            mSocket.close();
        }

        public Credentials getPeerCredentials() throws IOException {
            return mSocket.getPeerCredentials();
        }
    }

    static class SocketImpl extends AbsSocket {
        Socket mSocket;

        SocketImpl(Socket socket) {
            mSocket = socket;
        }

        public InputStream getInputStream() throws IOException {
            return mSocket.getInputStream();
        }

        public OutputStream getOutputStream() throws IOException {
            return mSocket.getOutputStream();
        }

        public void close() throws IOException {
            mSocket.close();
        }
    }
}
