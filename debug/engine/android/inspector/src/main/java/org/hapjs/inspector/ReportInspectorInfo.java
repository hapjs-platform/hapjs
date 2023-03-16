/*
 * Copyright (c) 2021, the hapjs-platform Project Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hapjs.inspector;

import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.facebook.stetho.inspector.ChromeDevtoolsServer;
import com.facebook.stetho.server.AbsServerSocket;
import com.facebook.stetho.server.SocketLike;
import com.facebook.stetho.server.http.ExactPathMatcher;
import com.facebook.stetho.server.http.HandlerRegistry;
import com.facebook.stetho.server.http.HttpHandler;
import com.facebook.stetho.server.http.HttpStatus;
import com.facebook.stetho.server.http.LightHttpBody;
import com.facebook.stetho.server.http.LightHttpRequest;
import com.facebook.stetho.server.http.LightHttpResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import org.hapjs.common.executors.Executors;
import org.hapjs.debug.log.DebuggerLogUtil;

public class ReportInspectorInfo implements HttpHandler {

    private static final String PATH_STATUS = "/status";
    private static final String PATH_POSTWS = "/poststdbg";
    private static final String LOCAL_SERVER = "127.0.0.1";
    private static final String HEADER_SERIAL_NUMBER = "device-serial-number";

    private String mUrl;
    private int mPort;
    private String mApplicationName;
    private Context mContext;

    private ReportInspectorInfo() {
    }

    public static ReportInspectorInfo getInstance() {
        return Holder.instance;
    }

    public void registerReportURL(String url) {
        if (url.startsWith("inspect://")) {
            mUrl = url.substring("inspect://".length());
        } else {
            mUrl = url + PATH_POSTWS;
        }
    }

    public void registerSocket(AbsServerSocket socket) {
        DebuggerLogUtil.logBreadcrumb("registerSocket");
        if (socket.getSocket() instanceof ServerSocket) {
            ServerSocket ssock = (ServerSocket) socket.getSocket();
            mPort = ssock.getLocalPort();
            DebuggerLogUtil.logBreadcrumb("mPort=" + mPort);
            // try report
            tryReport();
        }
    }

    public void setApplicatonName(String appName) {
        mApplicationName = appName;
    }

    public void register(Context context, HandlerRegistry registry) {
        mContext = context;
        if (mApplicationName == null) {
            // get the package name
            mApplicationName = context.getPackageName();
        }
        if (mUrl == null) {
            mUrl = V8Inspector.getInstance().getUrl();
        }
        if (registry != null) {
            registry.register(new ExactPathMatcher(PATH_STATUS), this);
        }
    }

    public String getReport() {
        // build the json string
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        builder.append("\"protocol-version\":\"1.1\"");
        builder.append(",\"status\":\"" + getHostURL() + PATH_STATUS + "\"");
        builder.append(",\"ws\":\"" + getWsUrl() + "\"");
        builder.append(",\"target\":\"" + V8Inspector.getInstance().getDebugTatget() + "\"");
        builder.append(",\"traceId\":\"" + DebuggerLogUtil.getTraceId() + "\"");
        builder.append(
                ",\"application\":\""
                        + mApplicationName
                        + "("
                        + android.os.Build.MANUFACTURER
                        + "/"
                        + android.os.Build.MODEL
                        + "@"
                        + android.os.Build.VERSION.RELEASE
                        + ")\"");
        builder.append("}");
        return builder.toString();
    }

    public void tryReport() {
        DebuggerLogUtil.logBreadcrumb(
                "tryReport, mUrl="
                        + mUrl
                        + ", mPort="
                        + mPort
                        + ", mContext ！= null is "
                        + (mContext != null));
        if (mUrl != null && mPort > 0 && mContext != null) {
            Executors.io().execute(this::report);
        } else {
            DebuggerLogUtil.logError("tryReport, params invalid");
        }
    }

    public boolean handleRequest(
            SocketLike socket, LightHttpRequest request, LightHttpResponse response)
            throws IOException {
        String path = request.uri.getPath();
        if (PATH_STATUS.equals(path)) {
            handleStatus(request, response);
        } else {
            response.code = HttpStatus.HTTP_NOT_IMPLEMENTED;
            response.reasonPhrase = "Not implemented";
            response.body = LightHttpBody.create("No support for " + path + "\n", "text/plain");
        }
        return true;
    }

    private String getJsonCallback(Uri uri) {
        String callback = uri.getQueryParameter("callback");
        if (callback != null) {
            return callback;
        }
        return uri.getQueryParameter("jsoncallback");
    }

    private void handleStatus(LightHttpRequest request, LightHttpResponse response) {
        String callback = getJsonCallback(request.uri);
        response.code = HttpStatus.HTTP_OK;
        response.reasonPhrase = "OK";
        String report = getReport();
        if (callback != null) {
            report = callback + "(" + report + ")";
        }
        Log.d("ReportInspectorInfo", "send " + report + " with callback=" + callback);
        response.body = LightHttpBody.create(report, "application/json");
    }

    private void report() {
        DebuggerLogUtil.logBreadcrumb("ReportInspectorInfo.report");
        try {
            String result = sendPost("utf-8");

            Log.d("ReportInspectorInfo", "send post result=" + result);
            DebuggerLogUtil.logMessage("ENGINE_POST_DEBUG_ADDRESS");
        } catch (Exception e) {
            DebuggerLogUtil.logBreadcrumb("DEBUGGER_SEND_REPORT FAILURE");
            DebuggerLogUtil.logException(e);
            e.printStackTrace();
        }
    }

    public String getHostURL() {
        String ip = V8Inspector.getInstance().isUseADB() ? LOCAL_SERVER : getWifiIPAddr();
        return ip + ":" + mPort;
    }

    public String getWsUrl(){
        return getHostURL() + ChromeDevtoolsServer.PATH;
    }

    private String getWifiIPAddr() {
        WifiManager wifiManager =
                (WifiManager) mContext.getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            reportError("Wifi is not opend!");
            throw new RuntimeException("Wifi is closed");
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return (ipAddress & 0xff)
                + "."
                + ((ipAddress >> 8) & 0xff)
                + "."
                + ((ipAddress >> 16) & 0xff)
                + "."
                + ((ipAddress >> 24) & 0xff);
    }

    private void reportError(String err) {
        // Toast.makeText(mContext, err, Toast.LENGTH_SHORT).show();
        Log.i("Error:", err);
    }

    private String sendPost(String charset) throws RuntimeException {
        StringBuffer resultBuffer = null;
        // create request paramters
        String paramter = getReport();
        Log.d("ReportInspectorInfo", "send Post to " + mUrl + " with paramters:" + paramter);
        DebuggerLogUtil.logBreadcrumb("send Post to " + mUrl + " with paramters:" + paramter);
        URLConnection con = null;
        OutputStreamWriter osw = null;
        BufferedReader br = null;
        try {
            URL realUrl = new URL(mUrl);
            // open url
            con = realUrl.openConnection();
            con.setRequestProperty("accept", "*/*");
            con.setRequestProperty("connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty(
                    "user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            if (V8Inspector.getInstance().isUseADB()) {
                con.setRequestProperty(HEADER_SERIAL_NUMBER,
                        V8Inspector.getInstance().getSerialNumber());
            }

            con.setDoOutput(true);
            con.setDoInput(true);
            osw = new OutputStreamWriter(con.getOutputStream(), charset);
            if (paramter != null && paramter.length() > 0) {
                osw.write(paramter);
                osw.flush();
            }
            resultBuffer = new StringBuffer();
            // String strContentLength = con.getHeaderField("Content-Length");
            // int contentLength = strContentLength == null ? 0 : Integer.parseInt(strContentLength);
            /*if (contentLength > 0) {*/
            br = new BufferedReader(new InputStreamReader(con.getInputStream(), charset));
            String temp;
            while ((temp = br.readLine()) != null) {
                resultBuffer.append(temp);
            }
            // }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // INSPECTOR MOD BEGIN:
            /* if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    osw = null;
                    throw new RuntimeException(e);
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    br = null;
                    throw new RuntimeException(e);
                }
            } */
            boolean hasCloseException = false;
            IOException ioException = null;
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    osw = null;
                    hasCloseException = true;
                    ioException = e;
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    br = null;
                    throw new RuntimeException(e);
                }
            }
            if (hasCloseException) {
                throw new RuntimeException(ioException);
            }
            // END
        }
        return resultBuffer.toString();
    }

    private static class Holder {
        private static ReportInspectorInfo instance = new ReportInspectorInfo();
    }
}
