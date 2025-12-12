/*
 * Copyright (C) 2023, hapjs.org. All rights reserved.
 */

package org.hapjs.runtime.sandbox;

import android.util.ArrayMap;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hapjs.bridge.InstanceManager;
import org.hapjs.bridge.Response;
import org.hapjs.render.jsruntime.JsThread;
import org.hapjs.render.jsruntime.serialize.AbstractSerializeObject;
import org.hapjs.render.jsruntime.serialize.JavaSerializeObject;
import org.hapjs.render.jsruntime.serialize.TypedArrayProxy;
import org.hapjs.render.jsruntime.serialize.V8SerializeObject;
import org.json.JSONArray;
import org.json.JSONObject;

public class SerializeHelper {
    private static final String TAG = "SerializeHelper";

    private static class SingletonHolder {
        private static final SerializeHelper sInstance = new SerializeHelper();
    }

    public static SerializeHelper getInstance() {
        return SingletonHolder.sInstance;
    }

    private ThreadLocal<Kryo> mKryo = new ThreadLocal<>();

    private SerializeHelper() {
    }

    private void ensureRegister() {
        Kryo kryo = mKryo.get();
        if (kryo != null) {
            return;
        }

        long start = System.currentTimeMillis();
        kryo = new Kryo();
        mKryo.set(kryo);

        kryo.register(Map.class);
        kryo.register(HashMap.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(ArrayMap.class);
        kryo.register(JSONObject.class);
        kryo.register(JSONArray.class);
        kryo.register(Object[].class);
        kryo.register(List.class);
        kryo.register(ArrayList.class);
        kryo.register(JsThread.JsEventCallbackData.class);
        kryo.register(JsThread.JsMethodCallbackData.class);
        kryo.register(Response.class);
        kryo.register(JavaSerializeObject.class);
        kryo.register(StackTraceElement[].class);
        kryo.register(V8SerializeObject.class);
        kryo.register(AbstractSerializeObject.class);
        kryo.register(InstanceManager.InstanceHandler.class);
        kryo.register(byte[].class);
        kryo.register(org.hapjs.common.json.JSONObject.class);
        kryo.register(org.hapjs.common.json.JSONArray.class);

        V8Value undefined = V8.getUndefined();
        kryo.register(undefined.getClass(), new FieldSerializer(kryo, undefined.getClass()) {
            @Override
            public void write(Kryo kryo, Output output, Object object) {
                // 不序列化 Undefined 数据
            }

            @Override
            public Object read(Kryo kryo, Input input, Class type) {
                return V8.getUndefined();
            }
        });

        kryo.register(StackTraceElement.class, new FieldSerializer(kryo, StackTraceElement.class) {
            public void write(Kryo kryo, Output output, Object object) {
                StackTraceElement stack = (StackTraceElement) object;
                output.writeString(stack.getClassName());
                output.writeString(stack.getMethodName());
                output.writeString(stack.getFileName());
                output.writeInt(stack.getLineNumber());
            }

            public Object read(Kryo kryo, Input input, Class type) {
                String className = input.readString();
                String methodName = input.readString();
                String fileName = input.readString();
                int lineNumber = input.readInt();
                return new StackTraceElement(className, methodName, fileName, lineNumber);
            }
        });

        Class clazz = ByteBuffer.wrap(new byte[1]).getClass();
        kryo.register(clazz, new FieldSerializer(kryo, clazz) {
            public void write(Kryo kryo, Output output, Object object) {
                byte[] bytes = ((ByteBuffer) object).array();
                output.writeInt(bytes.length);
                output.write(bytes);
            }

            public Object read(Kryo kryo, Input input, Class type) {
                int len = input.readInt();
                byte[] bytes = input.readBytes(len);
                return ByteBuffer.wrap(bytes);
            }
        });

        kryo.register(TypedArrayProxy.class, new FieldSerializer(kryo, TypedArrayProxy.class) {
            public void write(Kryo kryo, Output output, Object object) {
                byte[] bytes = ((TypedArrayProxy) object).getBytes();
                output.writeInt(bytes.length);
                output.write(bytes);
                output.writeInt(((TypedArrayProxy) object).getType());
            }

            public Object read(Kryo kryo, Input input, Class type) {
                int len = input.readInt();
                byte[] bytes = input.readBytes(len);
                int dataType = input.readInt();
                return new TypedArrayProxy(dataType, bytes);
            }
        });

        Log.i(TAG, "kryo register costs=" + (System.currentTimeMillis() - start));
    }

    private Kryo getKryo() {
        ensureRegister();
        return mKryo.get();
    }

    public Serializer createSerializer() {
        return new Serializer();
    }

    public Deserializer createDeserializer(byte[] data) {
        return new Deserializer(data);
    }

    public class Serializer {
        private ByteArrayOutputStream mByteArrayOutputStream;
        private Output mOutput;

        private Serializer() {
            mByteArrayOutputStream = new ByteArrayOutputStream();
            mOutput = new Output(mByteArrayOutputStream);
        }

        public void writeObject(Object arg) {
            getKryo().writeObject(mOutput, arg);
        }

        public void writeObjectOrNull(Object arg, Class clazz) {
            getKryo().writeObjectOrNull(mOutput, arg, clazz);
        }

        public byte[] closeAndGetBytes() {
            mOutput.close();
            return mByteArrayOutputStream.toByteArray();
        }
    }

    public class Deserializer {
        private Input mInput;

        private Deserializer(byte[] data) {
            mInput = new Input(new ByteArrayInputStream(data));
        }

        public <T> T readObject(Class<T> clazz) {
            return getKryo().readObject(mInput, clazz);
        }

        public void close() {
            mInput.close();
        }
    }
}
