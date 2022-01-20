/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.debugger.server;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hapjs.debugger.DebuggerApplication;
import org.hapjs.debugger.utils.AppUtils;
import org.hapjs.debugger.utils.HttpUtils;
import org.hapjs.debugger.utils.PreferenceUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Server {
    private static final String TAG = "Server";

    private static final int REQUEST_CODE_UPDATE = 0;
    private static final int REQUEST_CODE_VERSION = 1;
    private static final int REQUEST_CODE_REPORT_SN = 2;
    private static final int REQUEST_CODE_SELECT_PLATFORM = 3;
    private static final int REQUEST_CODE_DEBUG = 4;
    private static final int REQUEST_CODE_AVAILABLE_PLATFORMS = 5;

    private static final String PARAM_VERSIONS = "versions";
    private static final String PARAM_SN = "sn";
    private static final String PARAM_DEBUGGER = "debugger";
    private static final String PARAM_DEBUG_CORE = "debug_core";
    private static final String PARAM_PLATFORM = "platform";
    private static final String PARAM_AVAILABLE_PLATFORMS = "availablePlatforms";
    private static final String PARAM_DEVICE_MODEL = "deviceModel";

    private static final String PARAM_PKG = "pkg";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_VERSION = "version";
    private static final String PARAM_SELECTED_PLATFORM = "selectedPlatform";
    private static final String PARAM_DEBUG = "debug";

    private static final String LOCAL_HOST = "127.0.0.1";

    private static final int PORT = 39517;

    private static final Map<String, Integer> sRequestMap = new HashMap<>();
    @Deprecated
    private static final String DEBUG_CORE_PACKAGE_NAME_PREFIX = "org.hapjs.debug.core.v";

    static {
        sRequestMap.put("/update", REQUEST_CODE_UPDATE);
        sRequestMap.put("/deviceinfo", REQUEST_CODE_VERSION);
        sRequestMap.put("/reportsn", REQUEST_CODE_REPORT_SN);
        sRequestMap.put("/platform", REQUEST_CODE_SELECT_PLATFORM);
        sRequestMap.put("/debug", REQUEST_CODE_DEBUG);
        sRequestMap.put("/availablePlatforms", REQUEST_CODE_AVAILABLE_PLATFORMS);
    }

    private Thread mServerThread;
    private ServerSocket mServerSocket;
    private ServerListener mListener;

    @Deprecated
    private static String getDebugCorePackageName(int platformVersionCode) {
        return DEBUG_CORE_PACKAGE_NAME_PREFIX + platformVersionCode;
    }

    public void setListener(ServerListener listener) {
        mListener = listener;
    }

    public void start() {
        if (mServerThread != null) {
            throw new IllegalStateException("Server is running");
        }
        mServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serve();
                } catch (IOException e) {
                    Log.e(TAG, "Server error occurs", e);
                }
            }
        });
        mServerThread.start();
    }

    public void stop() {
        closeSocket();
        if (mServerThread != null) {
            mServerThread.interrupt();
            mServerThread = null;
        }
    }

    private void serve() throws IOException {
        for (int i = 0; i < 10; ++i) {
            try {
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(PORT + i));
                break;
            } catch (IOException e) {
                Log.e(TAG, "Fail to bind port: " + (PORT + i), e);
            }
        }
        if (mServerSocket == null) {
            Log.e(TAG, "Fail to start server");
            return;
        }
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket;
                try {
                    socket = mServerSocket.accept();
                } catch (SocketException e) {
                    return;
                }
                boolean useADB = PreferenceUtils.isUseADB(DebuggerApplication.getInstance());
                String clientIp = socket.getLocalAddress().getHostAddress();
                if ((useADB && !LOCAL_HOST.equals(clientIp)) || (!useADB && LOCAL_HOST.equals(clientIp))) {
                    socket.close();
                    continue;
                }
                List<String> requestLines = readRequest(socket);
                String path = getRequestPath(requestLines);
                Integer requestCode = path == null ? null : sRequestMap.get(path);
                writeResponse(socket, requestCode);
                socket.close();

                if (requestCode != null && mListener != null) {
                    if (requestCode == REQUEST_CODE_UPDATE) {
                        mListener.onUpdate(isDebug(requestLines));
                    } else if (requestCode == REQUEST_CODE_REPORT_SN) {
                        mListener.onUpdateSerialNumber(getSerialNumber(requestLines));
                    } else if (requestCode == REQUEST_CODE_SELECT_PLATFORM) {
                        mListener.onSelectPlatform(getSelectedPlatform(requestLines));
                    } else if (requestCode == REQUEST_CODE_DEBUG) {
                        mListener.onDebug();
                    }
                }
            }
        } finally {
            closeSocket();
        }
    }

    private List<String> readRequest(Socket socket) throws IOException {
        List<String> requestLines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        String line = reader.readLine();
        while (line != null && !line.isEmpty()) {
            requestLines.add(line);
            line = reader.readLine();
        }
        return requestLines;
    }

    private void writeResponse(Socket socket, Integer code) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
        if (code == null) {
            writer.write("HTTP/1.1 404 Not Found\r\nConnection: Close\r\nContent-Length: 0\r\n\r\n");
            return;
        }

        JSONObject result = new JSONObject();
        switch (code) {
            case REQUEST_CODE_UPDATE:
            case REQUEST_CODE_REPORT_SN:
            case REQUEST_CODE_SELECT_PLATFORM:
                writer.write(
                        "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: Close\r\nTransfer-Encoding: "
                                + "chunked\r\n\r\n2\r\nOK\r\n0\r\n\r\n");
                break;
            case REQUEST_CODE_VERSION:
                Context context = DebuggerApplication.getInstance();
                int debuggerVersion = AppUtils.getVersionCode(context, context.getPackageName());
                int platformVersion = AppUtils.getPlatformVersion(context, PreferenceUtils.getPlatformPackage(context));
                int debugCoreVersion = AppUtils.getVersionCode(context, getDebugCorePackageName(platformVersion));
                JSONObject versionsJSON = new JSONObject();
                try {
                    versionsJSON.put(PARAM_DEVICE_MODEL, Build.MODEL);
                    versionsJSON.put(PARAM_DEBUGGER, debuggerVersion);
                    versionsJSON.put(PARAM_DEBUG_CORE, debugCoreVersion);
                    versionsJSON.put(PARAM_PLATFORM, platformVersion);
                    result.put(PARAM_VERSIONS, versionsJSON);
                    result.put(PARAM_SN, AppUtils.getSerialNumber());
                    String jsonResponse = result.toString();
                    writer.write("HTTP/1.1 200 OK\r\n"
                            + "Content-Type: text/json; charset=utf-8\r\n"
                            + "Connection: Close\r\n\r\n"
                            + jsonResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to write response", e);
                }
                break;
            case REQUEST_CODE_AVAILABLE_PLATFORMS:
                try {
                    result.put(PARAM_AVAILABLE_PLATFORMS, getAvailablePlatforms());
                    String jsonResponse = result.toString();
                    writer.write("HTTP/1.1 200 OK\r\n"
                            + "Content-Type: text/json; charset=utf-8\r\n"
                            + "Connection: Close\r\n\r\n"
                            + jsonResponse);
                } catch (JSONException e) {
                    Log.e(TAG, "Fail to write response", e);
                }
                break;
            default:
                writer.write("HTTP/1.1 404 Not Found\r\nConnection: Close\r\nContent-Length: 0\r\n\r\n");
                break;
        }

        writer.flush();
    }

    private JSONArray getAvailablePlatforms() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        if (mListener != null) {
            List<PlatformInfo> platformInfos = mListener.onRequestAvailablePlatforms();
            if (platformInfos != null) {
                for (PlatformInfo info : platformInfos) {
                    jsonArray.put(info.toJson());
                }
            }
        }
        return jsonArray;
    }

    private String getRequestPath(List<String> requestLines) {
        for (String line : requestLines) {
            if (line.startsWith("GET")) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    return parts[1];
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean isDebug(List<String> requestLines) {
        return "true".equals(getParam(requestLines, PARAM_DEBUG));
    }

    private String getSerialNumber(List<String> requestLines) {
        return getParam(requestLines, HttpUtils.HEADER_SERIAL_NUMBER);
    }

    private String getSelectedPlatform(List<String> requestLines) {
        return getParam(requestLines, PARAM_SELECTED_PLATFORM);
    }

    private String getParam(List<String> requestLines, String param) {
        for (String line : requestLines) {
            if (line.startsWith(param)) {
                String[] parts = line.split(": ");
                if (parts.length >= 2) {
                    return parts[1];
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private void closeSocket() {
        if (mServerSocket != null) {
            if (!mServerSocket.isClosed()) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public interface ServerListener {
        void onUpdate(boolean isDebug);

        void onUpdateSerialNumber(String serialNumber);

        List<PlatformInfo> onRequestAvailablePlatforms();

        void onSelectPlatform(String pkg);

        void onDebug();
    }

    public static class PlatformInfo {
        private String mPkg;
        private String mName;
        private long mVersion;

        public PlatformInfo(String pkg, String name, long version) {
            mPkg = pkg;
            mName = name;
            mVersion = version;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(PARAM_PKG, mPkg);
            jsonObject.put(PARAM_NAME, mName);
            jsonObject.put(PARAM_VERSION, mVersion);
            return jsonObject;
        }
    }
}
