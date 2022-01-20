/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.facebook.stetho.server;

import android.net.LocalServerSocket;
import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.hapjs.debug.log.DebuggerLogUtil;

public abstract class AbsServerSocket {

    private static final int TIME_BETWEEN_BIND_RETRIES_MS = 1000;

    // schema: localsocket://<name>  socket://<port>
    public static AbsServerSocket createServerSocket(String address) throws IOException {
        String[] strArr = address.split("://");
        if (strArr.length != 2) {
            // treat as local socket
            return createLocalServerSocket(address);
        } else if (strArr[0].equals("localsocket")) {
            return createLocalServerSocket(strArr[1]);
        } else if (strArr[0].equals("socket")) {
            return createWebServerSocket(strArr[1]);
        }
        return null;
    }

    private static AbsServerSocket createLocalServerSocket(String address) throws IOException {
        int retries = 2;
        IOException firstException = null;
        do {
            try {
                LocalServerSocket socket = new LocalServerSocket(address);
                return new LocalServerSocketImpl(socket);
            } catch (BindException be) {
                LogUtil.w(be, "Binding error, sleep " + TIME_BETWEEN_BIND_RETRIES_MS + " ms...");
                if (firstException == null) {
                    firstException = be;
                }
                Util.sleepUninterruptibly(TIME_BETWEEN_BIND_RETRIES_MS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (retries-- > 0);

        if (firstException != null) {
            throw firstException;
        }
        return null;
    }

    private static AbsServerSocket createWebServerSocket(String address) throws IOException {
        String[] strArr = address.split(":");

        int port = 0;
        if (strArr.length >= 2) {
            port = Integer.parseInt(strArr[1]);
        } else {
            port = Integer.parseInt(strArr[0]);
        }

        InetAddress inetaddress = null;

        if (strArr.length >= 2) {
            try {
                inetaddress = InetAddress.getByName(strArr[0]);
            } catch (Exception e) {
                DebuggerLogUtil.logBreadcrumb("ServerSocket's inetaddress is null");
                e.printStackTrace();
                return null;
            }
        }
        DebuggerLogUtil.logBreadcrumb(
                String.format(
                        "ServerSocket bind, port: %s ,inetaddress: %s",
                        port, inetaddress != null ? inetaddress.toString() : "null"));

        int retries = 2;
        IOException firstException = null;
        do {
            try {
                if (inetaddress != null) {
                    return new ServerSocketImpl(new ServerSocket(port, 0, inetaddress));
                } else {
                    return new ServerSocketImpl(new ServerSocket(port));
                }
            } catch (IOException be) {
                LogUtil.w(be, "Binding error, sleep " + TIME_BETWEEN_BIND_RETRIES_MS + " ms...");
                if (firstException == null) {
                    firstException = be;
                }
                Util.sleepUninterruptibly(TIME_BETWEEN_BIND_RETRIES_MS);
            }
        } while (retries-- > 0);
        DebuggerLogUtil.logBreadcrumb("ServerSocket binding fail");

        throw firstException;
    }

    public abstract AbsSocket accept() throws IOException;

    public abstract void close() throws IOException;

    public abstract Object getSocket();

    private static class ServerSocketImpl extends AbsServerSocket {
        ServerSocket mSocket;

        ServerSocketImpl(ServerSocket socket) {
            mSocket = socket;
        }

        public AbsSocket accept() throws IOException {
            return new AbsSocket.SocketImpl(mSocket.accept());
        }

        public void close() throws IOException {
            mSocket.close();
        }

        public Object getSocket() {
            return mSocket;
        }
    }

    private static class LocalServerSocketImpl extends AbsServerSocket {
        LocalServerSocket mSocket;

        LocalServerSocketImpl(LocalServerSocket socket) {
            mSocket = socket;
        }

        public AbsSocket accept() throws IOException {
            return new AbsSocket.LocalSocketImpl(mSocket.accept());
        }

        public void close() throws IOException {
            mSocket.close();
        }

        public Object getSocket() {
            return mSocket;
        }
    }
}
