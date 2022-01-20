/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.common.net;

import androidx.annotation.Keep;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

class TLSSocketFactory extends SSLSocketFactory {
    @Keep
    private SSLSocketFactory delegate;

    public TLSSocketFactory(SSLSocketFactory sslSocketFactory) {
        delegate = sslSocketFactory;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose)
            throws IOException {
        Socket ssl = delegate.createSocket(s, host, port, autoClose);
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket ssl = delegate.createSocket(host, port);
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
            throws IOException {
        Socket ssl = delegate.createSocket(host, port, localHost, localPort);
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        Socket ssl = delegate.createSocket(host, port);
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                               int localPort)
            throws IOException {
        Socket ssl = delegate.createSocket(address, port, localAddress, localPort);
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    @Override
    public Socket createSocket() throws IOException {
        Socket ssl = delegate.createSocket();
        setSupportProtocolAndCipherSuites(ssl);
        return ssl;
    }

    private void setSupportProtocolAndCipherSuites(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            String[] protocols = sslSocket.getSupportedProtocols();
            String[] cipherSuites = sslSocket.getSupportedCipherSuites();

            sslSocket.setEnabledProtocols(protocols);
            sslSocket.setEnabledCipherSuites(cipherSuites);
            sslSocket.getSSLParameters().setProtocols(protocols);
            sslSocket.getSSLParameters().setCipherSuites(cipherSuites);
        }
    }
}
