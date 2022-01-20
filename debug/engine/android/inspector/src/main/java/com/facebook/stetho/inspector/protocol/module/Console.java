/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.stetho.inspector.protocol.module;

import android.annotation.SuppressLint;
import com.facebook.stetho.inspector.console.ConsolePeerManager;
import com.facebook.stetho.inspector.jsonrpc.JsonRpcPeer;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsDomain;
import com.facebook.stetho.inspector.protocol.ChromeDevtoolsMethod;
import com.facebook.stetho.json.annotation.JsonProperty;
import com.facebook.stetho.json.annotation.JsonValue;
import java.util.List;
import org.hapjs.inspector.V8Inspector;
import org.json.JSONObject;

public class Console implements ChromeDevtoolsDomain {
    public Console() {
    }

    @ChromeDevtoolsMethod
    public void enable(JsonRpcPeer peer, JSONObject params) {
        ConsolePeerManager.getOrCreateInstance().addPeer(peer);
        // INSPECTOR ADD
        V8Inspector.getInstance().consoleEnabled(peer);
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {
        ConsolePeerManager.getOrCreateInstance().removePeer(peer);
        // INSPECTOR ADD
        V8Inspector.getInstance().consoleDisabled(peer);
    }

    public enum MessageSource {
        XML("xml"),
        JAVASCRIPT("javascript"),
        NETWORK("network"),
        CONSOLE_API("console-api"),
        STORAGE("storage"),
        APPCACHE("appcache"),
        RENDERING("rendering"),
        CSS("css"),
        SECURITY("security"),
        OTHER("other");

        private final String mProtocolValue;

        private MessageSource(String protocolValue) {
            mProtocolValue = protocolValue;
        }

        @JsonValue
        public String getProtocolValue() {
            return mProtocolValue;
        }
    }

    public enum MessageLevel {
        LOG("log"),
        WARNING("warning"),
        ERROR("error"),
        DEBUG("debug");

        private final String mProtocolValue;

        private MessageLevel(String protocolValue) {
            mProtocolValue = protocolValue;
        }

        @JsonValue
        public String getProtocolValue() {
            return mProtocolValue;
        }
    }

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class MessageAddedRequest {
        @JsonProperty(required = true)
        public ConsoleMessage message;
    }

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class ConsoleMessage {
        @JsonProperty(required = true)
        public MessageSource source;

        @JsonProperty(required = true)
        public MessageLevel level;

        @JsonProperty(required = true)
        public String text;

        // INSPECTOR ADD
        @JsonProperty
        public List<Parameter> parameters;
    }

    // INSPECTOR ADD BEGIN
    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class Parameter {
        @JsonProperty
        public String type;

        @JsonProperty
        public String value;

        @JsonProperty
        public String subtype;

        @JsonProperty
        public String className;

        @JsonProperty
        public String description;

        @JsonProperty
        public String objectId;

        @JsonProperty
        public Preview preview;
    }
    // INSPECTOR END

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class Preview {
        @JsonProperty
        public String type;

        @JsonProperty
        public String description;

        @JsonProperty
        public boolean lossless;

        @JsonProperty
        public boolean overflow;

        @JsonProperty
        public List<Property> properties;
    }

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class Property {
        @JsonProperty
        public String name;

        @JsonProperty
        public String type;

        @JsonProperty
        public String value;
    }

    @SuppressLint({"UsingDefaultJsonDeserializer", "EmptyJsonPropertyUse"})
    public static class CallFrame {
        @JsonProperty(required = true)
        public String functionName;

        @JsonProperty(required = true)
        public String url;

        @JsonProperty(required = true)
        public int lineNumber;

        @JsonProperty(required = true)
        public int columnNumber;

        public CallFrame() {
        }

        public CallFrame(String functionName, String url, int lineNumber, int columnNumber) {
            this.functionName = functionName;
            this.url = url;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
    }
}
