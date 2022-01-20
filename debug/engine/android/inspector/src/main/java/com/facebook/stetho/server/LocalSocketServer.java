/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.server;

import com.facebook.stetho.common.LogUtil;
import com.facebook.stetho.common.Util;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.hapjs.debug.log.DebuggerLogUtil;
import org.hapjs.inspector.ReportInspectorInfo;

// INSPECTOR ADD BEGIN:
// INSPECTOR END

public class LocalSocketServer {
    private static final String WORKER_THREAD_NAME_PREFIX = "StethoWorker";
    private static final int MAX_BIND_RETRIES = 2;
    private static final int TIME_BETWEEN_BIND_RETRIES_MS = 1000;

    private final String mFriendlyName;
    private final String mAddress;
    private final SocketHandler mSocketHandler;
    private final AtomicInteger mThreadId = new AtomicInteger();

    private Thread mListenerThread;
    private boolean mStopped;
    private AbsServerSocket mServerSocket;

    /**
     * @param friendlyName  identifier to help debug this server, used for naming threads and such.
     * @param address       the local socket address to listen on.
     * @param socketHandler functional handler once a socket is accepted.
     */
    public LocalSocketServer(String friendlyName, String address, SocketHandler socketHandler) {
        mFriendlyName = Util.throwIfNull(friendlyName);
        mAddress = Util.throwIfNull(address);
        mSocketHandler = socketHandler;
    }

    @Nonnull
    private static AbsServerSocket bindToSocket(String address) throws IOException {
        /*int retries = MAX_BIND_RETRIES;
        IOException firstException = null;
        do {
          try {
            if (LogUtil.isLoggable(Log.DEBUG)) {
              LogUtil.d("Trying to bind to @" + address);
            }
            return new AbsServerSocket(address);
          } catch (BindException be) {
            LogUtil.w(be, "Binding error, sleep " + TIME_BETWEEN_BIND_RETRIES_MS + " ms...");
            if (firstException == null) {
              firstException = be;
            }
            Util.sleepUninterruptibly(TIME_BETWEEN_BIND_RETRIES_MS);
          }
        } while (retries-- > 0);

        throw firstException;*/
        return AbsServerSocket.createServerSocket(address);
    }

    public String getName() {
        return mFriendlyName;
    }

    /**
     * Binds to the address and listens for connections.
     *
     * <p>If successful, this thread blocks forever or until {@link #stop} is called, whichever
     * happens first.
     *
     * @throws IOException Thrown on failure to bind the socket.
     */
    public void run() throws IOException {
        synchronized (this) {
            if (mStopped) {
                DebuggerLogUtil.logError("Stetho start fail，LocalSocketServer stoped");
                return;
            }
            mListenerThread = Thread.currentThread();
        }

        listenOnAddress(mAddress);
    }

    private void listenOnAddress(String address) throws IOException {
        DebuggerLogUtil.logBreadcrumb("listenOnAddress, address=" + address);
        mServerSocket = bindToSocket(address);
        // INSPECTOR ADD BEGIN:
        if (mServerSocket == null) {
            DebuggerLogUtil.logError("Stetho start fail, mServerSocket is null");
            return;
        }
        DebuggerLogUtil.logMessage("ENGINE_CREATE_SOCKET_SERVER");
        // END
        LogUtil.i("Listening on @" + address);
        // INSPECTOR ADD
        ReportInspectorInfo.getInstance().registerSocket(mServerSocket);

        DebuggerLogUtil.logMessage("ENGINE_SOCKET_SERVER_ACCEPT");
        while (!Thread.interrupted()) {
            try {
                // Use previously accepted socket the first time around, otherwise wait to
                // accept another.
                AbsSocket socket = mServerSocket.accept();
                DebuggerLogUtil.logBreadcrumb("mServerSocket.accept");
                DebuggerLogUtil.logMessage("ENGINE_SOCKET_ACCEPT");
                // Start worker thread
                Thread t = new WorkerThread(socket, mSocketHandler);
                t.setName(
                        WORKER_THREAD_NAME_PREFIX + "-" + mFriendlyName + "-"
                                + mThreadId.incrementAndGet());
                t.setDaemon(true);
                t.start();
            } catch (SocketException se) {
                // ignore exception if interrupting the thread
                if (Thread.interrupted()) {
                    break;
                }
                DebuggerLogUtil.logException(se);
                LogUtil.w(se, "I/O error");
            } catch (InterruptedIOException ex) {
                DebuggerLogUtil.logException(ex);
                break;
            } catch (IOException e) {
                DebuggerLogUtil.logException(e);
                LogUtil.w(e, "I/O error initialising connection thread");
                break;
            }
        }
        LogUtil.i("Server shutdown on @" + address);
    }

    /**
     * Stops the listener thread and unbinds the address.
     */
    public void stop() {
        synchronized (this) {
            mStopped = true;
            if (mListenerThread == null) {
                return;
            }
            // INSPECTOR ADD:
            mListenerThread.interrupt();
        }

        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            // Don't care...
        }
        DebuggerLogUtil.logMessage("ENGINE_SERVERSOCKET_STOP");
        DebuggerLogUtil.stop();
    }

    private static class WorkerThread extends Thread {
        private final AbsSocket mSocket;
        private final SocketHandler mSocketHandler;

        public WorkerThread(AbsSocket socket, SocketHandler socketHandler) {
            mSocket = socket;
            mSocketHandler = socketHandler;
        }

        @Override
        public void run() {
            try {
                mSocketHandler.onAccepted(mSocket);
            } catch (IOException ex) {
                LogUtil.w("I/O error: %s", ex);
                DebuggerLogUtil.logException(ex);
            } finally {
                try {
                    DebuggerLogUtil.logMessage("ENGINE_SOCEKT_CLOSE");
                    mSocket.close();
                } catch (IOException ignore) {
                    DebuggerLogUtil.logException(ignore);
                }
            }
        }
    }
}
